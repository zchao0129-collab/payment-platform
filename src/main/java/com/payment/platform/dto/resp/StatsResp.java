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

    private BigDecimal todayAmount;
    private Integer todayOrders;
    private BigDecimal weekAmount;
    private Integer weekOrders;
    private BigDecimal monthAmount;
    private Integer monthOrders;
    private List<DailyData> dailyTrend;

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
