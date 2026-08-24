package com.payment.platform.controller;

import com.payment.platform.common.Result;
import com.payment.platform.entity.OrderAmountConfig;
import com.payment.platform.service.OrderAmountConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "订单金额浮动配置", description = "开放API订单金额浮动配置管理（仅管理员）")
@RestController
@RequestMapping("/api/admin/order-amount-config")
@RequiredArgsConstructor
public class OrderAmountConfigController {

    private final OrderAmountConfigService orderAmountConfigService;

    @Operation(summary = "查询金额浮动配置")
    @GetMapping
    public Result<OrderAmountConfig> get() {
        return Result.ok(orderAmountConfigService.get());
    }

    @Operation(summary = "保存金额浮动配置")
    @PutMapping
    public Result<Void> save(@RequestBody OrderAmountConfig config) {
        orderAmountConfigService.save(config);
        return Result.success("保存成功");
    }
}
