package com.payment.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "sms")
public class SmsConfig {

    /** 短信提供商: aliyun / tencent */
    private String provider = "aliyun";

    /** 阿里云短信配置 */
    private Aliyun aliyun = new Aliyun();

    @Data
    public static class Aliyun {
        private String accessKeyId;
        private String accessKeySecret;
        private String signName = "支付商户平台";
        private String templateCode;
    }
}
