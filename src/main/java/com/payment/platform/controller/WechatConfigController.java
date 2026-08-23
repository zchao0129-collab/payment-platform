package com.payment.platform.controller;

import com.payment.platform.common.Result;
import com.payment.platform.entity.WechatConfig;
import com.payment.platform.service.WechatConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "微信支付配置", description = "微信支付配置管理（仅管理员）")
@RestController
@RequestMapping("/api/admin/wechat-config")
@RequiredArgsConstructor
public class WechatConfigController {

    private final WechatConfigService wechatConfigService;

    @Operation(summary = "配置列表")
    @GetMapping("/list")
    public Result<List<WechatConfig>> list() {
        return Result.ok(wechatConfigService.listAll());
    }

    @Operation(summary = "保存配置")
    @PostMapping("/save")
    public Result<Void> save(@RequestBody WechatConfig config) {
        wechatConfigService.save(config);
        return Result.success("保存成功");
    }

    @Operation(summary = "删除配置")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        wechatConfigService.delete(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "启用配置")
    @PutMapping("/{id}/enable")
    public Result<Void> enable(@PathVariable Long id) {
        wechatConfigService.enable(id);
        return Result.success("已启用");
    }

    @Operation(summary = "切换配置状态")
    @PutMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        wechatConfigService.updateStatus(id, status);
        return Result.success(status == 1 ? "已启用" : "已停用");
    }

    @Operation(summary = "连通性测试")
    @PostMapping("/{id}/test")
    public Result<Boolean> testConnection(@PathVariable Long id) {
        boolean ok = wechatConfigService.testConnection(id);
        return Result.ok(ok ? "连接成功" : "连接失败", ok);
    }
}
