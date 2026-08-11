package com.payment.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_order")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long merchantId;

    private String merchantNo;

    private String productName;

    private java.math.BigDecimal orderAmount;

    /** 状态: 1-新建, 2-已支付, 3-已回调, 4-已退款, 5-已失效, 6-支付失败 */
    private Integer orderStatus;

    private String alipayTradeNo;

    private LocalDateTime payTime;

    private LocalDateTime callbackTime;

    private LocalDateTime refundTime;

    private java.math.BigDecimal refundAmount;

    private LocalDateTime expireTime;

    private String failReason;

    private Long qrcodeId;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updatedAt;
}
