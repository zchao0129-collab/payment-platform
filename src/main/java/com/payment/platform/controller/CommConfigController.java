package com.payment.platform.controller;

import com.payment.platform.common.Result;
import com.payment.platform.entity.CommissionConfig;
import com.payment.platform.service.CommConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "返佣配置", description = "返佣配置管理（仅管理员）")
@RestController
@RequestMapping("/api/admin/comm-config")
@RequiredArgsConstructor
public class CommConfigController {

    private final CommConfigService commConfigService;

    @Operation(summary = "返佣区间列表")
    @GetMapping("/list")
    public Result<List<CommissionConfig>> list() {
        return Result.ok(commConfigService.listAll());
    }

    @Operation(summary = "新增区间")
    @PostMapping("/add")
    public Result<Void> add(@RequestBody CommissionConfig config) {
        commConfigService.add(config);
        return Result.success("添加成功");
    }

    @Operation(summary = "修改区间")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody CommissionConfig config) {
        config.setId(id);
        commConfigService.update(config);
        return Result.success("修改成功");
    }

    @Operation(summary = "删除区间")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commConfigService.delete(id);
        return Result.success("删除成功");
    }
}
