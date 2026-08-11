package com.payment.platform.service;

import com.payment.platform.dto.req.LoginReq;
import com.payment.platform.dto.req.RegisterReq;
import com.payment.platform.dto.req.SmsSendReq;
import com.payment.platform.dto.resp.LoginResp;

public interface AuthService {

    /** 发送短信验证码 */
    void sendSmsCode(SmsSendReq req, String clientIp);

    /** 生成验证票据（机器人校验通过后调用） */
    String generateCaptchaTicket(Integer scene);

    /** 商户注册 */
    void register(RegisterReq req);

    /** 登录 */
    LoginResp login(LoginReq req, String ip, String deviceInfo);

    /** Token 刷新 */
    LoginResp refreshToken(String refreshToken);

    /** 登出 */
    void logout(Long userId);
}
