package com.payment.platform.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsResp {

    // ===== 兼容旧字段（总计，商户端/统计页仍在使用）=====
    private BigDecimal todayAmount;
    private Integer todayOrders;
    private BigDecimal weekAmount;
    private Integer weekOrders;
    private BigDecimal monthAmount;
    private Integer monthOrders;
    private List<DailyData> dailyTrend;

    // ===== 新增：按来源（API/码牌）分区间统计 =====
    private PeriodStat today;
    private PeriodStat yesterday;
    private PeriodStat week;
    private PeriodStat month;
    private List<TrendPoint> weekTrend;
    private List<TrendPoint> monthTrend;

    /** 某周期统计：金额/笔数/成率，均按来源（API/码牌）拆分 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeriodStat {
        private BigDecimal totalAmount;
        private BigDecimal apiAmount;
        private BigDecimal cashierAmount;
        /** 订单总笔数（所有状态） */
        private Integer totalOrders;
        private Integer apiOrders;
        private Integer cashierOrders;
        /** 已支付笔数（状态2已支付/3已回调） */
        private Integer totalPaid;
        /** 成率(%) = 已支付 / 订单总笔数 × 100 */
        private BigDecimal totalRate;
        private BigDecimal apiRate;
        private BigDecimal cashierRate;
    }

    /** 曲线单点：某日 API/码牌 已支付金额 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        private String date;
        private BigDecimal apiAmount;
        private BigDecimal cashierAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyData {
        private String date;
        private BigDecimal amount;
        private Integer orderCount;
    }
}
