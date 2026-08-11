package com.payment.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.platform.common.PageResult;
import com.payment.platform.common.Result;
import com.payment.platform.entity.Commission;
import com.payment.platform.service.CommissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Tag(name = "佣金", description = "佣金管理")
@RestController
@RequestMapping("/api/commission")
@RequiredArgsConstructor
public class CommissionController {

    private final CommissionService commissionService;

    @Operation(summary = "佣金列表")
    @GetMapping("/list")
    public Result<PageResult<Commission>> list(@RequestAttribute Long merchantId,
                                                @RequestParam(defaultValue = "1") Long page,
                                                @RequestParam(defaultValue = "20") Long size) {
        Page<Commission> result = commissionService.queryPage(merchantId, page, size);
        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @Operation(summary = "佣金汇总")
    @GetMapping("/summary")
    public Result<Map<String, BigDecimal>> summary(@RequestAttribute Long merchantId) {
        return Result.ok(Map.of(
                "total", commissionService.getTotalCommission(merchantId),
                "withdrawable", commissionService.getWithdrawableAmount(merchantId),
                "withdrawn", commissionService.getWithdrawnAmount(merchantId),
                "auditing", commissionService.getAuditingAmount(merchantId)
        ));
    }

    @Operation(summary = "发起提现")
    @PostMapping("/withdraw")
    public Result<Void> withdraw(@RequestAttribute Long merchantId,
                                  @RequestBody Map<String, BigDecimal> params) {
        commissionService.withdraw(merchantId, params.get("amount"));
        return Result.success("提现申请已提交，等待管理员审核");
    }
}
