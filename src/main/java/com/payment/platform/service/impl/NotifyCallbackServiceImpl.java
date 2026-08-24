package com.payment.platform.service.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.payment.platform.common.utils.SignUtil;
import com.payment.platform.entity.Merchant;
import com.payment.platform.entity.Order;
import com.payment.platform.enums.OrderSourceEnum;
import com.payment.platform.enums.OrderStatusEnum;
import com.payment.platform.mapper.MerchantMapper;
import com.payment.platform.mapper.OrderMapper;
import com.payment.platform.service.NotifyCallbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
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
 * 支付成功后向商户 notify_url 推送结果（带签名），商户返回 SUCCESS 视为成功。
 * <ul>
 *   <li>首次推送：{@link #notifyPaid} 异步执行，不阻塞支付宝/微信的异步通知响应。</li>
 *   <li>失败重试：{@link #retryFailedCallbacks} 定时扫描「开放API、已支付、未回调成功」的订单，
 *       按 1秒→10秒→30秒→1分钟→5分钟→15分钟→30分钟 的递增间隔重试。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyCallbackServiceImpl implements NotifyCallbackService {

    /** 重试间隔（秒），与 notify_count 对应：第 n 次失败后，间隔 RETRY_INTERVAL_SECONDS[n-1] 再重试 */
    private static final long[] RETRY_INTERVAL_SECONDS = {1, 10, 30, 60, 300, 900, 1800};

    /** 最大尝试次数 = 1 次首次推送 + 7 次重试 */
    private static final int MAX_ATTEMPTS = RETRY_INTERVAL_SECONDS.length + 1;

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OrderMapper orderMapper;
    private final MerchantMapper merchantMapper;

    @Override
    @Async("notifyCallbackExecutor")
    public void notifyPaid(String orderNo) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo).last("LIMIT 1"));
        if (order == null) {
            return;
        }
        pushCallback(order);
    }

    @Override
    @Scheduled(fixedDelay = 1_000, initialDelay = 5_000)
    public void retryFailedCallbacks() {
        // 只扫「开放API、已支付、回调失败待重试、未超过最大次数」的订单
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderSource, OrderSourceEnum.OPEN_API.getCode())
                        .eq(Order::getOrderStatus, OrderStatusEnum.PAID.getCode())
                        .eq(Order::getNotifyStatus, 2)
                        .lt(Order::getNotifyCount, MAX_ATTEMPTS)
                        .last("LIMIT 200"));
        LocalDateTime now = LocalDateTime.now();
        for (Order order : orders) {
            if (!isDue(order, now)) {
                continue;
            }
            try {
                pushCallback(order);
            } catch (Exception e) {
                log.error("回调重试失败: orderNo={}", order.getOrderNo(), e);
            }
        }
    }

    /** 单个订单推送回调：查商户 → 校验API启用 → 解析回调地址 → 推送 */
    private void pushCallback(Order order) {
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

    /** 判断订单是否到重试时间点：已尝试 count 次，间隔 RETRY_INTERVAL_SECONDS[count-1] 后重试 */
    private boolean isDue(Order order, LocalDateTime now) {
        int count = order.getNotifyCount() == null ? 0 : order.getNotifyCount();
        if (count <= 0 || count > RETRY_INTERVAL_SECONDS.length) {
            return false;
        }
        LocalDateTime last = order.getNotifyTime();
        if (last == null) {
            return true;
        }
        return !last.plusSeconds(RETRY_INTERVAL_SECONDS[count - 1]).isAfter(now);
    }

    private void doPush(Order order, Merchant merchant, String notifyUrl) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderNo", order.getOrderNo());
        body.put("merchantOrderNo", order.getMerchantOrderNo() == null ? "" : order.getMerchantOrderNo());
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
