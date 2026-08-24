package com.payment.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.platform.entity.Merchant;
import com.payment.platform.entity.OrderAmountConfig;
import com.payment.platform.mapper.OrderAmountConfigMapper;
import com.payment.platform.service.OrderAmountConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderAmountConfigServiceImpl implements OrderAmountConfigService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    /** 金额下限，避免下浮后金额过小或为负 */
    private static final BigDecimal AMOUNT_FLOOR = new BigDecimal("0.01");

    /** 判定主从: 以商户为主 */
    private static final String JUDGE_MODE_MERCHANT = "MERCHANT";
    /** 判定主从: 以跳转/回调地址为主 */
    private static final String JUDGE_MODE_URL = "URL";

    /** 浮动方向: 只上浮 */
    private static final String DIRECTION_UP = "UP";
    /** 浮动方向: 只下浮 */
    private static final String DIRECTION_DOWN = "DOWN";

    private final OrderAmountConfigMapper orderAmountConfigMapper;

    @Override
    public OrderAmountConfig get() {
        OrderAmountConfig config = orderAmountConfigMapper.selectById(1L);
        if (config == null) {
            // 未初始化时返回默认值（不落库）
            config = new OrderAmountConfig();
            config.setId(1L);
            config.setEnabled(1);
            config.setMinFloat(new BigDecimal("0.01"));
            config.setMaxFloat(new BigDecimal("0.09"));
            config.setJudgeMode(JUDGE_MODE_MERCHANT);
            config.setFloatUrlKeywords("");
            config.setFloatDirection("BOTH");
        }
        return config;
    }

    @Override
    public void save(OrderAmountConfig config) {
        config.setId(1L);
        if (orderAmountConfigMapper.selectById(1L) == null) {
            orderAmountConfigMapper.insert(config);
        } else {
            orderAmountConfigMapper.updateById(config);
        }
        log.info("订单金额浮动配置已更新: enabled={}, minFloat={}, maxFloat={}, direction={}, judgeMode={}, urlKeywords={}",
                config.getEnabled(), config.getMinFloat(), config.getMaxFloat(),
                config.getFloatDirection(), config.getJudgeMode(), config.getFloatUrlKeywords());
    }

    @Override
    public boolean shouldFloat(Merchant merchant, String returnUrl, String notifyUrl) {
        OrderAmountConfig config = get();
        if (config.getEnabled() == null || config.getEnabled() != 1) {
            return false;
        }
        if (JUDGE_MODE_URL.equalsIgnoreCase(config.getJudgeMode())) {
            return matchUrlKeywords(config.getFloatUrlKeywords(), returnUrl, notifyUrl);
        }
        // 默认以商户为主（judgeMode 为空或 MERCHANT）
        return merchant != null && merchant.getFloatEnabled() != null && merchant.getFloatEnabled() == 1;
    }

    /**
     * returnUrl/notifyUrl 是否命中关键字（域名/关键字白名单），命中任一即视为需要浮动。
     */
    private boolean matchUrlKeywords(String keywords, String returnUrl, String notifyUrl) {
        if (keywords == null || keywords.isBlank()) {
            return false;
        }
        String[] arr = keywords.split("[,，]");
        for (String kw : arr) {
            String k = kw.trim();
            if (k.isEmpty()) {
                continue;
            }
            if ((returnUrl != null && returnUrl.contains(k))
                    || (notifyUrl != null && notifyUrl.contains(k))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public BigDecimal applyFloat(BigDecimal amount) {
        if (amount == null) {
            return amount;
        }
        OrderAmountConfig config = orderAmountConfigMapper.selectOne(
                new LambdaQueryWrapper<OrderAmountConfig>()
                        .eq(OrderAmountConfig::getEnabled, 1)
                        .last("LIMIT 1"));
        if (config == null || config.getMinFloat() == null || config.getMaxFloat() == null) {
            return amount;
        }
        // 转成「分」做随机，避免浮点精度问题
        int minCents = config.getMinFloat().multiply(HUNDRED).setScale(0, RoundingMode.HALF_UP).intValue();
        int maxCents = config.getMaxFloat().multiply(HUNDRED).setScale(0, RoundingMode.HALF_UP).intValue();
        if (minCents < 0 || maxCents < minCents) {
            return amount;
        }
        int deltaCents = ThreadLocalRandom.current().nextInt(minCents, maxCents + 1);
        if (deltaCents == 0) {
            return amount;
        }
        BigDecimal delta = BigDecimal.valueOf(deltaCents, 2);
        boolean up = resolveDirection(config.getFloatDirection());
        BigDecimal result = up ? amount.add(delta) : amount.subtract(delta);
        // 下浮后金额不得低于下限：只下浮模式压到 0.01，其余（含上下随机下浮）改为上浮
        if (result.compareTo(AMOUNT_FLOOR) < 0) {
            result = DIRECTION_DOWN.equalsIgnoreCase(config.getFloatDirection())
                    ? AMOUNT_FLOOR
                    : amount.add(delta);
        }
        return result.setScale(2, RoundingMode.HALF_UP);
    }

    /** 解析浮动方向：UP-只上浮, DOWN-只下浮, 其他(含 BOTH)-上下随机 */
    private boolean resolveDirection(String direction) {
        if (DIRECTION_UP.equalsIgnoreCase(direction)) {
            return true;
        }
        if (DIRECTION_DOWN.equalsIgnoreCase(direction)) {
            return false;
        }
        return ThreadLocalRandom.current().nextBoolean();
    }
}
