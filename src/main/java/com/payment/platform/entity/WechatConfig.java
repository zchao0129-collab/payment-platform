package com.payment.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_wechat_config")
public class WechatConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String configName;

    /** 微信公众号/小程序 AppId */
    private String appId;

    /** 商户号 */
    private String mchId;

    /** APIv3 密钥 */
    private String apiV3Key;

    /** 商户证书序列号 */
    private String serialNo;

    /** 商户私钥 (PEM 格式) */
    private String privateKey;

    /** 状态: 1-启用, 2-停用 */
    private Integer status;

    /** 权重: 启用配置间的流量分配比例，0=不使用 */
    private Integer weight;

    private LocalDateTime lastTestTime;

    private Integer lastTestResult;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updatedAt;
}
