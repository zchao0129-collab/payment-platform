package com.payment.platform.service.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.payment.platform.common.utils.SignUtil;
import com.payment.platform.entity.Merchant;
import com.payment.platform.entity.Order;
import com.payment.platform.enums.OrderStatusEnum;
import com.payment.platform.mapper.MerchantMapper;
import com.payment.platform.mapper.OrderMapper;
import com.payment.platform.service.NotifyCallbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 商户回调推送实现
 * <p>
 * 支付成功后向商户 notify_url 推送结果（带 HMAC 签名），商户返回 SUCCESS 视为成功；
 * 失败标记 notify_status=2，由定时任务重试（最多 3 次）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyCallbackServiceImpl implements NotifyCallbackService {

    private static final int MAX_RETRY = 3;
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OrderMapper orderMapper;
    private final MerchantMapper merchantMapper;

    @Override
    public void notifyPaid(String orderNo) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo).last("LIMIT 1"));
        if (order == null) {
            return;
        }
        Merchant merchant = merchantMapper.selectById(order.getMerchantId());
        if (!isApiEnabled(merchant)) {
            return;
        }
        String notifyUrl = StringUtils.hasText(order.getNotifyUrl())
                ? order.getNotifyUrl() : merchant.getNotifyUrl();
        if (!StringUtils.hasText(notifyUrl)) {
            return;
        }
        doPush(order, merchant, notifyUrl);
    }

    @Override
    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void retryFailedCallbacks() {
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderStatus, OrderStatusEnum.PAID.getCode())
                        .eq(Order::getNotifyStatus, 2)
                        .lt(Order::getNotifyCount, MAX_RETRY)
                        .last("LIMIT 200"));
        for (Order order : orders) {
            try {
                Merchant merchant = merchantMapper.selectById(order.getMerchantId());
                if (!isApiEnabled(merchant)) {
                    continue;
                }
                String notifyUrl = StringUtils.hasText(order.getNotifyUrl())
                        ? order.getNotifyUrl() : merchant.getNotifyUrl();
                if (!StringUtils.hasText(notifyUrl)) {
                    continue;
                }
                doPush(order, merchant, notifyUrl);
            } catch (Exception e) {
                log.error("回调重试失败: orderNo={}", order.getOrderNo(), e);
            }
        }
    }

    private void doPush(Order order, Merchant merchant, String notifyUrl) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderNo", order.getOrderNo());
        body.put("merchantNo", order.getMerchantNo());
        body.put("orderStatus", order.getOrderStatus());
        body.put("amount", order.getOrderAmount().toString());
        body.put("payChannel", order.getPayChannel());
        body.put("channelTradeNo", order.getChannelTradeNo() == null ? "" : order.getChannelTradeNo());
        body.put("payTime", order.getPayTime() == null ? "" : order.getPayTime().format(DATE_TIME));

        // 用商户 api_secret 对回调参数签名
        Map<String, String> signParams = new LinkedHashMap<>();
        body.forEach((k, v) -> signParams.put(k, v == null ? "" : v.toString()));
        String sign = SignUtil.sign(signParams, merchant.getApiSecret());
        body.put("sign", sign);

        boolean success = false;
        try {
            String resp = HttpUtil.post(notifyUrl, JSONUtil.toJsonStr(body), 5000);
            success = "SUCCESS".equalsIgnoreCase(resp.trim());
        } catch (Exception e) {
            log.warn("回调推送异常: orderNo={}, notifyUrl={}", order.getOrderNo(), notifyUrl, e);
        }

        int newCount = (order.getNotifyCount() == null ? 0 : order.getNotifyCount()) + 1;
        orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getOrderNo, order.getOrderNo())
                .set(Order::getNotifyStatus, success ? 1 : 2)
                .set(Order::getNotifyCount, newCount)
                .set(Order::getNotifyTime, LocalDateTime.now()));
        log.info("回调推送完成: orderNo={}, success={}, count={}", order.getOrderNo(), success, newCount);
    }

    private boolean isApiEnabled(Merchant merchant) {
        return merchant != null && merchant.getApiEnabled() != null && merchant.getApiEnabled() == 1
                && StringUtils.hasText(merchant.getApiSecret());
    }
}
