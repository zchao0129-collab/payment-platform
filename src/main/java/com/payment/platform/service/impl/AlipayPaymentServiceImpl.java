package com.payment.platform.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.CertAlipayRequest;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeWapPayModel;
import com.alipay.api.domain.AlipayTradePrecreateModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.response.AlipayTradeWapPayResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.platform.common.BusinessException;
import com.payment.platform.entity.AlipayConfig;
import com.payment.platform.mapper.AlipayConfigMapper;
import com.payment.platform.service.AlipayPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayPaymentServiceImpl implements AlipayPaymentService {

    private final AlipayConfigMapper alipayConfigMapper;

    @Value("${alipay.gateway}")
    private String gateway;

    @Value("${alipay.notify-base-url}")
    private String notifyBaseUrl;

    @Value("${alipay.sign-type:RSA2}")
    private String signType;

    @Value("${file.upload-path}")
    private String uploadPath;

    /** 配置类型: 证书 */
    private static final int TYPE_CERT = 1;

    /** 配置类型: 秘钥 */
    private static final int TYPE_KEY = 2;

    /** 数据格式 */
    private static final String FORMAT = "json";

    /** 字符编码 */
    private static final String CHARSET = "UTF-8";

    // ======================== 对外接口 ========================

    @Override
    public Map<String, String> buildWapPay(String orderNo, BigDecimal amount, String subject, String returnUrl) {
        // 1. 获取启用的支付宝配置
        AlipayConfig cfg = getEnabledConfig();
        if (cfg == null) {
            log.warn("没有启用的支付宝配置，无法创建支付订单");
            return Map.of("orderNo", orderNo, "alipayForm", "");
        }

        // 2. 根据配置类型创建对应的支付宝客户端
        AlipayClient alipayClient = createAlipayClient(cfg);

        // 3. 构建 WAP 支付请求
        AlipayTradeWapPayModel model = new AlipayTradeWapPayModel();
        model.setOutTradeNo(orderNo);
        model.setTotalAmount(amount.toString());
        model.setSubject(StringUtils.hasText(subject) ? subject : "扫码支付");
        model.setProductCode("QUICK_WAP_WAY");
        if (StringUtils.hasText(returnUrl)) {
            model.setQuitUrl(returnUrl);
        }

        AlipayTradeWapPayRequest request = new AlipayTradeWapPayRequest();
        request.setBizModel(model);
        request.setNotifyUrl(notifyBaseUrl + "/api/alipay/notify");
        request.setReturnUrl(returnUrl);

        // 4. 调用 pageExecute 生成自动提交的 HTML 表单
        try {
            AlipayTradeWapPayResponse response = alipayClient.pageExecute(request);
            if (!response.isSuccess()) {
                log.error("支付宝 WAP 支付请求失败: code={}, msg={}, subMsg={}",
                        response.getCode(), response.getMsg(), response.getSubMsg());
                throw new BusinessException("创建支付宝支付订单失败: " + response.getMsg());
            }
            String form = response.getBody();
            log.info("支付宝 WAP 支付表单生成成功: orderNo={}, formLength={}",
                    orderNo, form != null ? form.length() : 0);
            return Map.of("orderNo", orderNo, "alipayForm", form != null ? form : "");
        } catch (AlipayApiException e) {
            log.error("调用支付宝 WAP 支付异常: orderNo={}", orderNo, e);
            throw new BusinessException("创建支付宝支付订单失败: " + e.getErrMsg());
        }
    }

    @Override
    public Map<String, String> buildF2FPay(String orderNo, BigDecimal amount, String subject) {
        // 1. 获取启用的支付宝配置
        AlipayConfig cfg = getEnabledConfig();
        if (cfg == null) {
            log.warn("没有启用的支付宝配置，无法创建当面付订单");
            return Map.of("orderNo", orderNo, "qrCode", "");
        }

        // 2. 根据配置类型创建对应的支付宝客户端
        AlipayClient alipayClient = createAlipayClient(cfg);

        // 3. 构建当面付预下单请求
        AlipayTradePrecreateModel model = new AlipayTradePrecreateModel();
        model.setOutTradeNo(orderNo);
        model.setTotalAmount(amount.toString());
        model.setSubject(StringUtils.hasText(subject) ? subject : "扫码支付");

        AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
        request.setBizModel(model);
        // 当面付与 WAP 共用同一异步通知地址
        request.setNotifyUrl(notifyBaseUrl + "/api/alipay/notify");

        // 4. 调用 execute 获取二维码内容（注意：当面付用 execute，不是 pageExecute）
        try {
            AlipayTradePrecreateResponse response = alipayClient.execute(request);
            if (!response.isSuccess()) {
                log.error("支付宝当面付预下单失败: code={}, msg={}, subMsg={}",
                        response.getCode(), response.getMsg(), response.getSubMsg());
                throw new BusinessException("创建当面付订单失败: " + response.getMsg());
            }
            String qrCode = response.getQrCode();
            log.info("支付宝当面付预下单成功: orderNo={}, qrCode={}", orderNo, qrCode);
            return Map.of("orderNo", orderNo, "qrCode", qrCode != null ? qrCode : "");
        } catch (AlipayApiException e) {
            log.error("调用支付宝当面付预下单异常: orderNo={}", orderNo, e);
            throw new BusinessException("创建当面付订单失败: " + e.getErrMsg());
        }
    }

    @Override
    public boolean verifyNotifySign(Map<String, String> params) {
        AlipayConfig cfg = getEnabledConfig();
        if (cfg == null) {
            log.error("验签失败: 没有启用的支付宝配置");
            return false;
        }
        try {
            if (cfg.getConfigType() != null && cfg.getConfigType() == TYPE_CERT) {
                // 证书模式验签
                String alipayCertContent = readCertContent(cfg.getRootCertPath());
                return AlipaySignature.rsaCertCheckV1(params, alipayCertContent, CHARSET, signType);
            } else {
                // 秘钥模式验签
                return AlipaySignature.rsaCheckV1(params, cfg.getAlipayPublicKey(), CHARSET, signType);
            }
        } catch (Exception e) {
            log.error("验签异常: configType={}", cfg.getConfigType(), e);
            return false;
        }
    }

    @Override
    public boolean verifyReturnSign(Map<String, String> params) {
        return verifyNotifySign(params);
    }

    // ======================== 支付宝客户端创建 ========================

    /**
     * 根据配置类型创建对应的 AlipayClient
     *
     * @param cfg 启用的支付宝配置
     * @return 密钥模式或证书模式的客户端
     */
    private AlipayClient createAlipayClient(AlipayConfig cfg) {
        if (cfg.getConfigType() != null && cfg.getConfigType() == TYPE_CERT) {
            return createCertClient(cfg);
        }
        return createKeyClient(cfg);
    }

    /**
     * 创建密钥模式客户端
     */
    private AlipayClient createKeyClient(AlipayConfig cfg) {
        log.info("创建支付宝客户端 [密钥模式]: appId={}", cfg.getAppId());
        return new DefaultAlipayClient(
                gateway,
                cfg.getAppId(),
                cfg.getPrivateKey(),
                FORMAT,
                CHARSET,
                cfg.getAlipayPublicKey(),
                signType
        );
    }

    /**
     * 创建证书模式客户端
     */
    private AlipayClient createCertClient(AlipayConfig cfg) {
        log.info("创建支付宝客户端 [证书模式]: appId={}", cfg.getAppId());

        // 读取证书文件内容
        String appCertContent = readCertContent(cfg.getAppCertPath());
        String alipayPublicCertContent = readCertContent(cfg.getPublicCertPath());
        String rootCertContent = readCertContent(cfg.getRootCertPath());

        CertAlipayRequest certRequest = new CertAlipayRequest();
        certRequest.setServerUrl(gateway);
        certRequest.setAppId(cfg.getAppId());
        certRequest.setPrivateKey(cfg.getPrivateKey());
        certRequest.setFormat(FORMAT);
        certRequest.setCharset(CHARSET);
        certRequest.setSignType(signType);
        // 应用公钥证书
        certRequest.setCertContent(appCertContent);
        // 支付宝公钥证书
        certRequest.setAlipayPublicCertContent(alipayPublicCertContent);
        // 支付宝根证书
        certRequest.setRootCertContent(rootCertContent);

        try {
            return new DefaultAlipayClient(certRequest);
        } catch (AlipayApiException e) {
            log.error("创建证书模式客户端失败: appId={}", cfg.getAppId(), e);
            throw new BusinessException("创建证书模式支付宝客户端失败: " + e.getErrMsg());
        }
    }

    // ======================== 证书文件读取 ========================

    /**
     * 读取证书文件内容
     *
     * @param relativePath 证书相对路径（如 certs/2024-08/abc.crt）
     * @return 证书文件全文
     */
    private String readCertContent(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            throw new BusinessException("证书路径未配置");
        }
        Path certFile = Paths.get(uploadPath, relativePath);
        if (!Files.exists(certFile)) {
            log.error("证书文件不存在: {}", certFile);
            throw new BusinessException("证书文件不存在: " + relativePath);
        }
        try {
            String content = Files.readString(certFile, StandardCharsets.UTF_8);
            log.debug("读取证书成功: path={}, size={}", relativePath, content.length());
            return content;
        } catch (IOException e) {
            log.error("读取证书文件失败: {}", certFile, e);
            throw new BusinessException("读取证书文件失败: " + e.getMessage());
        }
    }

    // ======================== 其他私有方法 ========================

    /**
     * 按权重随机选取当前启用的支付宝配置。
     * 权重越大被选中概率越高；权重 ≤ 0 的配置不会被选中。
     * 示例: A(weight=70) + B(weight=30) → A 有 70% 概率被选, B 有 30%。
     */
    private AlipayConfig getEnabledConfig() {
        List<AlipayConfig> configs = alipayConfigMapper.selectList(
                new LambdaQueryWrapper<AlipayConfig>()
                        .eq(AlipayConfig::getStatus, 1));
        // 过滤掉权重为 0 的
        configs = configs.stream()
                .filter(c -> c.getWeight() != null && c.getWeight() > 0)
                .toList();

        if (configs.isEmpty()) {
            return null;
        }
        if (configs.size() == 1) {
            return configs.get(0);
        }

        // 计算总权重
        int totalWeight = configs.stream().mapToInt(AlipayConfig::getWeight).sum();
        if (totalWeight <= 0) {
            return configs.get(0);
        }

        // 随机选取
        int random = ThreadLocalRandom.current().nextInt(totalWeight);
        int cumulative = 0;
        for (AlipayConfig cfg : configs) {
            cumulative += cfg.getWeight();
            if (random < cumulative) {
                log.info("权重选取配置: configName={}, weight={}/{}, random={}",
                        cfg.getConfigName(), cfg.getWeight(), totalWeight, random);
                return cfg;
            }
        }
        // fallback
        return configs.get(0);
    }
}
