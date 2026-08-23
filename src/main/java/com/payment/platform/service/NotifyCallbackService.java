package com.payment.platform.service;

/**
 * 商户回调推送服务 — 支付成功后主动推送商户 notify_url（带签名）
 */
public interface NotifyCallbackService {

    /** 支付成功后推送商户回调 */
    void notifyPaid(String orderNo);

    /** 定时重试失败的回调 */
    void retryFailedCallbacks();
}
