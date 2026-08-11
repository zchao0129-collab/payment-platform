package com.payment.platform.controller;

import com.payment.platform.common.Result;
import com.payment.platform.dto.req.LoginReq;
import com.payment.platform.dto.req.RegisterReq;
import com.payment.platform.dto.req.SmsSendReq;
import com.payment.platform.dto.resp.LoginResp;
import com.payment.platform.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证", description = "注册、登录、验证码")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "发送短信验证码")
    @PostMapping("/sms/send")
    public Result<Void> sendSms(@Valid @RequestBody SmsSendReq req, HttpServletRequest request) {
        authService.sendSmsCode(req, request.getRemoteAddr());
        return Result.success("验证码已发送");
    }

    @Operation(summary = "生成验证票据（机器人校验通过后）")
    @GetMapping("/captcha/ticket")
    public Result<String> captchaTicket(@RequestParam Integer scene) {
        String ticket = authService.generateCaptchaTicket(scene);
        return Result.ok(ticket);
    }

    @Operation(summary = "商户注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterReq req) {
        authService.register(req);
        return Result.success("注册成功");
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<LoginResp> login(@Valid @RequestBody LoginReq req, HttpServletRequest request) {
        LoginResp resp = authService.login(req, request.getRemoteAddr(),
                request.getHeader("User-Agent"));
        return Result.ok(resp);
    }

    @Operation(summary = "刷新Token")
    @PostMapping("/refresh")
    public Result<LoginResp> refresh(@RequestParam String refreshToken) {
        LoginResp resp = authService.refreshToken(refreshToken);
        return Result.ok(resp);
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestAttribute Long userId) {
        authService.logout(userId);
        return Result.ok();
    }
}
