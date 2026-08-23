package com.payment.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.platform.common.BusinessException;
import com.payment.platform.common.PageResult;
import com.payment.platform.common.Result;
import com.payment.platform.common.utils.SignUtil;
import com.payment.platform.dto.req.OpenOrderCreateReq;
import com.payment.platform.dto.req.OrderQueryReq;
import com.payment.platform.entity.Merchant;
import com.payment.platform.entity.Order;
import com.payment.platform.mapper.MerchantMapper;
import com.payment.platform.mapper.OrderMapper;
import com.payment.platform.service.NotifyCallbackService;
import com.payment.platform.service.OpenApiService;
import com.payment.platform.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Tag(name = "订单", description = "订单管理")
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;
    private final MerchantMapper merchantMapper;
    private final NotifyCallbackService notifyCallbackService;
    private final OpenApiService openApiService;

    @Operation(summary = "收银台创建订单（公开接口）— 创建本地订单并获取支付参数")
    @PostMapping("/create")
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> params) {
        Long merchantId = Long.valueOf(params.get("merchantId").toString());
        String productName = (String) params.getOrDefault("productName", "扫码支付");
        BigDecimal amount = new BigDecimal(params.get("amount").toString());
        Long qrcodeId = params.get("qrcodeId") != null ? Long.valueOf(params.get("qrcodeId").toString()) : null;
        String remark = (String) params.getOrDefault("remark", "");
        // 支付通道: ALIPAY 或 WECHAT（前端根据 UA 检测传入）
        String payChannel = ((String) params.getOrDefault("payChannel", "ALIPAY")).toUpperCase();
        // 支付宝交易类型: WAP(手机网站支付,默认) / F2F(当面付)；微信通道忽略
        String tradeType = ((String) params.getOrDefault("tradeType", "WAP")).toUpperCase();
        String openid = (String) params.get("openid");

        // 1. 创建本地订单（标记支付通道 + 交易类型）
        Order order = orderService.createOrder(merchantId, productName, amount, qrcodeId, remark);
        order.setPayChannel(payChannel);
        order.setTradeType(tradeType);
        orderMapper.updateById(order);

        // 2. 按通道构建支付参数
        Map<String, Object> result = orderService.buildPaymentParams(order, payChannel, openid, getClientIp());

        log.info("订单创建完成: orderNo={}, channel={}, amount={}",
                order.getOrderNo(), payChannel, order.getOrderAmount());
        return Result.ok(result);
    }

    private String getClientIp() {
        // 简化处理，实际应从 request 获取
        return "127.0.0.1";
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

    // ======================== 管理端 ========================

    @Operation(summary = "[管理端] 测试订单（调用开放API下单，返回支付链接）")
    @PostMapping("/admin/test-create")
    public Result<Map<String, Object>> adminTestCreate(@RequestBody Map<String, Object> params) {
        Object amountObj = params.get("amount");
        String amount = amountObj == null ? "1.00" : amountObj.toString();
        String payChannel = String.valueOf(params.getOrDefault("payChannel", "ALIPAY")).toUpperCase();
        String tradeType = String.valueOf(params.getOrDefault("tradeType", "WAP")).toUpperCase();
        String merchantNo = (String) params.get("merchantNo");

        Merchant merchant;
        if (StringUtils.hasText(merchantNo)) {
            merchant = merchantMapper.selectOne(
                    new LambdaQueryWrapper<Merchant>()
                            .eq(Merchant::getMerchantNo, merchantNo)
                            .last("LIMIT 1"));
        } else {
            // 默认取第一个已开通开放API且已配置密钥的商户
            merchant = merchantMapper.selectOne(
                    new LambdaQueryWrapper<Merchant>()
                            .eq(Merchant::getStatus, 1)
                            .eq(Merchant::getApiEnabled, 1)
                            .isNotNull(Merchant::getApiSecret)
                            .ne(Merchant::getApiSecret, "")
                            .last("LIMIT 1"));
        }
        if (merchant == null || !StringUtils.hasText(merchant.getApiSecret())) {
            throw new BusinessException("暂无可用测试商户（需开通开放API并配置密钥）");
        }

        // 构造开放API下单请求（与服务端验签逻辑一致）
        OpenOrderCreateReq req = new OpenOrderCreateReq();
        req.setAppId(merchant.getMerchantNo());
        req.setTimestamp(String.valueOf(System.currentTimeMillis()));
        req.setNonce(UUID.randomUUID().toString().replace("-", ""));
        req.setAmount(amount);
        req.setPayChannel(payChannel);
        req.setTradeType(tradeType);
        req.setProductName("测试订单");

        Map<String, String> signParams = new LinkedHashMap<>();
        signParams.put("appId", req.getAppId());
        signParams.put("timestamp", req.getTimestamp());
        signParams.put("nonce", req.getNonce());
        signParams.put("amount", req.getAmount());
        signParams.put("payChannel", req.getPayChannel());
        signParams.put("tradeType", req.getTradeType());
        signParams.put("productName", req.getProductName());
        req.setSign(SignUtil.sign(signParams, merchant.getApiSecret()));

        Map<String, Object> result = new LinkedHashMap<>(openApiService.createOrder(req));
        result.put("merchantNo", merchant.getMerchantNo());
        return Result.ok(result);
    }

    @Operation(summary = "[管理端] 手动触发订单回调推送")
    @PostMapping("/admin/callback/{orderNo}")
    public Result<Map<String, Object>> adminCallback(@PathVariable String orderNo) {
        Order order = orderService.getByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        notifyCallbackService.notifyPaid(orderNo);
        Order updated = orderService.getByOrderNo(orderNo);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderNo", orderNo);
        result.put("notifyStatus", updated.getNotifyStatus());
        result.put("notifyCount", updated.getNotifyCount());
        result.put("notifyTime", updated.getNotifyTime());
        return Result.ok(result);
    }
}
