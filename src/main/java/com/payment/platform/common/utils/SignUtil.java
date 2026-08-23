package com.payment.platform.common.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

/**
 * 开放接口签名工具（MD5）
 * <p>
 * 签名规则：将除 sign 外的参数按 key 字典序升序排列，拼接为 k1=v1&k2=v2&...（空字符串值不参与），
 * 末尾追加 "&amp;key=" + api_secret，再对整个字符串做 MD5，输出大写十六进制。
 */
public class SignUtil {

    private SignUtil() {
    }

    /** 计算签名（大写 MD5） */
    public static String sign(Map<String, String> params, String secret) {
        return md5(buildSignStr(params) + "&key=" + secret);
    }

    /** 校验签名（常量时间比较，防时序攻击） */
    public static boolean verify(Map<String, String> params, String secret, String sign) {
        if (sign == null || sign.isEmpty()) {
            return false;
        }
        String expect = sign(params, secret);
        return MessageDigest.isEqual(
                expect.getBytes(StandardCharsets.UTF_8),
                sign.trim().getBytes(StandardCharsets.UTF_8));
    }

    /** 构建待签名串：按 key 升序，排除 sign 与空值 */
    public static String buildSignStr(Map<String, String> params) {
        TreeMap<String, String> sorted = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            String k = e.getKey();
            String v = e.getValue();
            if ("sign".equals(k) || v == null || v.isEmpty()) {
                continue;
            }
            sb.append(k).append('=').append(v).append('&');
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    /** MD5，输出大写十六进制 */
    public static String md5(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02X", b & 0xFF));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("MD5 计算失败", e);
        }
    }
}
