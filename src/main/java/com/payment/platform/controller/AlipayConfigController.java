package com.payment.platform.controller;

import com.payment.platform.common.Result;
import com.payment.platform.entity.AlipayConfig;
import com.payment.platform.service.AlipayConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "支付宝配置", description = "支付宝配置管理（仅管理员）")
@RestController
@RequestMapping("/api/admin/alipay-config")
@RequiredArgsConstructor
public class AlipayConfigController {

    private final AlipayConfigService alipayConfigService;

    @Operation(summary = "配置列表")
    @GetMapping("/list")
    public Result<List<AlipayConfig>> list() {
        return Result.ok(alipayConfigService.listAll());
    }

    @Operation(summary = "保存配置")
    @PostMapping("/save")
    public Result<Void> save(@RequestBody AlipayConfig config) {
        alipayConfigService.save(config);
        return Result.success("保存成功");
    }

    @Operation(summary = "删除配置")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        alipayConfigService.delete(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "启用配置")
    @PutMapping("/{id}/enable")
    public Result<Void> enable(@PathVariable Long id) {
        alipayConfigService.enable(id);
        return Result.success("已启用");
    }

    @Operation(summary = "切换配置状态")
    @PutMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        alipayConfigService.updateStatus(id, status);
        return Result.success(status == 1 ? "已启用" : "已停用");
    }

    @Operation(summary = "连通性测试")
    @PostMapping("/{id}/test")
    public Result<Boolean> testConnection(@PathVariable Long id) {
        boolean ok = alipayConfigService.testConnection(id);
        return Result.ok(ok ? "连接成功" : "连接失败", ok);
    }

    @Operation(summary = "上传证书文件")
    @PostMapping("/upload-cert")
    public Result<Map<String, String>> uploadCert(@RequestParam("file") MultipartFile file) {
        String path = alipayConfigService.uploadCertFile(file);
        return Result.ok(Map.of("path", path));
    }
}
