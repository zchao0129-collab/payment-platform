package com.payment.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.platform.common.PageResult;
import com.payment.platform.common.Result;
import com.payment.platform.entity.Merchant;
import com.payment.platform.entity.Qrcode;
import com.payment.platform.service.MerchantService;
import com.payment.platform.service.QrCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "商户", description = "商户管理")
@RestController
@RequestMapping("/api/merchant")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;
    private final QrCodeService qrCodeService;

    @Operation(summary = "获取当前商户信息")
    @GetMapping("/profile")
    public Result<Merchant> profile(@RequestAttribute Long merchantId) {
        return Result.ok(merchantService.getById(merchantId));
    }

    @Operation(summary = "修改商户信息")
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody Merchant merchant, @RequestAttribute Long merchantId) {
        merchant.setId(merchantId);
        merchantService.update(merchant);
        return Result.success("修改成功");
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody Map<String, String> params,
                                        @RequestAttribute Long merchantId) {
        merchantService.changePassword(merchantId,
                params.get("oldPassword"), params.get("newPassword"));
        return Result.success("密码修改成功");
    }

    // ========== 管理端 ==========

    @Operation(summary = "[管理端] 商户列表")
    @GetMapping("/admin/list")
    public Result<PageResult<Merchant>> adminList(@RequestParam(required = false) String merchantName,
                                                   @RequestParam(required = false) String phone,
                                                   @RequestParam(defaultValue = "1") Long page,
                                                   @RequestParam(defaultValue = "20") Long size) {
        Page<Merchant> result = merchantService.queryPage(merchantName, phone, page, size);
        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @Operation(summary = "[管理端] 新增商户")
    @PostMapping("/admin/create")
    public Result<Void> adminCreate(@RequestBody Merchant merchant) {
        merchantService.create(merchant);
        return Result.success("商户创建成功");
    }

    @Operation(summary = "[管理端] 停用/启用商户")
    @PutMapping("/admin/{id}/status")
    public Result<Void> adminToggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        merchantService.toggleStatus(id, status);
        return Result.ok();
    }

    @Operation(summary = "[管理端] 修改商户信息")
    @PutMapping("/admin/{id}")
    public Result<Void> adminUpdate(@PathVariable Long id, @RequestBody Merchant merchant) {
        merchant.setId(id);
        merchantService.adminUpdate(merchant);
        return Result.success("修改成功");
    }

    @Operation(summary = "[管理端] 获取商户码牌")
    @GetMapping("/admin/{merchantId}/qrcode")
    public Result<Qrcode> adminQrcode(@PathVariable Long merchantId) {
        Qrcode qrcode = qrCodeService.getByMerchantId(merchantId);
        return Result.ok(qrcode);
    }

    @Operation(summary = "[管理端] 配置商户开放API")
    @PutMapping("/admin/{id}/api-config")
    public Result<Void> adminApiConfig(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        merchantService.updateApiConfig(id,
                params.get("apiEnabled") != null ? Integer.valueOf(params.get("apiEnabled").toString()) : null,
                (String) params.get("notifyUrl"),
                (String) params.get("ipWhitelist"),
                params.get("floatEnabled") != null ? Integer.valueOf(params.get("floatEnabled").toString()) : null);
        return Result.success("配置成功");
    }

    @Operation(summary = "[管理端] 重置商户API密钥")
    @PostMapping("/admin/{id}/api-secret")
    public Result<String> adminResetApiSecret(@PathVariable Long id) {
        return Result.ok("密钥已重置", merchantService.resetApiSecret(id));
    }

    @Operation(summary = "[管理端] 删除商户")
    @DeleteMapping("/admin/{id}")
    public Result<Void> adminDelete(@PathVariable Long id) {
        merchantService.deleteMerchant(id);
        return Result.success("商户删除成功");
    }
}
