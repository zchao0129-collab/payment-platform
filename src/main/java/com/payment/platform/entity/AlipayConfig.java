package com.payment.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_alipay_config")
public class AlipayConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String configName;

    /** 配置类型: 1-证书, 2-秘钥 */
    private Integer configType;

    private String appId;

    private String uid;

    /** 秘钥模式 */
    private String privateKey;

    private String alipayPublicKey;

    private String appPublicKey;

    /** 证书模式 */
    private String appCertPath;

    private String rootCertPath;

    /** 支付宝公钥证书路径 */
    private String publicCertPath;

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
