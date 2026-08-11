package com.payment.platform.controller;

import com.payment.platform.common.Result;
import com.payment.platform.entity.Qrcode;
import com.payment.platform.service.QrCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "码牌", description = "码牌管理")
@RestController
@RequestMapping("/api/qrcode")
@RequiredArgsConstructor
public class QrCodeController {

    private final QrCodeService qrCodeService;

    // ======================== 商户端（需登录） ========================

    @Operation(summary = "生成/获取码牌")
    @GetMapping("/my")
    public Result<Qrcode> myQrcode(@RequestAttribute Long merchantId) {
        Qrcode qrcode = qrCodeService.getByMerchantId(merchantId);
        // 旧码牌数据为裸参数格式，不含 "http"，需自动迁移为完整 URL
        if (qrcode == null || qrcode.getQrcodeData() == null || !qrcode.getQrcodeData().startsWith("http")) {
            qrcode = qrCodeService.generate(merchantId);
        }
        return Result.ok(qrcode);
    }

    @Operation(summary = "重新生成码牌")
    @PostMapping("/regenerate")
    public Result<Qrcode> regenerate(@RequestAttribute Long merchantId) {
        return Result.ok(qrCodeService.generate(merchantId));
    }

    // ======================== 收银台（公开接口） ========================

    @Operation(summary = "收银台获取码牌及商户信息（公开）")
    @GetMapping("/info")
    public Result<Map<String, Object>> qrcodeInfo(@RequestParam String merchantNo) {
        return Result.ok(qrCodeService.getCashierInfo(merchantNo));
    }
}
