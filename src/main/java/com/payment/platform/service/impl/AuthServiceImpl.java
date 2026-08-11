package com.payment.platform.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.payment.platform.common.BusinessException;
import com.payment.platform.common.utils.CodeGenerator;
import com.payment.platform.dto.req.LoginReq;
import com.payment.platform.dto.req.RegisterReq;
import com.payment.platform.dto.req.SmsSendReq;
import com.payment.platform.dto.resp.LoginResp;
import com.payment.platform.entity.*;
import com.payment.platform.enums.SmsSceneEnum;
import com.payment.platform.mapper.*;
import com.payment.platform.security.JwtTokenProvider;
import com.payment.platform.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final UserTokenMapper userTokenMapper;
    private final SmsCodeMapper smsCodeMapper;
    private final CaptchaTicketMapper captchaTicketMapper;
    private final ReferralRelationMapper referralRelationMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void sendSmsCode(SmsSendReq req, String clientIp) {
        // 校验验证票据
        validateCaptchaTicket(req.getCaptchaTicket(), req.getScene());
        // 限频检查：24小时内同手机号最多5次
        long todayCount = smsCodeMapper.selectCount(
                new LambdaQueryWrapper<SmsCode>()
                        .eq(SmsCode::getPhone, req.getPhone())
                        .ge(SmsCode::getCreatedAt, LocalDateTime.now().minusHours(24))
        );
        if (todayCount >= 5) {
            throw new BusinessException("该手机号24小时内发送次数已达上限");
        }
        // 生成验证码
        String code = RandomUtil.randomNumbers(6);
        SmsCode smsCode = new SmsCode();
        smsCode.setPhone(req.getPhone());
        smsCode.setCode(code);
        smsCode.setScene(req.getScene());
        smsCode.setIp(clientIp);
        smsCode.setExpireTime(LocalDateTime.now().plusMinutes(5));
        smsCodeMapper.insert(smsCode);
        // TODO: 调用短信服务商发送验证码
        log.info("短信验证码已生成: phone={}, code={}", req.getPhone(), code);
    }

    @Override
    public String generateCaptchaTicket(Integer scene) {
        CaptchaTicket ticket = new CaptchaTicket();
        ticket.setTicket(UUID.randomUUID().toString().replace("-", ""));
        ticket.setScene(scene);
        ticket.setExpireTime(LocalDateTime.now().plusMinutes(5));
        captchaTicketMapper.insert(ticket);
        return ticket.getTicket();
    }

    @Override
    @Transactional
    public void register(RegisterReq req) {
        // 校验密码一致性
        if (!req.getPassword().equals(req.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        // 校验短信验证码
        validateSmsCode(req.getPhone(), req.getSmsCode(), SmsSceneEnum.REGISTER.getCode());
        // 校验验证票据
        validateCaptchaTicket(req.getCaptchaTicket(), SmsSceneEnum.REGISTER.getCode());
        // 校验手机号唯一
        if (merchantMapper.selectCount(
                new LambdaQueryWrapper<Merchant>().eq(Merchant::getPhone, req.getPhone())) > 0) {
            throw new BusinessException("该手机号已被注册");
        }
        // 生成商户号 + 推荐码
        String merchantNo = CodeGenerator.generateMerchantNo();
        String referralCode = CodeGenerator.generateReferralCode();
        // 校验推荐码（如有）
        Long parentMerchantId = null;
        if (req.getReferralCode() != null && !req.getReferralCode().isBlank()) {
            Merchant parent = merchantMapper.selectOne(
                    new LambdaQueryWrapper<Merchant>().eq(Merchant::getReferralCode, req.getReferralCode()));
            if (parent == null) {
                throw new BusinessException("推荐码无效");
            }
            parentMerchantId = parent.getId();
        }
        // 创建商户
        Merchant merchant = new Merchant();
        merchant.setMerchantNo(merchantNo);
        merchant.setMerchantName("商户" + merchantNo);
        merchant.setPhone(req.getPhone());
        merchant.setAlipayAccount(req.getAlipayAccount());
        merchant.setPassword(passwordEncoder.encode(req.getPassword()));
        merchant.setReferralCode(referralCode);
        merchant.setParentReferral(req.getReferralCode());
        merchantMapper.insert(merchant);
        // 创建用户
        User user = new User();
        user.setUsername(req.getPhone());
        user.setPhone(req.getPhone());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(2); // 商户用户
        user.setMerchantId(merchant.getId());
        userMapper.insert(user);
        // 建立推荐关系
        if (parentMerchantId != null) {
            ReferralRelation relation = new ReferralRelation();
            relation.setParentMerchantId(parentMerchantId);
            relation.setChildMerchantId(merchant.getId());
            relation.setChildMerchantNo(merchantNo);
            relation.setLevel(1);
            referralRelationMapper.insert(relation);
        }
        log.info("商户注册成功: merchantNo={}, referralCode={}", merchantNo, referralCode);
    }

    @Override
    @Transactional
    public LoginResp login(LoginReq req, String ip, String deviceInfo) {
        // 校验验证票据
        validateCaptchaTicket(req.getCaptchaTicket(), SmsSceneEnum.LOGIN.getCode());
        // 查询用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, req.getPhone()));
        if (user == null) {
            throw new BusinessException("账号不存在");
        }
        if (user.getStatus() != 1) {
            throw new BusinessException("账号已被停用，请联系管理员");
        }
        // 检查锁定
        if (user.getLoginLockUntil() != null && user.getLoginLockUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException("账号已被锁定，请" +
                    user.getLoginLockUntil().toLocalTime() + "后重试");
        }
        // 校验密码
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            int failCount = user.getLoginFailCount() + 1;
            user.setLoginFailCount(failCount);
            if (failCount >= 5) {
                user.setLoginLockUntil(LocalDateTime.now().plusMinutes(30));
                user.setLoginFailCount(0);
                userMapper.updateById(user);
                throw new BusinessException("密码错误次数过多，账号已锁定30分钟");
            }
            userMapper.updateById(user);
            throw new BusinessException("密码错误，剩余 " + (5 - failCount) + " 次尝试");
        }
        // 登录成功，重置失败计数
        user.setLoginFailCount(0);
        user.setLoginLockUntil(null);
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(ip);
        userMapper.updateById(user);
        // 生成 Token
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        // 踢掉旧会话
        userTokenMapper.update(null,
                new LambdaUpdateWrapper<UserToken>()
                        .eq(UserToken::getUserId, user.getId())
                        .eq(UserToken::getIsLogout, 0)
                        .set(UserToken::getIsLogout, 1));
        // 存储新 Token
        UserToken token = new UserToken();
        token.setUserId(user.getId());
        token.setAccessToken(accessToken);
        token.setRefreshToken(refreshToken);
        token.setAccessExpire(LocalDateTime.now().plusHours(24));
        token.setRefreshExpire(LocalDateTime.now().plusDays(7));
        token.setLoginIp(ip);
        token.setDeviceInfo(deviceInfo);
        userTokenMapper.insert(token);
        // 查询商户信息
        Merchant merchant = null;
        if (user.getMerchantId() != null) {
            merchant = merchantMapper.selectById(user.getMerchantId());
        }
        log.info("登录成功: userId={}, role={}", user.getId(), user.getRole());
        return LoginResp.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .phone(user.getPhone())
                .role(user.getRole())
                .merchantId(user.getMerchantId())
                .merchantNo(merchant != null ? merchant.getMerchantNo() : null)
                .merchantName(merchant != null ? merchant.getMerchantName() : null)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(86400L)
                .build();
    }

    @Override
    @Transactional
    public LoginResp refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(401, "Token已过期，请重新登录");
        }
        UserToken tokenRecord = userTokenMapper.selectOne(
                new LambdaQueryWrapper<UserToken>()
                        .eq(UserToken::getRefreshToken, refreshToken)
                        .eq(UserToken::getIsLogout, 0));
        if (tokenRecord == null || tokenRecord.getRefreshExpire().isBefore(LocalDateTime.now())) {
            throw new BusinessException(401, "Token已过期，请重新登录");
        }
        User user = userMapper.selectById(tokenRecord.getUserId());
        if (user == null || user.getStatus() != 1) {
            throw new BusinessException(401, "账号不可用");
        }
        // 生成新 Token
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        // 旧 Token 失效
        tokenRecord.setIsLogout(1);
        userTokenMapper.updateById(tokenRecord);
        // 存储新 Token
        UserToken newToken = new UserToken();
        newToken.setUserId(user.getId());
        newToken.setAccessToken(newAccessToken);
        newToken.setRefreshToken(newRefreshToken);
        newToken.setAccessExpire(LocalDateTime.now().plusHours(24));
        newToken.setRefreshExpire(LocalDateTime.now().plusDays(7));
        newToken.setLoginIp(tokenRecord.getLoginIp());
        userTokenMapper.insert(newToken);

        Merchant merchant = null;
        if (user.getMerchantId() != null) {
            merchant = merchantMapper.selectById(user.getMerchantId());
        }
        return LoginResp.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .phone(user.getPhone())
                .role(user.getRole())
                .merchantId(user.getMerchantId())
                .merchantNo(merchant != null ? merchant.getMerchantNo() : null)
                .merchantName(merchant != null ? merchant.getMerchantName() : null)
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(86400L)
                .build();
    }

    @Override
    public void logout(Long userId) {
        userTokenMapper.update(null,
                new LambdaUpdateWrapper<UserToken>()
                        .eq(UserToken::getUserId, userId)
                        .eq(UserToken::getIsLogout, 0)
                        .set(UserToken::getIsLogout, 1));
    }

    // ---- 私有方法 ----

    private void validateSmsCode(String phone, String code, int scene) {
        SmsCode smsCode = smsCodeMapper.selectOne(
                new LambdaQueryWrapper<SmsCode>()
                        .eq(SmsCode::getPhone, phone)
                        .eq(SmsCode::getScene, scene)
                        .eq(SmsCode::getIsUsed, 0)
                        .orderByDesc(SmsCode::getCreatedAt)
                        .last("LIMIT 1"));
        if (smsCode == null) {
            throw new BusinessException("请先获取验证码");
        }
        if (smsCode.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("验证码已过期");
        }
        if (smsCode.getVerifyFail() >= 3) {
            throw new BusinessException("验证码错误次数过多，今日已冻结");
        }
        if (!smsCode.getCode().equals(code)) {
            smsCode.setVerifyFail(smsCode.getVerifyFail() + 1);
            smsCodeMapper.updateById(smsCode);
            throw new BusinessException("验证码错误");
        }
        smsCode.setIsUsed(1);
        smsCodeMapper.updateById(smsCode);
    }

    private void validateCaptchaTicket(String ticket, int scene) {
        CaptchaTicket t = captchaTicketMapper.selectOne(
                new LambdaQueryWrapper<CaptchaTicket>()
                        .eq(CaptchaTicket::getTicket, ticket)
                        .eq(CaptchaTicket::getIsUsed, 0));
        if (t == null) {
            throw new BusinessException("验证票据无效");
        }
        if (t.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("验证票据已过期");
        }
        t.setIsUsed(1);
        captchaTicketMapper.updateById(t);
    }
}
