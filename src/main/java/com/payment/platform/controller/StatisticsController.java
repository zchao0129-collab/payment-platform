package com.payment.platform.controller;

import com.payment.platform.common.Result;
import com.payment.platform.dto.resp.StatsResp;
import com.payment.platform.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "统计", description = "统计分析")
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Operation(summary = "营收统计")
    @GetMapping("/revenue")
    public Result<StatsResp> revenue(@RequestAttribute(required = false) Long merchantId) {
        return Result.ok(statisticsService.revenueStats(merchantId));
    }

    @Operation(summary = "[管理端] 订单排行 TOP10")
    @GetMapping("/admin/order-rank")
    public Result<List<Map<String, Object>>> orderRank() {
        return Result.ok(statisticsService.orderRankTop10());
    }

    @Operation(summary = "[管理端] 提现排行 TOP10")
    @GetMapping("/admin/withdraw-rank")
    public Result<List<Map<String, Object>>> withdrawRank() {
        return Result.ok(statisticsService.withdrawRankTop10());
    }
}
