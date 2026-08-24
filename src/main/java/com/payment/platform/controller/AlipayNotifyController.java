package com.payment.platform.controller;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.payment.platform.entity.Order;
import com.payment.platform.enums.OrderStatusEnum;
import com.payment.platform.mapper.OrderMapper;
import com.payment.platform.service.AlipayPaymentService;
import com.payment.platform.service.NotifyCallbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝回调控制器
 * <p>
 * /api/alipay/notify — 异步支付结果通知（支付宝服务器 → 商户服务器）
 * /api/alipay/return — 同步回跳（支付宝 → 浏览器 → 前端结果页）
 */
@Slf4j
@Tag(name = "支付宝回调", description = "支付宝支付结果通知与回跳")
@RestController
@RequestMapping("/api/alipay")
@RequiredArgsConstructor
public class AlipayNotifyController {

    private final AlipayPaymentService alipayPaymentService;
    private final OrderMapper orderMapper;
    private final NotifyCallbackService notifyCallbackService;

    @Value("${app.cashier-base-url}")
    private String cashierBaseUrl;

    /**
     * 支付宝异步通知（POST）
     * <p>
     * 支付宝在支付成功后以 POST 形式调用此接口通知商户。
     * 商户需返回 "success"（纯文本）表示接收成功，否则支付宝会按策略重发。
     */
    @Operation(summary = "支付宝异步支付通知")
    @PostMapping("/notify")
    public String notify(HttpServletRequest request) {
        Map<String, String> params = extractParams(request);
        log.info("收到支付宝异步通知: {}", params);

        // 1. 验签
        if (!alipayPaymentService.verifyNotifySign(params)) {
            log.error("异步通知验签失败");
            return "failure";
        }

        // 2. 获取订单信息
        String orderNo = params.get("out_trade_no");
        String tradeNo = params.get("trade_no");
        String tradeStatus = params.get("trade_status");

        if (orderNo == null || tradeNo == null) {
            log.error("异步通知参数不完整: out_trade_no={}, trade_no={}", orderNo, tradeNo);
            return "failure";
        }

        // 3. 根据交易状态更新订单
        try {
            switch (tradeStatus) {
                case "TRADE_SUCCESS":
                case "TRADE_FINISHED":
                    handlePaymentSuccess(orderNo, tradeNo, params);
                    break;
                case "TRADE_CLOSED":
                    handlePaymentClosed(orderNo);
                    break;
                default:
                    log.info("未处理的交易状态: {}, orderNo={}", tradeStatus, orderNo);
            }
        } catch (Exception e) {
            log.error("处理异步通知异常: orderNo={}", orderNo, e);
            return "failure";
        }

        return "success";
    }

    /**
     * 支付宝同步回跳（GET）
     * <p>
     * 用户在支付宝完成/取消支付后，浏览器被重定向到此接口。
     * 此接口做简单验签后，将用户重定向到前端结果展示页。
     */
    @Operation(summary = "支付宝同步回跳")
    @GetMapping("/return")
    public void syncReturn(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, String> params = extractParams(request);
        log.info("收到支付宝同步回跳: {}", params);

        String orderNo = params.get("out_trade_no");
        String totalAmount = params.get("total_amount");

        // 验签（非强制 — 同步回跳可能被篡改，最终以异步通知为准）
        boolean signOk = alipayPaymentService.verifyReturnSign(params);
        if (!signOk) {
            log.warn("同步回跳验签失败: orderNo={}", orderNo);
        }

        // 构建前端结果页 URL
        StringBuilder redirectUrl = new StringBuilder(cashierBaseUrl);
        redirectUrl.append("/app/pay-result?orderNo=").append(orderNo != null ? orderNo : "");
        if (totalAmount != null) {
            redirectUrl.append("&amount=").append(totalAmount);
        }
        if (signOk && "TRADE_SUCCESS".equals(params.get("trade_status"))) {
            redirectUrl.append("&status=success");
        } else if (signOk) {
            redirectUrl.append("&status=").append(params.getOrDefault("trade_status", "unknown"));
        } else {
            // 同步回跳验签失败也传递原始 trade_status，前端展示；最终以异步通知为准
            String rawStatus = params.get("trade_status");
            if (rawStatus != null) {
                redirectUrl.append("&status=").append(rawStatus);
            }
        }

        log.info("重定向到前端结果页: {}", redirectUrl);
        response.sendRedirect(redirectUrl.toString());
    }

    // ======================== 私有方法 ========================

    /** 支付成功 — 更新订单状态 */
    private void handlePaymentSuccess(String orderNo, String tradeNo, Map<String, String> params) {
        // 防止重复处理：仅处理 NEW 状态的订单
        orderMapper.update(null,
                new LambdaUpdateWrapper<Order>()
                        .eq(Order::getOrderNo, orderNo)
                        .in(Order::getOrderStatus, OrderStatusEnum.NEW.getCode(), OrderStatusEnum.PAID.getCode())
                        .set(Order::getOrderStatus, OrderStatusEnum.PAID.getCode())
                        .set(Order::getAlipayTradeNo, tradeNo)
                        .set(Order::getChannelTradeNo, tradeNo)
                        .set(Order::getPayTime, LocalDateTime.now()));

        // 推送商户回调
        notifyCallbackService.notifyPaid(orderNo);

        log.info("订单支付成功: orderNo={}, tradeNo={}", orderNo, tradeNo);
    }

    /** 交易关闭 — 更新订单为已失效 */
    private void handlePaymentClosed(String orderNo) {
        orderMapper.update(null,
                new LambdaUpdateWrapper<Order>()
                        .eq(Order::getOrderNo, orderNo)
                        .eq(Order::getOrderStatus, OrderStatusEnum.NEW.getCode())
                        .set(Order::getOrderStatus, OrderStatusEnum.EXPIRED.getCode()));

        log.info("订单交易关闭: orderNo={}", orderNo);
    }

    /** 从 HttpServletRequest 提取所有参数到 Map */
    private Map<String, String> extractParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                params.put(key, values[0]);
            }
        });
        return params;
    }
}
