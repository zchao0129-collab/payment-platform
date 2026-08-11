package com.payment.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_user_token")
public class UserToken {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String accessToken;

    private String refreshToken;

    private LocalDateTime accessExpire;

    private LocalDateTime refreshExpire;

    private String loginIp;

    private String deviceInfo;

    /** 是否已主动登出: 0-否, 1-是 */
    private Integer isLogout;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updatedAt;
}
