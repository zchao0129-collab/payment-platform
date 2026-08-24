package com.payment.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.platform.common.BusinessException;
import com.payment.platform.common.utils.CodeGenerator;
import com.payment.platform.entity.*;
import com.payment.platform.enums.OrderSourceEnum;
import com.payment.platform.enums.OrderStatusEnum;
import com.payment.platform.enums.WithdrawStatusEnum;
import com.payment.platform.mapper.*;
import com.payment.platform.service.CommissionService;
import com.payment.platform.service.CommConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommissionServiceImpl implements CommissionService {

    private final CommissionMapper commissionMapper;
    private final OrderMapper orderMapper;
    private final MerchantMapper merchantMapper;
    private final WithdrawalMapper withdrawalMapper;
    private final CommConfigService commConfigService;
    private final RedissonClient redissonClient;

    @Override
    public Page<Commission> queryPage(Long merchantId, Long page, Long size) {
        LambdaQueryWrapper<Commission> wrapper = new LambdaQueryWrapper<Commission>()
                .eq(Commission::getMerchantId, merchantId)
                .orderByDesc(Commission::getCreatedAt);
        return commissionMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public BigDecimal getTotalCommission(Long merchantId) {
        // 累计佣金 = 全部已产生的佣金（未提现 + 审核中 + 已打款）
        List<Commission> list = commissionMapper.selectList(
                new LambdaQueryWrapper<Commission>()
                        .eq(Commission::getMerchantId, merchantId)
                        .in(Commission::getWithdrawStatus,
                                WithdrawStatusEnum.UNWITHDRAWN.getCode(),
                                WithdrawStatusEnum.AUDITING.getCode(),
                                WithdrawStatusEnum.PAID.getCode()));
        return list.stream()
                .map(Commission::getCommAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal getWithdrawableAmount(Long merchantId) {
        List<Commission> list = commissionMapper.selectList(
                new LambdaQueryWrapper<Commission>()
                        .eq(Commission::getMerchantId, merchantId)
                        .eq(Commission::getWithdrawStatus, WithdrawStatusEnum.UNWITHDRAWN.getCode()));
        return list.stream()
                .map(Commission::getCommAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal getWithdrawnAmount(Long merchantId) {
        // 已提现 = 已打款（审核通过并完成打款）
        List<Commission> list = commissionMapper.selectList(
                new LambdaQueryWrapper<Commission>()
                        .eq(Commission::getMerchantId, merchantId)
                        .eq(Commission::getWithdrawStatus, WithdrawStatusEnum.PAID.getCode()));
        return list.stream()
                .map(Commission::getCommAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal getAuditingAmount(Long merchantId) {
        // 待审核 = 审核中的佣金
        List<Commission> list = commissionMapper.selectList(
                new LambdaQueryWrapper<Commission>()
                        .eq(Commission::getMerchantId, merchantId)
                        .eq(Commission::getWithdrawStatus, WithdrawStatusEnum.AUDITING.getCode()));
        return list.stream()
                .map(Commission::getCommAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Scheduled(fixedDelay = 600_000, initialDelay = 30_000)
    public void generateDailyCommissions() {
        RLock lock = redissonClient.getLock("commission:generate:lock");
        try {
            if (lock.tryLock(10, 60, TimeUnit.SECONDS)) {
                try {
                    // 查询所有已支付、待生成佣金的订单（PAID 而非 CALLBACK，因为回调仅设置 PAID 状态）
                    // 排除开放API下单的订单（外部接口调用创建的订单不计算佣金）
                    List<Order> orders = orderMapper.selectList(
                            new LambdaQueryWrapper<Order>()
                                    .eq(Order::getOrderStatus, OrderStatusEnum.PAID.getCode())
                                    .ne(Order::getOrderSource, OrderSourceEnum.OPEN_API.getCode())
                                    .last("LIMIT 1000"));
                    if (orders.isEmpty()) {
                        log.info("无待结算佣金订单");
                        return;
                    }
                    int generated = 0;
                    for (Order order : orders) {
                        try {
                            generateCommissionForOrder(order);
                            generated++;
                        } catch (Exception e) {
                            log.error("订单佣金生成失败: orderNo={}", order.getOrderNo(), e);
                        }
                    }
                    log.info("佣金结算完成: 处理{}笔, 成功{}笔", orders.size(), generated);
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    @Transactional
    public void withdraw(Long merchantId, BigDecimal amount) {
        // 校验提现金额
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("提现金额必须大于0");
        }
        // 查询商户信息
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException("商户不存在");
        }
        // 校验商户资料完整性（手机号、支付宝账号、真实姓名、身份证号）
        validateMerchantProfile(merchant);
        // 校验可提现金额
        BigDecimal withdrawable = getWithdrawableAmount(merchantId);
        if (amount.compareTo(withdrawable) > 0) {
            throw new BusinessException("可提现余额不足，当前可提现: " + withdrawable);
        }
        // 生成提现单
        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setWithdrawalNo(CodeGenerator.generateWithdrawalNo());
        withdrawal.setMerchantId(merchantId);
        withdrawal.setMerchantNo(merchant.getMerchantNo());
        withdrawal.setAmount(amount);
        withdrawal.setAlipayAccount(merchant.getAlipayAccount());
        withdrawal.setStatus(WithdrawStatusEnum.AUDIT_PENDING);
        withdrawalMapper.insert(withdrawal);
        // 按需扣减佣金（从最早的未提现开始）
        BigDecimal remaining = amount;
        List<Commission> commissions = commissionMapper.selectList(
                new LambdaQueryWrapper<Commission>()
                        .eq(Commission::getMerchantId, merchantId)
                        .eq(Commission::getWithdrawStatus, WithdrawStatusEnum.UNWITHDRAWN.getCode())
                        .orderByAsc(Commission::getCreatedAt));
        for (Commission c : commissions) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal deduct = c.getCommAmount().min(remaining);
            remaining = remaining.subtract(deduct);
            c.setWithdrawStatus(WithdrawStatusEnum.AUDITING.getCode());
            c.setWithdrawalId(withdrawal.getId());
            commissionMapper.updateById(c);
        }
        log.info("发起提现: merchantId={}, amount={}, withdrawalNo={}", merchantId, amount, withdrawal.getWithdrawalNo());
    }

    // ---- 私有方法 ----

    /**
     * 校验商户资料完整性：手机号、支付宝账号、真实姓名、身份证号。
     * 资料不完整或身份证号校验不通过时抛出异常，提示商户自行补充。
     */
    private void validateMerchantProfile(Merchant merchant) {
        if (hasNoText(merchant.getPhone())) {
            throw new BusinessException("商户资料不完整：请先补充手机号");
        }
        if (hasNoText(merchant.getAlipayAccount())) {
            throw new BusinessException("商户资料不完整：请先补充支付宝账号");
        }
        if (hasNoText(merchant.getRealName())) {
            throw new BusinessException("商户资料不完整：请先补充真实姓名");
        }
        if (hasNoText(merchant.getIdCardNo())) {
            throw new BusinessException("商户资料不完整：请先补充身份证号码");
        }
        if (!isValidIdCard(merchant.getIdCardNo())) {
            throw new BusinessException("身份证号码校验不通过，请核实后重新填写");
        }
    }

    private boolean hasNoText(String value) {
        return value == null || value.trim().isEmpty();
    }

    /** 18 位身份证号码真实性校验（含校验位） */
    private boolean isValidIdCard(String idCardNo) {
        if (idCardNo == null || !idCardNo.matches("\\d{17}[\\dXx]")) {
            return false;
        }
        int[] weights = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
        char[] checkCodes = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (idCardNo.charAt(i) - '0') * weights[i];
        }
        char expected = checkCodes[sum % 11];
        char actual = idCardNo.charAt(17);
        return expected == actual || (expected == 'X' && (actual == 'x' || actual == 'X'));
    }

    private void generateCommissionForOrder(Order order) {
        // 开放API下单的订单不计算佣金（双重保险，防止其它调用路径漏判）
        if (OrderSourceEnum.isOpenApi(order.getOrderSource())) {
            return;
        }
        // 检查是否已生成
        if (commissionMapper.selectCount(
                new LambdaQueryWrapper<Commission>().eq(Commission::getOrderId, order.getId())) > 0) {
            return;
        }
        // 匹配返佣比例
        BigDecimal rate = commConfigService.getRateByAmount(order.getOrderAmount());
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            return; // 无返佣配置，跳过
        }
        BigDecimal commAmount = order.getOrderAmount()
                .multiply(rate)
                .setScale(2, RoundingMode.HALF_UP);
        // 佣金金额四舍五入后为 0 时，不生成佣金记录（避免产生无意义的 0 佣金）
        if (commAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        Commission commission = new Commission();
        commission.setCommissionNo(CodeGenerator.generateCommissionNo());
        commission.setMerchantId(order.getMerchantId());
        commission.setOrderId(order.getId());
        commission.setOrderNo(order.getOrderNo());
        commission.setOrderAmount(order.getOrderAmount());
        commission.setCommRate(rate);
        commission.setCommAmount(commAmount);
        commission.setWithdrawStatus(WithdrawStatusEnum.UNWITHDRAWN.getCode());
        commission.setSettleDate(LocalDate.now());
        commissionMapper.insert(commission);
    }
}
