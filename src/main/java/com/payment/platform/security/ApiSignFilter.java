package com.payment.platform.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.platform.common.BusinessException;
import com.payment.platform.common.utils.SignUtil;
import com.payment.platform.entity.Merchant;
import com.payment.platform.mapper.MerchantMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 开放接口验签过滤器
 * <p>
 * 拦截 /api/open/**，校验 appId/timestamp/nonce/sign：
 * 1. 商户存在且已开通开放 API；
 * 2. timestamp 与服务器时间差 ≤ 5 分钟（防重放窗口）；
 * 3. nonce 未重复使用（Redis 去重）；
 * 4. HMAC-SHA256 验签；
 * 5. 可选 IP 白名单校验。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiSignFilter extends OncePerRequestFilter {

    private static final long TIMESTAMP_WINDOW_MS = 5 * 60 * 1000L;
    private static final String NONCE_KEY_PREFIX = "open:nonce:";

    private final MerchantMapper merchantMapper;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        if (!path.startsWith("/api/open/")) {
            return true;
        }
        // 支付链接 / 跳转链接由浏览器直接打开，无需验签
        return path.startsWith("/api/open/pay/") || path.startsWith("/api/open/redirect/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
        String body = cachedRequest.getBody();
        try {
            JsonNode root = objectMapper.readTree(body);
            String appId = text(root, "appId");
            String timestamp = text(root, "timestamp");
            String nonce = text(root, "nonce");
            String sign = text(root, "sign");
            if (!StringUtils.hasText(appId) || !StringUtils.hasText(timestamp)
                    || !StringUtils.hasText(nonce) || !StringUtils.hasText(sign)) {
                throw new BusinessException(401, "缺少验签参数(appId/timestamp/nonce/sign)");
            }

            Merchant merchant = merchantMapper.selectOne(
                    new LambdaQueryWrapper<Merchant>()
                            .eq(Merchant::getMerchantNo, appId)
                            .last("LIMIT 1"));
            if (merchant == null || merchant.getStatus() == null || merchant.getStatus() != 1) {
                throw new BusinessException(401, "商户不存在或已停用");
            }
            if (merchant.getApiEnabled() == null || merchant.getApiEnabled() != 1) {
                throw new BusinessException(403, "商户未开通开放API");
            }
            if (!StringUtils.hasText(merchant.getApiSecret())) {
                throw new BusinessException(403, "商户未配置API密钥");
            }

            long ts;
            try {
                ts = Long.parseLong(timestamp);
            } catch (NumberFormatException e) {
                throw new BusinessException(401, "timestamp 非法");
            }
            if (Math.abs(System.currentTimeMillis() - ts) > TIMESTAMP_WINDOW_MS) {
                throw new BusinessException(401, "请求已过期");
            }

            // nonce 防重放
            RBucket<String> bucket = redissonClient.getBucket(NONCE_KEY_PREFIX + appId + ":" + nonce);
            if (!bucket.trySet("1", 300, TimeUnit.SECONDS)) {
                throw new BusinessException(401, "重复请求");
            }

            checkIpWhitelist(merchant, request);

            Map<String, String> params = toParamMap(root);
            if (!SignUtil.verify(params, merchant.getApiSecret(), sign)) {
                throw new BusinessException(401, "验签失败");
            }

            request.setAttribute("openAppId", appId);
            filterChain.doFilter(cachedRequest, response);
        } catch (BusinessException e) {
            log.warn("开放接口验签失败: path={}, code={}, msg={}",
                    request.getServletPath(), e.getCode(), e.getMessage());
            writeError(response, e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("开放接口验签异常: path={}", request.getServletPath(), e);
            writeError(response, 401, "验签失败");
        }
    }

    private void checkIpWhitelist(Merchant merchant, HttpServletRequest request) {
        String whitelist = merchant.getIpWhitelist();
        if (!StringUtils.hasText(whitelist)) {
            return;
        }
        String clientIp = resolveClientIp(request);
        for (String ip : whitelist.split(",")) {
            if (ip.trim().equals(clientIp)) {
                return;
            }
        }
        throw new BusinessException(403, "IP 不在白名单内");
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private Map<String, String> toParamMap(JsonNode root) {
        Map<String, String> map = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> it = root.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            String k = e.getKey();
            JsonNode v = e.getValue();
            if ("sign".equals(k) || v == null || v.isNull()) {
                continue;
            }
            map.put(k, v.isTextual() ? v.asText() : v.toString());
        }
        return map;
    }

    private String text(JsonNode root, String key) {
        JsonNode node = root.get(key);
        return node == null || node.isNull() ? null : node.asText();
    }

    private void writeError(HttpServletResponse response, int code, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("code", code);
        err.put("msg", msg);
        err.put("data", null);
        response.getWriter().write(objectMapper.writeValueAsString(err));
    }

    /** 缓存请求体，使 body 可重复读取（验签后 Controller 仍可 @RequestBody） */
    private static class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.body = request.getInputStream().readAllBytes();
        }

        String getBody() {
            return new String(body, StandardCharsets.UTF_8);
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream bais = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return bais.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                }

                @Override
                public int read() {
                    return bais.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
