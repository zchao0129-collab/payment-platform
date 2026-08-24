package com.payment.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.platform.dto.resp.StatsResp;
import com.payment.platform.entity.Order;
import com.payment.platform.entity.Withdrawal;
import com.payment.platform.entity.Merchant;
import com.payment.platform.enums.OrderSourceEnum;
import com.payment.platform.enums.OrderStatusEnum;
import com.payment.platform.mapper.OrderMapper;
import com.payment.platform.mapper.WithdrawalMapper;
import com.payment.platform.mapper.MerchantMapper;
import com.payment.platform.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime yesterdayStart = today.minusDays(1).atStartOfDay();
        LocalDateTime weekStart = today.minusDays(6).atStartOfDay(); // 最近7天
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        // 查询范围取「本月」与「昨日」更早者（月初时昨日在上月）
        LocalDateTime rangeStart = monthStart.isBefore(yesterdayStart) ? monthStart : yesterdayStart;

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .ge(Order::getCreatedAt, rangeStart);
        if (merchantId != null) {
            wrapper.eq(Order::getMerchantId, merchantId);
        }
        List<Order> allOrders = orderMapper.selectList(wrapper);

        StatsResp resp = new StatsResp();
        resp.setToday(buildPeriod(allOrders, todayStart, null));
        resp.setYesterday(buildPeriod(allOrders, yesterdayStart, todayStart));
        resp.setWeek(buildPeriod(allOrders, weekStart, null));
        resp.setMonth(buildPeriod(allOrders, monthStart, null));

        // 兼容旧字段（总计）
        resp.setTodayAmount(resp.getToday().getTotalAmount());
        resp.setTodayOrders(resp.getToday().getTotalPaid());
        resp.setWeekAmount(resp.getWeek().getTotalAmount());
        resp.setWeekOrders(resp.getWeek().getTotalPaid());
        resp.setMonthAmount(resp.getMonth().getTotalAmount());
        resp.setMonthOrders(resp.getMonth().getTotalPaid());

        // 近7天趋势（旧结构，保留兼容）
        resp.setDailyTrend(buildDailyTrend(allOrders, today));

        // 本周曲线（近7天，按来源拆分）
        List<StatsResp.TrendPoint> weekTrend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            weekTrend.add(buildTrendPoint(allOrders, today.minusDays(i)));
        }
        resp.setWeekTrend(weekTrend);

        // 本月曲线（1号至今，按来源拆分）
        List<StatsResp.TrendPoint> monthTrend = new ArrayList<>();
        for (int d = 1; d <= today.getDayOfMonth(); d++) {
            monthTrend.add(buildTrendPoint(allOrders, today.withDayOfMonth(d)));
        }
        resp.setMonthTrend(monthTrend);

        return resp;
    }

    /** 计算某时间区间内的统计（含来源拆分与成率） */
    private StatsResp.PeriodStat buildPeriod(List<Order> all, LocalDateTime start, LocalDateTime end) {
        StatsResp.PeriodStat s = new StatsResp.PeriodStat();
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal apiAmount = BigDecimal.ZERO;
        BigDecimal cashierAmount = BigDecimal.ZERO;
        int totalOrders = 0, apiOrders = 0, cashierOrders = 0;
        int totalPaid = 0, apiPaid = 0, cashierPaid = 0;

        for (Order o : all) {
            LocalDateTime ct = o.getCreatedAt();
            if (ct.isBefore(start) || (end != null && !ct.isBefore(end))) {
                continue;
            }
            boolean isApi = OrderSourceEnum.isOpenApi(o.getOrderSource());
            boolean paid = isPaid(o.getOrderStatus());
            if (isApi) {
                apiOrders++;
            } else {
                cashierOrders++;
            }
            totalOrders++;
            if (paid) {
                BigDecimal amt = o.getOrderAmount() == null ? BigDecimal.ZERO : o.getOrderAmount();
                totalPaid++;
                totalAmount = totalAmount.add(amt);
                if (isApi) {
                    apiPaid++;
                    apiAmount = apiAmount.add(amt);
                } else {
                    cashierPaid++;
                    cashierAmount = cashierAmount.add(amt);
                }
            }
        }

        s.setTotalAmount(totalAmount);
        s.setApiAmount(apiAmount);
        s.setCashierAmount(cashierAmount);
        s.setTotalOrders(totalOrders);
        s.setApiOrders(apiOrders);
        s.setCashierOrders(cashierOrders);
        s.setTotalPaid(totalPaid);
        s.setTotalRate(rate(totalPaid, totalOrders));
        s.setApiRate(rate(apiPaid, apiOrders));
        s.setCashierRate(rate(cashierPaid, cashierOrders));
        return s;
    }

    /** 近7天趋势（旧结构：金额+笔数，仅已支付） */
    private List<StatsResp.DailyData> buildDailyTrend(List<Order> all, LocalDate today) {
        List<StatsResp.DailyData> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
            BigDecimal dayAmount = BigDecimal.ZERO;
            int dayCount = 0;
            for (Order o : all) {
                LocalDateTime ct = o.getCreatedAt();
                if (ct.isBefore(dayStart) || !ct.isBefore(dayEnd) || !isPaid(o.getOrderStatus())) {
                    continue;
                }
                dayAmount = dayAmount.add(o.getOrderAmount() == null ? BigDecimal.ZERO : o.getOrderAmount());
                dayCount++;
            }
            StatsResp.DailyData dd = new StatsResp.DailyData();
            dd.setDate(date.toString());
            dd.setAmount(dayAmount);
            dd.setOrderCount(dayCount);
            trend.add(dd);
        }
        return trend;
    }

    /** 曲线单点：某日 API/码牌 已支付金额 */
    private StatsResp.TrendPoint buildTrendPoint(List<Order> all, LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
        BigDecimal apiAmount = BigDecimal.ZERO;
        BigDecimal cashierAmount = BigDecimal.ZERO;
        for (Order o : all) {
            LocalDateTime ct = o.getCreatedAt();
            if (ct.isBefore(dayStart) || !ct.isBefore(dayEnd) || !isPaid(o.getOrderStatus())) {
                continue;
            }
            BigDecimal amt = o.getOrderAmount() == null ? BigDecimal.ZERO : o.getOrderAmount();
            if (OrderSourceEnum.isOpenApi(o.getOrderSource())) {
                apiAmount = apiAmount.add(amt);
            } else {
                cashierAmount = cashierAmount.add(amt);
            }
        }
        StatsResp.TrendPoint p = new StatsResp.TrendPoint();
        p.setDate(date.toString());
        p.setApiAmount(apiAmount);
        p.setCashierAmount(cashierAmount);
        return p;
    }

    /** 是否已支付（已支付/已回调） */
    private boolean isPaid(Integer status) {
        return status != null
                && (status == OrderStatusEnum.PAID.getCode() || status == OrderStatusEnum.CALLBACK.getCode());
    }

    /** 成率(%) = 已支付笔数 / 订单总笔数 × 100 */
    private BigDecimal rate(int paid, int total) {
        if (total == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(paid * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
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
