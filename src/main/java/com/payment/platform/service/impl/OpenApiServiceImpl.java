package com.payment.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.platform.common.BusinessException;
import com.payment.platform.dto.req.OpenOrderCreateReq;
import com.payment.platform.dto.req.OpenOrderQueryReq;
import com.payment.platform.entity.Merchant;
import com.payment.platform.entity.Order;
import com.payment.platform.enums.AlipayTradeTypeEnum;
import com.payment.platform.enums.OrderStatusEnum;
import com.payment.platform.mapper.MerchantMapper;
import com.payment.platform.mapper.OrderMapper;
import com.payment.platform.service.AlipayPaymentService;
import com.payment.platform.service.OpenApiService;
import com.payment.platform.service.OrderService;
import com.payment.platform.service.WechatPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 开放API服务实现
 * <p>
 * 验签由 {@link com.payment.platform.security.ApiSignFilter} 统一完成，
 * 本类只处理业务逻辑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenApiServiceImpl implements OpenApiService {

    private final MerchantMapper merchantMapper;
    private final OrderMapper orderMapper;
    private final OrderService orderService;
    private final AlipayPaymentService alipayPaymentService;
    private final WechatPaymentService wechatPaymentService;

    @Value("${app.open-api-base-url}")
    private String openApiBaseUrl;

    @Value("${app.cashier-base-url}")
    private String cashierBaseUrl;

    @Override
    @Transactional
    public Map<String, Object> createOrder(OpenOrderCreateReq req) {
        Merchant merchant = merchantMapper.selectOne(
                new LambdaQueryWrapper<Merchant>()
                        .eq(Merchant::getMerchantNo, req.getAppId())
                        .last("LIMIT 1"));
        if (merchant == null || merchant.getStatus() == null || merchant.getStatus() != 1) {
            throw new BusinessException("商户不可用");
        }

        String productName = StringUtils.hasText(req.getProductName()) ? req.getProductName() : "扫码支付";
        BigDecimal amount = new BigDecimal(req.getAmount());
        String payChannel = StringUtils.hasText(req.getPayChannel()) ? req.getPayChannel().toUpperCase() : "ALIPAY";
        String tradeType = StringUtils.hasText(req.getTradeType()) ? req.getTradeType().toUpperCase() : "WAP";

        Order order = orderService.createOrder(merchant.getId(), productName, amount, null, req.getRemark());
        order.setPayChannel(payChannel);
        order.setTradeType(tradeType);
        if (StringUtils.hasText(req.getNotifyUrl())) {
            order.setNotifyUrl(req.getNotifyUrl());
        }
        if (StringUtils.hasText(req.getReturnUrl())) {
            order.setReturnUrl(req.getReturnUrl());
        }
        orderMapper.updateById(order);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderNo", order.getOrderNo());
        result.put("amount", order.getOrderAmount().toString());
        result.put("payChannel", payChannel);
        result.put("tradeType", tradeType);
        if (AlipayTradeTypeEnum.isF2F(tradeType)) {
            // 当面付：直接返回二维码内容，商户展示给顾客用支付宝 App 扫
            Map<String, String> f2f = alipayPaymentService.buildF2FPay(order.getOrderNo(), amount, productName);
            result.put("qrCode", f2f.getOrDefault("qrCode", ""));
        } else {
            result.put("payUrl", openApiBaseUrl + "/api/open/pay/" + order.getOrderNo());
        }
        log.info("开放API创建订单: appId={}, orderNo={}, channel={}, tradeType={}, amount={}",
                req.getAppId(), order.getOrderNo(), payChannel, tradeType, order.getOrderAmount());
        return result;
    }

    @Override
    public Map<String, Object> queryOrder(OpenOrderQueryReq req) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderNo, req.getOrderNo())
                        .last("LIMIT 1"));
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!req.getAppId().equals(order.getMerchantNo())) {
            throw new BusinessException(403, "无权查询该订单");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderNo", order.getOrderNo());
        result.put("orderStatus", order.getOrderStatus());
        result.put("orderAmount", order.getOrderAmount().toString());
        result.put("payChannel", order.getPayChannel());
        result.put("channelTradeNo", order.getChannelTradeNo());
        result.put("payTime", order.getPayTime());
        return result;
    }

    @Override
    public Map<String, String> pay(String orderNo, String clientIp) {
        Order order = orderService.getByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (order.getOrderStatus() == null || order.getOrderStatus() != OrderStatusEnum.NEW.getCode()) {
            throw new BusinessException("订单已支付或已失效");
        }
        String returnUrl = openApiBaseUrl + "/api/open/redirect/" + orderNo;
        Map<String, String> result = new HashMap<>();
        if ("WECHAT".equalsIgnoreCase(order.getPayChannel())) {
            Map<String, String> h5 = wechatPaymentService.buildH5Pay(
                    orderNo, order.getOrderAmount(), order.getProductName(), clientIp, returnUrl);
            result.put("mwebUrl", h5.get("mwebUrl"));
        } else if (AlipayTradeTypeEnum.isF2F(order.getTradeType())) {
            Map<String, String> f2f = alipayPaymentService.buildF2FPay(
                    orderNo, order.getOrderAmount(), order.getProductName());
            result.put("qrCode", f2f.getOrDefault("qrCode", ""));
        } else {
            Map<String, String> alipay = alipayPaymentService.buildWapPay(
                    orderNo, order.getOrderAmount(), order.getProductName(), returnUrl);
            result.put("alipayForm", alipay.getOrDefault("alipayForm", ""));
        }
        return result;
    }

    @Override
    public String getReturnUrl(String orderNo) {
        Order order = orderService.getByOrderNo(orderNo);
        if (order != null && StringUtils.hasText(order.getReturnUrl())) {
            return order.getReturnUrl();
        }
        return cashierBaseUrl + "/app/pay-result?orderNo=" + orderNo;
    }
}
