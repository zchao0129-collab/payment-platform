package com.payment.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.platform.common.BusinessException;
import com.payment.platform.common.utils.CodeGenerator;
import com.payment.platform.dto.req.OrderQueryReq;
import com.payment.platform.entity.*;
import com.payment.platform.enums.AlipayTradeTypeEnum;
import com.payment.platform.enums.OrderSourceEnum;
import com.payment.platform.enums.OrderStatusEnum;
import com.payment.platform.mapper.*;
import com.payment.platform.service.AlipayPaymentService;
import com.payment.platform.service.OrderService;
import com.payment.platform.service.WechatPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final MerchantMapper merchantMapper;
    private final PaymentLogMapper paymentLogMapper;
    private final RedissonClient redissonClient;
    private final AlipayPaymentService alipayPaymentService;
    private final WechatPaymentService wechatPaymentService;

    @Value("${app.cashier-base-url}")
    private String cashierBaseUrl;

    @Override
    @Transactional
    public Order createOrder(Long merchantId, String productName, BigDecimal amount, Long qrcodeId, String remark) {
        // 校验商户
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null || merchant.getStatus() != 1) {
            throw new BusinessException("商户不可用");
        }
        if (amount.compareTo(BigDecimal.valueOf(0.01)) < 0) {
            throw new BusinessException("金额不能小于0.01元");
        }
        Order order = new Order();
        order.setOrderNo(CodeGenerator.generateOrderNo());
        order.setMerchantId(merchantId);
        order.setMerchantNo(merchant.getMerchantNo());
        order.setProductName(productName != null ? productName : "");
        order.setOrderAmount(amount);
        order.setOrderStatus(OrderStatusEnum.NEW.getCode());
        order.setExpireTime(LocalDateTime.now().plusMinutes(5));
        order.setQrcodeId(qrcodeId);
        order.setRemark(remark);
        // 默认订单来源为收银台/码牌；开放API下单会在调用方覆盖为 OPEN_API
        order.setOrderSource(OrderSourceEnum.CASHIER.getCode());
        orderMapper.insert(order);
        log.info("订单创建: orderNo={}, amount={}", order.getOrderNo(), amount);
        return order;
    }

    @Override
    public Map<String, Object> buildPaymentParams(Order order, String payChannel, String openid, String clientIp) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderNo", order.getOrderNo());
        result.put("amount", order.getOrderAmount().toString());
        result.put("payChannel", payChannel);

        if ("WECHAT".equalsIgnoreCase(payChannel)) {
            try {
                if (openid != null && !openid.isBlank()) {
                    Map<String, String> jsapiResult = wechatPaymentService.buildJSAPIPay(
                            order.getOrderNo(), order.getOrderAmount(), order.getProductName(),
                            openid, clientIp);
                    result.putAll(jsapiResult);
                    result.put("payType", "jsapi");
                } else {
                    String returnUrl = cashierBaseUrl + "/app/pay-result?orderNo=" + order.getOrderNo();
                    Map<String, String> h5Result = wechatPaymentService.buildH5Pay(
                            order.getOrderNo(), order.getOrderAmount(), order.getProductName(),
                            clientIp, returnUrl);
                    result.put("mwebUrl", h5Result.get("mwebUrl"));
                    result.put("payType", "h5");
                }
            } catch (Exception e) {
                log.error("创建微信支付订单失败: orderNo={}", order.getOrderNo(), e);
                result.put("payError", e.getMessage());
            }
        } else if (AlipayTradeTypeEnum.isF2F(order.getTradeType())) {
            // 支付宝当面付：预下单返回二维码内容
            try {
                Map<String, String> f2fResult = alipayPaymentService.buildF2FPay(
                        order.getOrderNo(), order.getOrderAmount(), order.getProductName());
                result.put("qrCode", f2fResult.getOrDefault("qrCode", ""));
                result.put("tradeType", "F2F");
            } catch (Exception e) {
                log.error("创建支付宝当面付订单失败: orderNo={}", order.getOrderNo(), e);
                result.put("payError", e.getMessage());
            }
        } else {
            // 支付宝手机网站支付（默认 WAP）
            String returnUrl = cashierBaseUrl + "/api/alipay/return";
            try {
                Map<String, String> alipayResult = alipayPaymentService.buildWapPay(
                        order.getOrderNo(), order.getOrderAmount(), order.getProductName(), returnUrl);
                result.put("alipayForm", alipayResult.getOrDefault("alipayForm", ""));
                result.put("tradeType", "WAP");
            } catch (Exception e) {
                log.error("创建支付宝支付订单失败: orderNo={}", order.getOrderNo(), e);
                result.put("payError", e.getMessage());
            }
        }
        return result;
    }

    @Override
    @Transactional
    public void handleAlipayNotify(String notifyContent) {
        // 解析支付宝回调参数
        // Map<String, String> params = parseNotifyParams(notifyContent);
        // String orderNo = params.get("out_trade_no");
        // String tradeNo = params.get("trade_no");
        // String tradeStatus = params.get("trade_status");
        // TODO: 验证签名 → 更新订单状态 → 记录回调日志
        log.info("收到支付宝回调: {}", notifyContent);
    }

    @Override
    public Page<Order> queryPage(OrderQueryReq req, Long merchantId) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (merchantId != null) {
            wrapper.eq(Order::getMerchantId, merchantId);
        }
        if (req.getOrderNo() != null && !req.getOrderNo().isBlank()) {
            wrapper.eq(Order::getOrderNo, req.getOrderNo());
        }
        if (req.getProductName() != null && !req.getProductName().isBlank()) {
            wrapper.like(Order::getProductName, req.getProductName());
        }
        if (req.getOrderStatus() != null) {
            wrapper.eq(Order::getOrderStatus, req.getOrderStatus());
        }
        if (req.getOrderAmount() != null) {
            wrapper.eq(Order::getOrderAmount, req.getOrderAmount());
        }
        if (req.getStartTime() != null) {
            wrapper.ge(Order::getCreatedAt, req.getStartTime());
        }
        if (req.getEndTime() != null) {
            wrapper.le(Order::getCreatedAt, req.getEndTime());
        }
        wrapper.orderByDesc(Order::getCreatedAt);
        return orderMapper.selectPage(new Page<>(req.getPage(), req.getSize()), wrapper);
    }

    @Override
    public Order getByOrderNo(String orderNo) {
        return orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderNo, orderNo)
                        .last("LIMIT 1"));
    }

    @Override
    public Order getDetail(Long orderId) {
        return orderMapper.selectById(orderId);
    }

    @Override
    @Transactional
    public void refund(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getOrderStatus() != OrderStatusEnum.PAID.getCode()
                && order.getOrderStatus() != OrderStatusEnum.CALLBACK.getCode()) {
            throw new BusinessException("仅已支付/已回调的订单可退款");
        }
        // 仅全额退款
        order.setOrderStatus(OrderStatusEnum.REFUNDED.getCode());
        order.setRefundAmount(order.getOrderAmount());
        order.setRefundTime(LocalDateTime.now());
        orderMapper.updateById(order);
        log.info("订单退款: orderNo={}, amount={}", order.getOrderNo(), order.getOrderAmount());
    }

    @Override
    public void expireOrders() {
        RLock lock = redissonClient.getLock("order:expire:lock");
        try {
            if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                try {
                    int count = orderMapper.update(null,
                            new LambdaUpdateWrapper<Order>()
                                    .eq(Order::getOrderStatus, OrderStatusEnum.NEW.getCode())
                                    .lt(Order::getExpireTime, LocalDateTime.now())
                                    .set(Order::getOrderStatus, OrderStatusEnum.EXPIRED.getCode()));
                    if (count > 0) {
                        log.info("超时失效订单处理完成: {} 笔", count);
                    }
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
