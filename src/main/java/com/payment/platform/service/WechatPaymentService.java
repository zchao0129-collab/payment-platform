package com.payment.platform.service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 微信支付服务 — JSAPI / H5 支付 + OAuth + 回调验签
 */
public interface WechatPaymentService {

    /**
     * JSAPI 支付（微信内置浏览器）— 返回前端调起支付所需的参数
     *
     * @param orderNo  商户订单号
     * @param amount   支付金额
     * @param subject  商品标题
     * @param openid   用户 openid（通过 OAuth 获取）
     * @param clientIp 用户客户端 IP
     * @return { "appId", "timeStamp", "nonceStr", "package", "signType", "paySign" }
     */
    Map<String, String> buildJSAPIPay(String orderNo, BigDecimal amount, String subject,
                                      String openid, String clientIp);

    /**
     * H5 支付（非微信浏览器）— 返回 mweb_url 用于跳转
     *
     * @param orderNo     商户订单号
     * @param amount      支付金额
     * @param subject     商品标题
     * @param clientIp    用户客户端 IP
     * @param redirectUrl 支付完成后微信回跳的地址（结果页），可为空
     * @return { "mwebUrl": "https://..." }
     */
    Map<String, String> buildH5Pay(String orderNo, BigDecimal amount, String subject, String clientIp,
                                   String redirectUrl);

    /**
     * 通过 OAuth2.0 code 换取 openid
     *
     * @param code 微信回调返回的 authorization_code
     * @return openid
     */
    String getOpenidByCode(String code);

    /**
     * 验证微信支付异步通知签名
     *
     * @param body      通知原始 body
     * @param serialNo  HTTP 头 Wechatpay-Serial
     * @param signature HTTP 头 Wechatpay-Signature
     * @param timestamp HTTP 头 Wechatpay-Timestamp
     * @param nonce     HTTP 头 Wechatpay-Nonce
     * @return true 验签通过
     */
    boolean verifyNotifySign(String body, String serialNo, String signature,
                             String timestamp, String nonce);
}
