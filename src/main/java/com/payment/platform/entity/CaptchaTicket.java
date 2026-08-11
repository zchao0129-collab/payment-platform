package com.payment.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_captcha_ticket")
public class CaptchaTicket {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ticket;

    /** 场景: 1-注册, 2-登录, 3-找回密码, 4-获取验证码 */
    private Integer scene;

    /** 是否已消费: 0-否, 1-是 */
    private Integer isUsed;

    private LocalDateTime expireTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
