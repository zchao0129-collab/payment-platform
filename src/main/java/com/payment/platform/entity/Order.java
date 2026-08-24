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

    /** 支付通道: ALIPAY, WECHAT */
    private String payChannel;

    /** 交易类型: WAP-手机网站支付, F2F-当面付(支付宝)；微信可传 JSAPI/H5 */
    private String tradeType;

    /** 订单来源: CASHIER-收银台/码牌, OPEN_API-开放API */
    private String orderSource;

    private String alipayTradeNo;

    /** 通道交易号 (支付宝/微信通用) */
    private String channelTradeNo;

    private LocalDateTime payTime;

    private LocalDateTime callbackTime;

    private LocalDateTime refundTime;

    private java.math.BigDecimal refundAmount;

    private LocalDateTime expireTime;

    private String failReason;

    private Long qrcodeId;

    private String remark;

    /** 本单回调地址(空则用商户默认) */
    private String notifyUrl;

    /** 支付完成跳转地址(创建订单时填写) */
    private String returnUrl;

    /** 回调状态: 0-未通知, 1-成功, 2-失败待重试 */
    private Integer notifyStatus;

    /** 回调重试次数 */
    private Integer notifyCount;

    /** 最近回调时间 */
    private LocalDateTime notifyTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updatedAt;
}
