package com.payment.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.platform.common.BusinessException;
import com.payment.platform.common.utils.CodeGenerator;
import com.payment.platform.entity.Merchant;
import com.payment.platform.entity.User;
import com.payment.platform.mapper.MerchantMapper;
import com.payment.platform.mapper.UserMapper;
import com.payment.platform.service.MerchantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private final MerchantMapper merchantMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

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
}
