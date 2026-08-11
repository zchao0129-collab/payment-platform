package com.payment.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.platform.common.PageResult;
import com.payment.platform.common.Result;
import com.payment.platform.dto.req.OrderQueryReq;
import com.payment.platform.entity.Order;
import com.payment.platform.service.AlipayPaymentService;
import com.payment.platform.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Tag(name = "订单", description = "订单管理")
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final AlipayPaymentService alipayPaymentService;

    @Value("${app.cashier-base-url}")
    private String cashierBaseUrl;

    @Operation(summary = "收银台创建订单（公开接口）— 创建本地订单并获取支付宝支付链接")
    @PostMapping("/create")
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> params) {
        Long merchantId = Long.valueOf(params.get("merchantId").toString());
        String productName = (String) params.getOrDefault("productName", "扫码支付");
        BigDecimal amount = new BigDecimal(params.get("amount").toString());
        Long qrcodeId = params.get("qrcodeId") != null ? Long.valueOf(params.get("qrcodeId").toString()) : null;
        String remark = (String) params.getOrDefault("remark", "");

        // 1. 创建本地订单
        Order order = orderService.createOrder(merchantId, productName, amount, qrcodeId, remark);

        // 2. 构建支付宝回跳地址（先到后端处理，后端验证后重定向到前端结果页）
        String returnUrl = cashierBaseUrl + "/api/alipay/return";

        // 3. 调用支付宝创建支付订单，获取支付表单（HTML 自动提交表单）
        String alipayForm = "";
        try {
            Map<String, String> alipayResult = alipayPaymentService.buildWapPay(
                    order.getOrderNo(), order.getOrderAmount(), order.getProductName(), returnUrl);
            alipayForm = alipayResult.getOrDefault("alipayForm", "");
        } catch (Exception e) {
            log.error("创建支付宝支付订单失败: orderNo={}", order.getOrderNo(), e);
            // 支付宝调用失败不阻塞订单创建，返回空表单由前端展示兜底页面
        }

        // 4. 返回结果
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderNo", order.getOrderNo());
        result.put("amount", order.getOrderAmount().toString());
        result.put("alipayForm", alipayForm);

        log.info("订单创建完成: orderNo={}, amount={}, hasAlipayForm={}",
                order.getOrderNo(), order.getOrderAmount(), !alipayForm.isEmpty());
        return Result.ok(result);
    }

    @Operation(summary = "订单列表")
    @GetMapping("/list")
    public Result<PageResult<Order>> list(OrderQueryReq req,
                                           @RequestAttribute(required = false) Long userId,
                                           @RequestAttribute(required = false) Long merchantId) {
        Page<Order> page = orderService.queryPage(req, merchantId);
        return Result.ok(PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @Operation(summary = "查询订单状态（公开接口 — 支付结果页轮询）")
    @GetMapping("/status")
    public Result<Map<String, Object>> status(@RequestParam String orderNo) {
        Order order = orderService.getByOrderNo(orderNo);
        Map<String, Object> result = new LinkedHashMap<>();
        if (order != null) {
            result.put("orderNo", order.getOrderNo());
            result.put("orderStatus", order.getOrderStatus());
            result.put("orderAmount", order.getOrderAmount());
        }
        return Result.ok(result);
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public Result<Order> detail(@PathVariable Long id) {
        return Result.ok(orderService.getDetail(id));
    }

    @Operation(summary = "全额退款")
    @PostMapping("/{id}/refund")
    public Result<Void> refund(@PathVariable Long id) {
        orderService.refund(id);
        return Result.success("退款申请已提交");
    }
}
