package com.payment.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.platform.dto.req.OrderQueryReq;
import com.payment.platform.entity.Order;

import java.math.BigDecimal;
import java.util.Map;

public interface OrderService {

    /** 收银台创建订单 */
    Order createOrder(Long merchantId, String productName, BigDecimal amount, Long qrcodeId, String remark);

    /** 构建支付参数（收银台/开放API共用），返回 orderNo/amount/payChannel + 通道支付参数 */
    Map<String, Object> buildPaymentParams(Order order, String payChannel, String openid, String clientIp);

    /** 支付宝支付回调 */
    void handleAlipayNotify(String notifyContent);

    /** 订单分页查询 */
    Page<Order> queryPage(OrderQueryReq req, Long merchantId);

    /** 根据订单号查询 */
    Order getByOrderNo(String orderNo);

    /** 订单详情 */
    Order getDetail(Long orderId);

    /** 全额退款 */
    void refund(Long orderId);

    /** 超时失效扫描（定时任务） */
    void expireOrders();
}
