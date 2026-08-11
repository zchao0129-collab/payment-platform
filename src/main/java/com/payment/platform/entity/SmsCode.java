package com.payment.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_sms_code")
public class SmsCode {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String phone;

    private String code;

    /** 场景: 1-注册, 2-登录, 3-找回密码, 4-修改手机号 */
    private Integer scene;

    private String ip;

    /** 是否已使用: 0-否, 1-是 */
    private Integer isUsed;

    /** 当日校验失败次数 */
    private Integer verifyFail;

    private LocalDateTime expireTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
