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

    private String realName;

    private String idCardNo;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String salt;

    private String referralCode;

    private String parentReferral;

    private Integer status;

    private LocalDateTime loginLockUntil;

    private Integer loginFailCount;

    /** API签名密钥(MD5)，仅写入不序列化返回 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String apiSecret;

    /** 支付回调地址 */
    private String notifyUrl;

    /** 是否开通开放API: 0-未开通, 1-已开通 */
    private Integer apiEnabled;

    /** 调用IP白名单, 逗号分隔, 空=不限 */
    private String ipWhitelist;

    /** 金额浮动: 1-该商户API订单需浮动, 0-不浮动 */
    private Integer floatEnabled;

    private LocalDateTime apiSecretUpdatedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updatedAt;
}
