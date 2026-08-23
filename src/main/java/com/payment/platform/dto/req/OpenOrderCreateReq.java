package com.payment.platform.dto.req;

import lombok.Data;

/**
 * 开放API — 创建订单请求
 * <p>
 * 签名规则见 {@link com.payment.platform.common.utils.SignUtil}，
 * 所有字段均为字符串，timestamp 为毫秒时间戳字符串。
 */
@Data
public class OpenOrderCreateReq {

    /** 商户号(appId) */
    private String appId;

    /** 毫秒时间戳 */
    private String timestamp;

    /** 随机串(防重放) */
    private String nonce;

    /** 签名 */
    private String sign;

    private String productName;

    /** 金额，单位元，字符串 */
    private String amount;

    /** 支付通道: ALIPAY / WECHAT，默认 ALIPAY */
    private String payChannel;

    /** 支付宝交易类型: WAP(默认,手机网站支付) / F2F(当面付)；微信通道忽略 */
    private String tradeType;

    /** 本单回调地址（可选，覆盖商户默认） */
    private String notifyUrl;

    /** 支付完成后的页面跳转地址（可选） */
    private String returnUrl;

    private String remark;
}
