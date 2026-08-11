package com.payment.platform.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SmsSendReq {

    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotNull(message = "场景不能为空")
    private Integer scene;

    @NotBlank(message = "验证票据不能为空")
    private String captchaTicket;
}
