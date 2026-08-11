package com.payment.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_merchant")
public class Merchant {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String merchantNo;

    private String merchantName;

    private String phone;

    private String alipayAccount;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String salt;

    private String referralCode;

    private String parentReferral;

    private Integer status;

    private LocalDateTime loginLockUntil;

    private Integer loginFailCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updatedAt;
}
