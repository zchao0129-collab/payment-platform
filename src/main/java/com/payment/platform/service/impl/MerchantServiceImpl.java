package com.payment.platform.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.platform.common.BusinessException;
import com.payment.platform.common.utils.CodeGenerator;
import com.payment.platform.entity.Commission;
import com.payment.platform.entity.Merchant;
import com.payment.platform.entity.Order;
import com.payment.platform.entity.PaymentLog;
import com.payment.platform.entity.Qrcode;
import com.payment.platform.entity.ReferralRelation;
import com.payment.platform.entity.User;
import com.payment.platform.entity.Withdrawal;
import com.payment.platform.mapper.CommissionMapper;
import com.payment.platform.mapper.MerchantMapper;
import com.payment.platform.mapper.OrderMapper;
import com.payment.platform.mapper.PaymentLogMapper;
import com.payment.platform.mapper.QrCodeMapper;
import com.payment.platform.mapper.ReferralRelationMapper;
import com.payment.platform.mapper.UserMapper;
import com.payment.platform.mapper.WithdrawalMapper;
import com.payment.platform.service.MerchantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private final MerchantMapper merchantMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final OrderMapper orderMapper;
    private final PaymentLogMapper paymentLogMapper;
    private final CommissionMapper commissionMapper;
    private final WithdrawalMapper withdrawalMapper;
    private final QrCodeMapper qrCodeMapper;
    private final ReferralRelationMapper referralRelationMapper;

    @Override
    public Merchant getById(Long merchantId) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException("商户不存在");
        }
        return merchant;
    }

    @Override
    public void update(Merchant merchant) {
        Merchant db = getById(merchant.getId());
        // 仅允许修改部分字段
        db.setMerchantName(merchant.getMerchantName());
        db.setAlipayAccount(merchant.getAlipayAccount());
        db.setRealName(merchant.getRealName());
        db.setIdCardNo(merchant.getIdCardNo());
        merchantMapper.updateById(db);
        log.info("商户信息更新: id={}", merchant.getId());
    }

    @Override
    public void changePassword(Long merchantId, String oldPassword, String newPassword) {
        Merchant merchant = getById(merchantId);
        if (!passwordEncoder.matches(oldPassword, merchant.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        merchant.setPassword(passwordEncoder.encode(newPassword));
        merchantMapper.updateById(merchant);
        // 同步更新关联用户密码
        userMapper.update(null,
                new LambdaUpdateWrapper<User>()
                        .eq(User::getMerchantId, merchantId)
                        .set(User::getPassword, merchant.getPassword()));
        log.info("商户密码修改: id={}", merchantId);
    }

    @Override
    public Page<Merchant> queryPage(String merchantName, String phone, Long page, Long size) {
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<>();
        if (merchantName != null && !merchantName.isBlank()) {
            wrapper.like(Merchant::getMerchantName, merchantName);
        }
        if (phone != null && !phone.isBlank()) {
            wrapper.eq(Merchant::getPhone, phone);
        }
        wrapper.orderByDesc(Merchant::getCreatedAt);
        return merchantMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional
    public void create(Merchant merchant) {
        // 检查手机号唯一性
        if (merchantMapper.selectCount(
                new LambdaQueryWrapper<Merchant>().eq(Merchant::getPhone, merchant.getPhone())) > 0) {
            throw new BusinessException("该手机号已被注册");
        }
        merchant.setMerchantNo(CodeGenerator.generateMerchantNo());
        merchant.setReferralCode(CodeGenerator.generateReferralCode());
        merchant.setPassword(passwordEncoder.encode(merchant.getPassword()));
        merchantMapper.insert(merchant);
        // 同时创建商户用户
        User user = new User();
        user.setUsername(merchant.getPhone());
        user.setPhone(merchant.getPhone());
        user.setPassword(merchant.getPassword());
        user.setRole(2); // 商户用户
        user.setMerchantId(merchant.getId());
        user.setStatus(1);
        userMapper.insert(user);
        log.info("管理端创建商户: merchantNo={}", merchant.getMerchantNo());
    }

    @Override
    @Transactional
    public void adminUpdate(Merchant merchant) {
        Merchant db = getById(merchant.getId());

        // 手机号变更：校验唯一性 + 同步关联用户登录账号
        String newPhone = merchant.getPhone();
        if (newPhone != null && !newPhone.isBlank() && !newPhone.equals(db.getPhone())) {
            if (merchantMapper.selectCount(
                    new LambdaQueryWrapper<Merchant>()
                            .eq(Merchant::getPhone, newPhone)
                            .ne(Merchant::getId, db.getId())) > 0) {
                throw new BusinessException("该手机号已被其他商户使用");
            }
            userMapper.update(null,
                    new LambdaUpdateWrapper<User>()
                            .eq(User::getMerchantId, db.getId())
                            .set(User::getPhone, newPhone)
                            .set(User::getUsername, newPhone));
            db.setPhone(newPhone);
        }

        db.setMerchantName(merchant.getMerchantName());
        db.setAlipayAccount(merchant.getAlipayAccount());
        db.setRealName(merchant.getRealName());
        db.setIdCardNo(merchant.getIdCardNo());
        // 开放API配置
        db.setNotifyUrl(merchant.getNotifyUrl());
        db.setApiEnabled(merchant.getApiEnabled());
        db.setIpWhitelist(merchant.getIpWhitelist());
        merchantMapper.updateById(db);
        log.info("管理端商户信息更新: id={}", merchant.getId());
    }

    @Override
    public void toggleStatus(Long merchantId, Integer status) {
        Merchant merchant = getById(merchantId);
        merchant.setStatus(status);
        merchantMapper.updateById(merchant);
        // 同步禁用商户用户
        userMapper.update(null,
                new LambdaUpdateWrapper<User>()
                        .eq(User::getMerchantId, merchantId)
                        .set(User::getStatus, status));
        log.info("商户状态变更: id={}, status={}", merchantId, status);
    }

    @Override
    public void updateApiConfig(Long merchantId, Integer apiEnabled, String notifyUrl, String ipWhitelist, Integer floatEnabled) {
        Merchant db = getById(merchantId);
        if (apiEnabled != null) {
            db.setApiEnabled(apiEnabled);
        }
        if (notifyUrl != null) {
            db.setNotifyUrl(notifyUrl);
        }
        if (ipWhitelist != null) {
            db.setIpWhitelist(ipWhitelist);
        }
        if (floatEnabled != null) {
            db.setFloatEnabled(floatEnabled);
        }
        merchantMapper.updateById(db);
        log.info("商户开放API配置更新: id={}, apiEnabled={}, floatEnabled={}", merchantId, apiEnabled, floatEnabled);
    }

    @Override
    public String resetApiSecret(Long merchantId) {
        Merchant db = getById(merchantId);
        String secret = RandomUtil.randomString(32);
        db.setApiSecret(secret);
        db.setApiSecretUpdatedAt(LocalDateTime.now());
        merchantMapper.updateById(db);
        log.info("商户API密钥重置: id={}", merchantId);
        return secret;
    }

    @Override
    @Transactional
    public void deleteMerchant(Long merchantId) {
        Merchant db = getById(merchantId);

        // 收集该商户的订单ID，用于清理支付日志
        List<Long> orderIds = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                        .select(Order::getId)
                        .eq(Order::getMerchantId, merchantId))
                .stream()
                .map(Order::getId)
                .collect(Collectors.toList());
        if (!orderIds.isEmpty()) {
            paymentLogMapper.delete(new LambdaQueryWrapper<PaymentLog>()
                    .in(PaymentLog::getOrderId, orderIds));
        }

        // 清理佣金、订单、码牌、提现记录
        commissionMapper.delete(new LambdaQueryWrapper<Commission>().eq(Commission::getMerchantId, merchantId));
        orderMapper.delete(new LambdaQueryWrapper<Order>().eq(Order::getMerchantId, merchantId));
        qrCodeMapper.delete(new LambdaQueryWrapper<Qrcode>().eq(Qrcode::getMerchantId, merchantId));
        withdrawalMapper.delete(new LambdaQueryWrapper<Withdrawal>().eq(Withdrawal::getMerchantId, merchantId));

        // 清理推荐关系（作为上级或下级）
        referralRelationMapper.delete(new LambdaQueryWrapper<ReferralRelation>()
                .eq(ReferralRelation::getParentMerchantId, merchantId)
                .or()
                .eq(ReferralRelation::getChildMerchantId, merchantId));

        // 清理关联登录用户
        userMapper.delete(new LambdaQueryWrapper<User>().eq(User::getMerchantId, merchantId));

        // 删除商户
        merchantMapper.deleteById(merchantId);
        log.info("管理端删除商户: id={}, merchantNo={}", merchantId, db.getMerchantNo());
    }
}
