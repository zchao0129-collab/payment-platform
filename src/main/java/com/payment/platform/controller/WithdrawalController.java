package com.payment.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.platform.common.PageResult;
import com.payment.platform.common.Result;
import com.payment.platform.dto.req.AuditReq;
import com.payment.platform.entity.Withdrawal;
import com.payment.platform.service.WithdrawalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "提现", description = "提现管理")
@RestController
@RequestMapping("/api/withdrawal")
@RequiredArgsConstructor
public class WithdrawalController {

    private final WithdrawalService withdrawalService;

    @Operation(summary = "提现记录列表")
    @GetMapping("/list")
    public Result<PageResult<Withdrawal>> list(@RequestAttribute(required = false) Long merchantId,
                                                @RequestParam(required = false) Integer status,
                                                @RequestParam(defaultValue = "1") Long page,
                                                @RequestParam(defaultValue = "20") Long size) {
        Page<Withdrawal> result = withdrawalService.queryPage(merchantId, status, page, size);
        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @Operation(summary = "[管理端] 审核通过")
    @PostMapping("/admin/approve")
    public Result<Void> approve(@Valid @RequestBody AuditReq req,
                                 @RequestAttribute Long userId) {
        withdrawalService.approve(req.getWithdrawalId(), userId);
        return Result.success("审核通过，已打款");
    }

    @Operation(summary = "[管理端] 审核驳回")
    @PostMapping("/admin/reject")
    public Result<Void> reject(@Valid @RequestBody AuditReq req,
                                @RequestAttribute Long userId) {
        withdrawalService.reject(req.getWithdrawalId(), userId, req.getRejectReason());
        return Result.success("已驳回");
    }
}
