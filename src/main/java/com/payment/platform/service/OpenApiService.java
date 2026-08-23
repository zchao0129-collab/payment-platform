package com.payment.platform.service;

import com.payment.platform.dto.req.OpenOrderCreateReq;
import com.payment.platform.dto.req.OpenOrderQueryReq;

import java.util.Map;

/**
 * 开放API服务 — 外部商户调用订单创建/查询 + 支付链接
 */
public interface OpenApiService {

    /** 创建订单并返回支付链接 */
    Map<String, Object> createOrder(OpenOrderCreateReq req);

    /** 查询订单状态 */
    Map<String, Object> queryOrder(OpenOrderQueryReq req);

    /** 生成支付参数（供浏览器直接打开的支付链接使用） */
    Map<String, String> pay(String orderNo, String clientIp);

    /** 获取支付完成后的跳转地址 */
    String getReturnUrl(String orderNo);
}
