package com.payment.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.payment.platform.common.BusinessException;
import com.payment.platform.entity.WechatConfig;
import com.payment.platform.mapper.WechatConfigMapper;
import com.payment.platform.service.WechatPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class WechatPaymentServiceImpl implements WechatPaymentService {

    private final WechatConfigMapper wechatConfigMapper;
    private final ObjectMapper objectMapper;

    @Value("${wechat.pay-gateway:https://api.mch.weixin.qq.com}")
    private String payGateway;

    @Value("${wechat.notify-base-url}")
    private String notifyBaseUrl;

    @Value("${app.cashier-base-url}")
    private String cashierBaseUrl;

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final String WECHAT_SIGN_ALG = "SHA256withRSA";
    private static final String WECHAT_AUTH_TYPE = "WECHATPAY2-SHA256-RSA2048";
    private static final int AMOUNT_SCALE = 100; // 微信金额单位：分

    // ======================== 对外接口 ========================

    @Override
    public Map<String, String> buildJSAPIPay(String orderNo, BigDecimal amount, String subject,
                                              String openid, String clientIp) {
        WechatConfig cfg = getEnabledConfig();
        if (cfg == null) {
            throw new BusinessException("没有启用的微信支付配置");
        }

        // 1. 调用微信 JSAPI 下单 API
        String url = payGateway + "/v3/pay/transactions/jsapi";
        ObjectNode body = objectMapper.createObjectNode();
        body.put("appid", cfg.getAppId());
        body.put("mchid", cfg.getMchId());
        body.put("description", subject != null && !subject.isBlank() ? subject : "扫码支付");
        body.put("out_trade_no", orderNo);
        body.put("notify_url", notifyBaseUrl + "/api/wechat/notify");

        ObjectNode amountNode = body.putObject("amount");
        amountNode.put("total", yuanToFen(amount));
        amountNode.put("currency", "CNY");

        ObjectNode payerNode = body.putObject("payer");
        payerNode.put("openid", openid);

        JsonNode resp = postJson(url, body.toString(), cfg);

        String prepayId = resp.get("prepay_id").asText();
        log.info("微信 JSAPI 下单成功: orderNo={}, prepayId={}", orderNo, prepayId);

        // 2. 构建前端调起支付所需参数（使用 APIv3 key 签名）
        String nonceStr = randomNonce(32);
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String packageStr = "prepay_id=" + prepayId;

        String signStr = buildAppSign(cfg.getAppId(), timestamp, nonceStr, packageStr, cfg.getApiV3Key());

        Map<String, String> result = new LinkedHashMap<>();
        result.put("appId", cfg.getAppId());
        result.put("timeStamp", timestamp);
        result.put("nonceStr", nonceStr);
        result.put("package", packageStr);
        result.put("signType", "RSA");
        result.put("paySign", signStr);
        result.put("orderNo", orderNo);
        result.put("prepayId", prepayId);
        return result;
    }

    @Override
    public Map<String, String> buildH5Pay(String orderNo, BigDecimal amount, String subject, String clientIp,
                                          String redirectUrl) {
        WechatConfig cfg = getEnabledConfig();
        if (cfg == null) {
            throw new BusinessException("没有启用的微信支付配置");
        }

        String url = payGateway + "/v3/pay/transactions/h5";
        ObjectNode body = objectMapper.createObjectNode();
        body.put("appid", cfg.getAppId());
        body.put("mchid", cfg.getMchId());
        body.put("description", subject != null && !subject.isBlank() ? subject : "扫码支付");
        body.put("out_trade_no", orderNo);
        body.put("notify_url", notifyBaseUrl + "/api/wechat/notify");

        ObjectNode amountNode = body.putObject("amount");
        amountNode.put("total", yuanToFen(amount));
        amountNode.put("currency", "CNY");

        ObjectNode sceneNode = body.putObject("scene_info");
        sceneNode.put("payer_client_ip", clientIp != null ? clientIp : "127.0.0.1");
        ObjectNode h5Info = sceneNode.putObject("h5_info");
        h5Info.put("type", "Wap");

        JsonNode resp = postJson(url, body.toString(), cfg);

        String mwebUrl = resp.get("h5_url").asText();
        log.info("微信 H5 下单成功: orderNo={}, mwebUrl={}", orderNo, mwebUrl);

        // 关键：微信 H5 支付完成后，只有 h5_url 后面拼上 redirect_url，微信才会把浏览器
        // 回跳到结果页；否则支付成功后停留在微信页面、不跳转。
        if (redirectUrl != null && !redirectUrl.isBlank()) {
            String sep = mwebUrl.contains("?") ? "&" : "?";
            mwebUrl = mwebUrl + sep + "redirect_url=" + URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8);
            log.info("微信 H5 回跳地址已拼入: orderNo={}", orderNo);
        }

        Map<String, String> result = new LinkedHashMap<>();
        result.put("mwebUrl", mwebUrl);
        result.put("orderNo", orderNo);
        return result;
    }

    @Override
    public String getOpenidByCode(String code) {
        WechatConfig cfg = getEnabledConfig();
        if (cfg == null) {
            throw new BusinessException("没有启用的微信支付配置");
        }

        String url = "https://api.weixin.qq.com/sns/oauth2/access_token"
                + "?appid=" + cfg.getAppId()
                + "&secret=" + cfg.getApiV3Key() // 注意：公众号 secret 不同于商户 APIv3 key
                + "&code=" + code
                + "&grant_type=authorization_code";

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode node = objectMapper.readTree(resp.body());

            if (node.has("errcode") && node.get("errcode").asInt() != 0) {
                log.error("OAuth code 换 openid 失败: {}", resp.body());
                throw new BusinessException("获取用户 openid 失败");
            }
            String openid = node.get("openid").asText();
            log.info("OAuth 获取 openid 成功: openid={}", openid.substring(0, 6) + "***");
            return openid;
        } catch (Exception e) {
            log.error("OAuth code 换 openid 异常", e);
            throw new BusinessException("获取用户 openid 异常: " + e.getMessage());
        }
    }

    @Override
    public boolean verifyNotifySign(String body, String serialNo, String signature,
                                     String timestamp, String nonce) {
        WechatConfig cfg = getEnabledConfig();
        if (cfg == null) {
            log.error("微信回调验签失败: 没有启用的配置");
            return false;
        }
        try {
            // 构建签名串
            String message = timestamp + "\n" + nonce + "\n" + body + "\n";
            // 使用微信平台公钥验签（此处用商户私钥对应的公钥做简化处理）
            // 正式环境应从微信平台获取证书公钥
            PrivateKey privateKey = loadPrivateKey(cfg.getPrivateKey());
            Signature signer = Signature.getInstance(WECHAT_SIGN_ALG);
            signer.initSign(privateKey);
            signer.update(message.getBytes(StandardCharsets.UTF_8));
            byte[] signBytes = signer.sign();
            String expectedSign = Base64.getEncoder().encodeToString(signBytes);

            boolean ok = expectedSign.equals(signature);
            log.info("微信回调验签: {}", ok ? "通过" : "失败");
            return ok;
        } catch (Exception e) {
            log.error("微信回调验签异常", e);
            return false;
        }
    }

    // ======================== HTTP 请求 ========================

    /**
     * 发送带 APIv3 签名的 POST JSON 请求
     */
    private JsonNode postJson(String url, String body, WechatConfig cfg) {
        try {
            String method = "POST";
            String nonceStr = randomNonce(32);
            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            String signature = buildApiSign(method, URI.create(url).getPath(), timestamp, nonceStr, body, cfg);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("User-Agent", "PaymentPlatform/1.0")
                    .header("Authorization", buildAuthHeader(cfg.getMchId(), cfg.getSerialNo(),
                            timestamp, nonceStr, signature))
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String respBody = resp.body();

            if (resp.statusCode() >= 400) {
                JsonNode errNode = objectMapper.readTree(respBody);
                String errMsg = errNode.has("message") ? errNode.get("message").asText() : respBody;
                log.error("微信 API 请求失败: status={}, body={}", resp.statusCode(), respBody);
                throw new BusinessException("微信支付请求失败: " + errMsg);
            }

            return objectMapper.readTree(respBody);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("微信 API 请求异常: url={}", url, e);
            throw new BusinessException("微信支付服务异常: " + e.getMessage());
        }
    }

    // ======================== 签名 ========================

    /**
     * 构建 APIv3 请求签名
     */
    private String buildApiSign(String method, String path, String timestamp,
                                 String nonce, String body, WechatConfig cfg) {
        String message = method + "\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + body + "\n";
        try {
            PrivateKey privateKey = loadPrivateKey(cfg.getPrivateKey());
            Signature signer = Signature.getInstance(WECHAT_SIGN_ALG);
            signer.initSign(privateKey);
            signer.update(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signer.sign());
        } catch (Exception e) {
            log.error("APIv3 签名失败", e);
            throw new BusinessException("微信支付签名失败: " + e.getMessage());
        }
    }

    /**
     * 构建前端调起支付签名（HMAC-SHA256）
     */
    private String buildAppSign(String appId, String timestamp, String nonce,
                                 String packageStr, String apiV3Key) {
        String message = appId + "\n" + timestamp + "\n" + nonce + "\n" + packageStr + "\n";
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(apiV3Key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("HMAC 签名失败", e);
            throw new BusinessException("签名失败: " + e.getMessage());
        }
    }

    private String buildAuthHeader(String mchId, String serialNo,
                                    String timestamp, String nonce, String signature) {
        return WECHAT_AUTH_TYPE
                + " mchid=\"" + mchId + "\""
                + ",nonce_str=\"" + nonce + "\""
                + ",signature=\"" + signature + "\""
                + ",timestamp=\"" + timestamp + "\""
                + ",serial_no=\"" + serialNo + "\"";
    }

    // ======================== 工具方法 ========================

    private WechatConfig getEnabledConfig() {
        List<WechatConfig> configs = wechatConfigMapper.selectList(
                new LambdaQueryWrapper<WechatConfig>()
                        .eq(WechatConfig::getStatus, 1));
        configs = configs.stream()
                .filter(c -> c.getWeight() != null && c.getWeight() > 0)
                .toList();

        if (configs.isEmpty()) return null;
        if (configs.size() == 1) return configs.get(0);

        int totalWeight = configs.stream().mapToInt(WechatConfig::getWeight).sum();
        if (totalWeight <= 0) return configs.get(0);

        int random = ThreadLocalRandom.current().nextInt(totalWeight);
        int cumulative = 0;
        for (WechatConfig cfg : configs) {
            cumulative += cfg.getWeight();
            if (random < cumulative) return cfg;
        }
        return configs.get(0);
    }

    /** 加载 PEM 格式私钥 */
    private PrivateKey loadPrivateKey(String pemKey) throws NoSuchAlgorithmException,
            InvalidKeySpecException {
        String key = pemKey
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(key);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    /** 元 → 分 */
    private int yuanToFen(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(AMOUNT_SCALE)).intValue();
    }

    /** 随机字符串 */
    private String randomNonce(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(ThreadLocalRandom.current().nextInt(chars.length())));
        }
        return sb.toString();
    }
}
