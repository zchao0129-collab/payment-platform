package com.payment.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.platform.dto.resp.StatsResp;
import com.payment.platform.entity.Order;
import com.payment.platform.entity.Withdrawal;
import com.payment.platform.entity.Merchant;
import com.payment.platform.enums.OrderStatusEnum;
import com.payment.platform.mapper.OrderMapper;
import com.payment.platform.mapper.WithdrawalMapper;
import com.payment.platform.mapper.MerchantMapper;
import com.payment.platform.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final OrderMapper orderMapper;
    private final WithdrawalMapper withdrawalMapper;
    private final MerchantMapper merchantMapper;

    @Override
    public StatsResp revenueStats(Long merchantId) {
        LocalDateTime today = LocalDate.now().atStartOfDay();
        LocalDateTime weekStart = LocalDate.now().minusDays(6).atStartOfDay(); // 最近7天
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        LambdaQueryWrapper<Order> baseWrapper = new LambdaQueryWrapper<Order>()
                .ge(Order::getCreatedAt, monthStart)
                .in(Order::getOrderStatus,
                        OrderStatusEnum.PAID.getCode(),
                        OrderStatusEnum.CALLBACK.getCode());
        if (merchantId != null) {
            baseWrapper.eq(Order::getMerchantId, merchantId);
        }

        List<Order> allOrders = orderMapper.selectList(baseWrapper);
        // 今日成交
        BigDecimal todayAmount = allOrders.stream()
                .filter(o -> o.getCreatedAt().isAfter(today))
                .map(Order::getOrderAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int todayOrders = (int) allOrders.stream()
                .filter(o -> o.getCreatedAt().isAfter(today)).count();
        // 本周成交
        BigDecimal weekAmount = allOrders.stream()
                .filter(o -> o.getCreatedAt().isAfter(weekStart))
                .map(Order::getOrderAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int weekOrders = (int) allOrders.stream()
                .filter(o -> o.getCreatedAt().isAfter(weekStart)).count();
        // 本月成交
        BigDecimal monthAmount = allOrders.stream()
                .map(Order::getOrderAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 近7天趋势
        List<StatsResp.DailyData> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
            BigDecimal dayAmount = allOrders.stream()
                    .filter(o -> !o.getCreatedAt().isBefore(dayStart) && o.getCreatedAt().isBefore(dayEnd))
                    .map(Order::getOrderAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long dayCount = allOrders.stream()
                    .filter(o -> !o.getCreatedAt().isBefore(dayStart) && o.getCreatedAt().isBefore(dayEnd))
                    .count();
            StatsResp.DailyData dd = new StatsResp.DailyData();
            dd.setDate(date.toString());
            dd.setAmount(dayAmount);
            dd.setOrderCount((int) dayCount);
            trend.add(dd);
        }

        StatsResp resp = new StatsResp();
        resp.setTodayAmount(todayAmount);
        resp.setTodayOrders(todayOrders);
        resp.setWeekAmount(weekAmount);
        resp.setWeekOrders(weekOrders);
        int monthOrders = (int) allOrders.size();
        resp.setMonthAmount(monthAmount);
        resp.setMonthOrders(monthOrders);
        resp.setDailyTrend(trend);
        return resp;
    }

    @Override
    public List<Map<String, Object>> orderRankTop10() {
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .in(Order::getOrderStatus,
                                OrderStatusEnum.PAID.getCode(),
                                OrderStatusEnum.CALLBACK.getCode())
                        .ge(Order::getCreatedAt, LocalDate.now().minusDays(30).atStartOfDay()));

        Map<Long, Map<String, Object>> merchantMap = new LinkedHashMap<>();
        for (Order o : orders) {
            merchantMap.compute(o.getMerchantId(), (k, v) -> {
                if (v == null) {
                    v = new HashMap<>();
                    v.put("merchantId", o.getMerchantId());
                    v.put("merchantNo", o.getMerchantNo());
                    v.put("totalAmount", BigDecimal.ZERO);
                    v.put("orderCount", 0);
                }
                v.put("totalAmount", ((BigDecimal) v.get("totalAmount")).add(o.getOrderAmount()));
                v.put("orderCount", (int) v.get("orderCount") + 1);
                return v;
            });
        }
        return merchantMap.values().stream()
                .sorted(Comparator.comparing(m -> ((BigDecimal) m.get("totalAmount")).negate()))
                .limit(10)
                .peek(m -> {
                    Merchant merchant = merchantMapper.selectById((Long) m.get("merchantId"));
                    m.put("merchantName", merchant != null ? merchant.getMerchantName() : "");
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> withdrawRankTop10() {
        List<Withdrawal> withdrawals = withdrawalMapper.selectList(
                new LambdaQueryWrapper<Withdrawal>()
                        .eq(Withdrawal::getStatus, 2) // AUDIT_PASSED（已打款）
                        .ge(Withdrawal::getCreatedAt, LocalDate.now().minusDays(30).atStartOfDay()));

        Map<Long, Map<String, Object>> merchantMap = new LinkedHashMap<>();
        for (Withdrawal w : withdrawals) {
            merchantMap.compute(w.getMerchantId(), (k, v) -> {
                if (v == null) {
                    v = new HashMap<>();
                    v.put("merchantId", w.getMerchantId());
                    v.put("merchantNo", w.getMerchantNo());
                    v.put("totalAmount", BigDecimal.ZERO);
                    v.put("withdrawCount", 0);
                }
                v.put("totalAmount", ((BigDecimal) v.get("totalAmount")).add(w.getAmount()));
                v.put("withdrawCount", (int) v.get("withdrawCount") + 1);
                return v;
            });
        }
        return merchantMap.values().stream()
                .sorted(Comparator.comparing(m -> ((BigDecimal) m.get("totalAmount")).negate()))
                .limit(10)
                .peek(m -> {
                    Merchant merchant = merchantMapper.selectById((Long) m.get("merchantId"));
                    m.put("merchantName", merchant != null ? merchant.getMerchantName() : "");
                })
                .collect(Collectors.toList());
    }
}
