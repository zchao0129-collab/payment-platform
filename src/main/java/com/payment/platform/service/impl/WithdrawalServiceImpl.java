package com.payment.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.platform.common.BusinessException;
import com.payment.platform.common.utils.CodeGenerator;
import com.payment.platform.entity.*;
import com.payment.platform.enums.WithdrawStatusEnum;
import com.payment.platform.mapper.*;
import com.payment.platform.service.WithdrawalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalServiceImpl implements WithdrawalService {

    private final WithdrawalMapper withdrawalMapper;
    private final CommissionMapper commissionMapper;
    private final MerchantMapper merchantMapper;

    @Override
    @Transactional
    public void submitWithdraw(Long merchantId, BigDecimal amount) {
        // 校验金额
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("提现金额必须大于0");
        }
        // 查询可提现佣金总和
        List<Commission> commissions = commissionMapper.selectList(
                new LambdaQueryWrapper<Commission>()
                        .eq(Commission::getMerchantId, merchantId)
                        .eq(Commission::getWithdrawStatus, WithdrawStatusEnum.UNWITHDRAWN.getCode()));
        BigDecimal withdrawable = commissions.stream()
                .map(Commission::getCommAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (amount.compareTo(withdrawable) > 0) {
            throw new BusinessException("可提现余额不足，当前可提现: " + withdrawable);
        }
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException("商户不存在");
        }
        // 创建提现单
        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setWithdrawalNo(CodeGenerator.generateWithdrawalNo());
        withdrawal.setMerchantId(merchantId);
        withdrawal.setMerchantNo(merchant.getMerchantNo());
        withdrawal.setAmount(amount);
        withdrawal.setAlipayAccount(merchant.getAlipayAccount());
        withdrawal.setStatus(WithdrawStatusEnum.AUDIT_PENDING);
        withdrawalMapper.insert(withdrawal);
        // 按最早佣金顺序扣减到审核中
        BigDecimal remaining = amount;
        for (Commission c : commissions) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal deduct = c.getCommAmount().min(remaining);
            remaining = remaining.subtract(deduct);
            c.setWithdrawStatus(WithdrawStatusEnum.AUDITING.getCode());
            c.setWithdrawalId(withdrawal.getId());
            commissionMapper.updateById(c);
        }
        log.info("提现申请: merchantId={}, amount={}, withdrawalNo={}", merchantId, amount, withdrawal.getWithdrawalNo());
    }

    @Override
    @Transactional
    public void approve(Long withdrawalId, Long auditUserId) {
        Withdrawal withdrawal = withdrawalMapper.selectById(withdrawalId);
        if (withdrawal == null) {
            throw new BusinessException("提现单不存在");
        }
        if (withdrawal.getStatus() != WithdrawStatusEnum.AUDIT_PENDING) {
            throw new BusinessException("只有待审核的提现单可以审核");
        }
        // 更新提现单
        withdrawal.setStatus(WithdrawStatusEnum.AUDIT_PASSED);
        withdrawal.setAuditUserId(auditUserId);
        withdrawal.setAuditTime(LocalDateTime.now());
        withdrawalMapper.updateById(withdrawal);
        // 更新关联佣金为已打款
        commissionMapper.update(null,
                new LambdaUpdateWrapper<Commission>()
                        .eq(Commission::getWithdrawalId, withdrawalId)
                        .eq(Commission::getWithdrawStatus, WithdrawStatusEnum.AUDITING.getCode())
                        .set(Commission::getWithdrawStatus, WithdrawStatusEnum.PAID.getCode()));
        log.info("提现审核通过: withdrawalNo={}, auditUser={}", withdrawal.getWithdrawalNo(), auditUserId);
    }

    @Override
    @Transactional
    public void reject(Long withdrawalId, Long auditUserId, String reason) {
        Withdrawal withdrawal = withdrawalMapper.selectById(withdrawalId);
        if (withdrawal == null) {
            throw new BusinessException("提现单不存在");
        }
        if (withdrawal.getStatus() != WithdrawStatusEnum.AUDIT_PENDING) {
            throw new BusinessException("只有待审核的提现单可以驳回");
        }
        // 更新提现单
        withdrawal.setStatus(WithdrawStatusEnum.AUDIT_REJECTED);
        withdrawal.setAuditUserId(auditUserId);
        withdrawal.setAuditTime(LocalDateTime.now());
        withdrawal.setRejectReason(reason);
        withdrawalMapper.updateById(withdrawal);
        // 恢复关联佣金为未提现
        commissionMapper.update(null,
                new LambdaUpdateWrapper<Commission>()
                        .eq(Commission::getWithdrawalId, withdrawalId)
                        .eq(Commission::getWithdrawStatus, WithdrawStatusEnum.AUDITING.getCode())
                        .set(Commission::getWithdrawStatus, WithdrawStatusEnum.UNWITHDRAWN.getCode())
                        .set(Commission::getWithdrawalId, null));
        log.info("提现审核驳回: withdrawalNo={}, reason={}", withdrawal.getWithdrawalNo(), reason);
    }

    @Override
    public Page<Withdrawal> queryPage(Long merchantId, Integer status, Long page, Long size) {
        LambdaQueryWrapper<Withdrawal> wrapper = new LambdaQueryWrapper<>();
        if (merchantId != null) {
            wrapper.eq(Withdrawal::getMerchantId, merchantId);
        }
        if (status != null) {
            wrapper.eq(Withdrawal::getStatus, status);
        }
        wrapper.orderByDesc(Withdrawal::getCreatedAt);
        return withdrawalMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
