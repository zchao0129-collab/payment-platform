package com.payment.platform.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterReq {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "短信验证码不能为空")
    private String smsCode;

    @NotBlank(message = "支付宝账号不能为空")
    private String alipayAccount;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 20, message = "密码长度为8-20位")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    @NotBlank(message = "验证票据不能为空")
    private String captchaTicket;

    private String referralCode;
}
