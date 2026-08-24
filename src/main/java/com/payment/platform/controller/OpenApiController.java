package com.payment.platform.controller;

import com.payment.platform.common.BusinessException;
import com.payment.platform.common.Result;
import com.payment.platform.dto.req.OpenOrderCreateReq;
import com.payment.platform.dto.req.OpenOrderQueryReq;
import com.payment.platform.service.OpenApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

/**
 * 开放API — 供外部商户系统调用（订单创建/查询）
 * <p>
 * 下单/查询需在请求体携带 appId/timestamp/nonce/sign，由 ApiSignFilter 统一验签；
 * 支付链接 /pay/{orderNo} 与跳转链接 /redirect/{orderNo} 由浏览器直接打开，无需验签。
 */
@Slf4j
@Tag(name = "开放API", description = "外部商户调用接口")
@RestController
@RequestMapping("/api/open")
@RequiredArgsConstructor
public class OpenApiController {

    private final OpenApiService openApiService;

    @Operation(summary = "创建订单（返回支付链接）")
    @PostMapping("/order/create")
    public Result<Map<String, Object>> createOrder(@RequestBody OpenOrderCreateReq req) {
        log.info("[开放API] 创建订单入参: {}", req);
        Map<String, Object> data = openApiService.createOrder(req);
        log.info("[开放API] 创建订单出参: {}", data);
        return Result.ok(data);
    }

    @Operation(summary = "查询订单")
    @PostMapping("/order/query")
    public Result<Map<String, Object>> queryOrder(@RequestBody OpenOrderQueryReq req) {
        log.info("[开放API] 查询订单入参: {}", req);
        Map<String, Object> data = openApiService.queryOrder(req);
        log.info("[开放API] 查询订单出参: {}", data);
        return Result.ok(data);
    }

    @Operation(summary = "支付链接（浏览器直接打开）")
    @GetMapping("/pay/{orderNo}")
    public void pay(@PathVariable String orderNo,
                    HttpServletRequest request,
                    HttpServletResponse response) throws IOException {
        try {
            Map<String, String> params = openApiService.pay(orderNo, resolveClientIp(request));
            if (params.containsKey("alipayForm")) {
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().write(params.get("alipayForm"));
            } else if (params.containsKey("mwebUrl")) {
                response.sendRedirect(params.get("mwebUrl"));
            } else if (params.containsKey("payUrl")) {
                // 当面付：兜底展示二维码内容（正常由创建订单接口直接返回 payUrl）
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().write(
                        "<html><body style='font-family:sans-serif;padding:40px;text-align:center'>"
                                + "<h2>请使用支付宝扫码支付</h2>"
                                + "<p style='word-break:break-all;color:#666'>" + params.get("payUrl") + "</p>"
                                + "</body></html>");
            } else {
                writeErrorHtml(response, "支付参数缺失");
            }
        } catch (BusinessException e) {
            writeErrorHtml(response, e.getMessage());
        }
    }

    @Operation(summary = "支付完成跳转（浏览器回调）")
    @GetMapping("/redirect/{orderNo}")
    public void redirect(@PathVariable String orderNo, HttpServletResponse response) throws IOException {
        response.sendRedirect(openApiService.getReturnUrl(orderNo));
    }

    private void writeErrorHtml(HttpServletResponse response, String msg) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write(
                "<html><body style='font-family:sans-serif;padding:40px;text-align:center'>"
                        + "<h2>" + msg + "</h2></body></html>");
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
