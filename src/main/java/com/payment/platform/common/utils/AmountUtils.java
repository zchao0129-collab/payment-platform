package com.payment.platform.common.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金额计算工具类
 */
public final class AmountUtils {

    private AmountUtils() {}

    /** 元转分 */
    public static long yuanToFen(BigDecimal yuan) {
        return yuan.multiply(new BigDecimal("100")).longValue();
    }

    /** 分转元 */
    public static BigDecimal fenToYuan(long fen) {
        return new BigDecimal(fen).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    /** 百分比费率转小数 (e.g. 0.38% → 0.0038) */
    public static BigDecimal rateToDecimal(BigDecimal ratePercent) {
        return ratePercent.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
    }

    /** 计算佣金 */
    public static BigDecimal calcCommission(BigDecimal amount, BigDecimal rate) {
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    /** 金额相等比较 */
    public static boolean eq(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) == 0;
    }

    /** a > b */
    public static boolean gt(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) > 0;
    }

    /** a >= b */
    public static boolean ge(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) >= 0;
    }
}
