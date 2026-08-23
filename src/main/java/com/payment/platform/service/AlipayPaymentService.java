package com.payment.platform.service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 支付宝支付服务 — 构建支付请求、验证回调签名
 */
public interface AlipayPaymentService {

    /**
     * 构建手机网站支付（WAP）请求，返回支付宝收银台自动提交表单
     *
     * @param orderNo   商户订单号
     * @param amount    支付金额
     * @param subject   商品标题
     * @param returnUrl 支付完成后回跳页面（前端）
     * @return { "alipayForm": "<form>...</form>", "orderNo": "..." }
     */
    Map<String, String> buildWapPay(String orderNo, BigDecimal amount, String subject, String returnUrl);

    /**
     * 构建当面付（F2F）预下单请求，返回二维码内容
     *
     * @param orderNo 商户订单号
     * @param amount  支付金额
     * @param subject 商品标题
     * @return { "qrCode": "https://qr.alipay.com/...", "orderNo": "..." }
     */
    Map<String, String> buildF2FPay(String orderNo, BigDecimal amount, String subject);

    /**
     * 验证支付宝异步通知签名
     *
     * @param params 通知参数
     * @return true 验签通过
     */
    boolean verifyNotifySign(Map<String, String> params);

    /**
     * 验证支付宝同步回跳参数签名
     *
     * @param params 回跳参数
     * @return true 验签通过
     */
    boolean verifyReturnSign(Map<String, String> params);
}
