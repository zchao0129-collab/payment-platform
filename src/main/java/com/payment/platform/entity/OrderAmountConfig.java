package com.payment.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单金额浮动配置（单行全局配置）
 * <p>
 * 开放API下单时，订单金额在原始金额基础上做「上下浮动」，
 * 浮动区间由 {@code minFloat} ~ {@code maxFloat} 决定。
 */
@Data
@TableName("t_order_amount_config")
public class OrderAmountConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 是否启用: 1-启用, 2-停用 */
    private Integer enabled;

    /** 最小浮动金额（元） */
    private BigDecimal minFloat;

    /** 最大浮动金额（元） */
    private BigDecimal maxFloat;

    /** 浮动方向: UP-只上浮, DOWN-只下浮, BOTH-上下随机 */
    private String floatDirection;

    /** 判定主从: MERCHANT-以商户为主, URL-以跳转/回调地址为主 */
    private String judgeMode;

    /** 跳转/回调地址关键字(域名/关键字), 逗号分隔; judgeMode=URL 时命中任一即浮动 */
    private String floatUrlKeywords;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updatedAt;
}
