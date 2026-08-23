package com.payment.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.platform.entity.Order;
import com.payment.platform.entity.WechatConfig;
import com.payment.platform.enums.OrderStatusEnum;
import com.payment.platform.mapper.OrderMapper;
import com.payment.platform.mapper.PaymentLogMapper;
import com.payment.platform.mapper.WechatConfigMapper;
import com.payment.platform.entity.PaymentLog;
import com.payment.platform.service.NotifyCallbackService;
import com.payment.platform.service.WechatPaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/wechat")
@RequiredArgsConstructor
public class WechatNotifyController {

    private final WechatPaymentService wechatPaymentService;
    private final WechatConfigMapper wechatConfigMapper;
    private final OrderMapper orderMapper;
    private final PaymentLogMapper paymentLogMapper;
    private final ObjectMapper objectMapper;
    private final NotifyCallbackService notifyCallbackService;

    // ======================== OAuth2.0 授权 ========================

    /**
     * 发起微信 OAuth2.0 授权 — 重定向至微信授权页面
     */
    @GetMapping("/oauth/authorize")
    public void authorize(@RequestParam String redirect_uri, HttpServletResponse response) throws IOException {
        WechatConfig cfg = wechatConfigMapper.selectOne(
                new LambdaQueryWrapper<WechatConfig>()
                        .eq(WechatConfig::getStatus, 1)
                        .last("LIMIT 1"));
        if (cfg == null) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":-1,\"msg\":\"微信支付未配置\"}");
            return;
        }
        String encodedUri = URLEncoder.encode(redirect_uri, StandardCharsets.UTF_8);
        String oauthUrl = "https://open.weixin.qq.com/connect/oauth2/authorize"
                + "?appid=" + cfg.getAppId()
                + "&redirect_uri=" + encodedUri
                + "&response_type=code"
                + "&scope=snsapi_base"
                + "#wechat_redirect";
        response.sendRedirect(oauthUrl);
    }

    /**
     * 微信 OAuth2.0 code 换 openid（前端 AJAX 调用）
     */
    @GetMapping("/oauth")
    @ResponseBody
    public Map<String, Object> oauthCallback(@RequestParam String code) {
        String openid = wechatPaymentService.getOpenidByCode(code);
        return Map.of("openid", openid);
    }

    /**
     * 微信支付异步通知
     */
    @PostMapping("/notify")
    public String notify(HttpServletRequest request) {
        try {
            // 读取原始 body
            String body = request.getReader().lines().collect(Collectors.joining("\n"));

            // 提取微信签名头
            String serialNo = request.getHeader("Wechatpay-Serial");
            String signature = request.getHeader("Wechatpay-Signature");
            String timestamp = request.getHeader("Wechatpay-Timestamp");
            String nonce = request.getHeader("Wechatpay-Nonce");

            log.info("收到微信支付回调: serial={}, ts={}", serialNo, timestamp);

            // 验签
            boolean signOk = wechatPaymentService.verifyNotifySign(body, serialNo, signature,
                    timestamp, nonce);

            // 解析通知数据
            JsonNode node = objectMapper.readTree(body);
            String eventType = node.get("event_type").asText();

            if (!signOk) {
                log.error("微信回调验签失败");
                // 记录失败日志
                PaymentLog failLog = new PaymentLog();
                failLog.setNotifyContent(body);
                failLog.setNotifyStatus(2); // 验签失败
                failLog.setErrorMsg("验签失败");
                paymentLogMapper.insert(failLog);
                return errorResp("SIGN_FAIL");
            }

            if (!"TRANSACTION.SUCCESS".equals(eventType)) {
                log.info("非支付成功通知，忽略: eventType={}", eventType);
                return successResp();
            }

            // 获取 resource 并解密
            JsonNode resource = node.get("resource");
            String ciphertext = resource.get("ciphertext").asText();
            String associatedData = resource.has("associated_data") ?
                    resource.get("associated_data").asText() : "";
            String nonceStr = resource.get("nonce").asText();

            // AEAD_AES_256_GCM 解密 ciphertext
            String decrypted = decryptNotify(ciphertext, associatedData, nonceStr);
            JsonNode txNode = objectMapper.readTree(decrypted);

            String orderNo = txNode.get("out_trade_no").asText();
            String tradeNo = txNode.has("transaction_id") ?
                    txNode.get("transaction_id").asText() : "";

            log.info("微信支付成功: orderNo={}, tradeNo={}", orderNo, tradeNo);

            // 更新订单状态
            orderMapper.update(null,
                    new LambdaUpdateWrapper<Order>()
                            .eq(Order::getOrderNo, orderNo)
                            .eq(Order::getOrderStatus, OrderStatusEnum.NEW.getCode())
                            .set(Order::getOrderStatus, OrderStatusEnum.PAID.getCode())
                            .set(Order::getChannelTradeNo, tradeNo)
                            .set(Order::getPayTime, LocalDateTime.now()));

            // 推送商户回调
            notifyCallbackService.notifyPaid(orderNo);

            // 记录成功日志
            PaymentLog logEntry = new PaymentLog();
            logEntry.setOrderNo(orderNo);
            logEntry.setNotifyContent(body);
            logEntry.setNotifyStatus(1); // 成功
            paymentLogMapper.insert(logEntry);

            return successResp();
        } catch (Exception e) {
            log.error("微信回调处理异常", e);
            return errorResp("SYSTEM_ERROR");
        }
    }

    /**
     * AEAD_AES_256_GCM 解密微信回调 resource.ciphertext
     */
    private String decryptNotify(String ciphertext, String associatedData, String nonce) {
        try {
            WechatConfig cfg = wechatConfigMapper.selectOne(
                    new LambdaQueryWrapper<WechatConfig>()
                            .eq(WechatConfig::getStatus, 1)
                            .last("LIMIT 1"));
            if (cfg == null) {
                throw new RuntimeException("微信支付未配置");
            }

            // APIv3 key 作为 AES 密钥
            byte[] keyBytes = cfg.getApiV3Key().getBytes(StandardCharsets.UTF_8);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

            // nonce 截取低 12 字节作为 GCM IV
            byte[] nonceBytes = nonce.getBytes(StandardCharsets.UTF_8);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, nonceBytes, 0, 12);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            // associated_data 作为 AAD
            if (associatedData != null && !associatedData.isEmpty()) {
                cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
            }

            byte[] cipherBytes = Base64.getDecoder().decode(ciphertext);
            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("AEAD_AES_256_GCM 解密失败", e);
            throw new RuntimeException("微信通知解密失败: " + e.getMessage());
        }
    }

    private String successResp() {
        return "{\"code\":\"SUCCESS\",\"message\":\"OK\"}";
    }

    private String errorResp(String code) {
        return "{\"code\":\"" + code + "\",\"message\":\"error\"}";
    }
}
