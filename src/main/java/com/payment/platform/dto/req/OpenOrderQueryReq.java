package com.payment.platform.dto.req;

import lombok.Data;

/**
 * 开放API — 查询订单请求
 */
@Data
public class OpenOrderQueryReq {

    /** 商户号(appId) */
    private String appId;

    /** 毫秒时间戳 */
    private String timestamp;

    /** 随机串(防重放) */
    private String nonce;

    /** 签名 */
    private String sign;

    private String orderNo;

    /** 上游商户订单号（可选，与 orderNo 二选一） */
    private String merchantOrderNo;
}
