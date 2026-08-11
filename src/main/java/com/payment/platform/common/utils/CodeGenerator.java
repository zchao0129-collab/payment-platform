package com.payment.platform.common.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;

import java.util.Date;

/**
 * 编号生成器：商户号 / 订单号 / 推荐码 / 提现编号等
 */
public class CodeGenerator {

    /** 生成商户号: M + yyyyMMdd + 4位随机数 */
    public static String generateMerchantNo() {
        return "M" + DateUtil.format(new Date(), "yyyyMMdd") + RandomUtil.randomNumbers(4);
    }

    /** 生成订单号: ORD + yyyyMMdd + 5位随机数 */
    public static String generateOrderNo() {
        return "ORD" + DateUtil.format(new Date(), "yyyyMMdd") + RandomUtil.randomNumbers(5);
    }

    /** 生成提现编号: WD + yyyyMMdd + 4位随机数 */
    public static String generateWithdrawalNo() {
        return "WD" + DateUtil.format(new Date(), "yyyyMMdd") + RandomUtil.randomNumbers(4);
    }

    /** 生成佣金编号: COM + yyyyMMdd + 4位随机数 */
    public static String generateCommissionNo() {
        return "COM" + DateUtil.format(new Date(), "yyyyMMdd") + RandomUtil.randomNumbers(4);
    }

    /** 生成码牌编号: QR + yyyyMMdd + 4位随机数 */
    public static String generateQrcodeNo() {
        return "QR" + DateUtil.format(new Date(), "yyyyMMdd") + RandomUtil.randomNumbers(4);
    }

    /** 生成推荐码: 6位随机大写字母+数字 */
    public static String generateReferralCode() {
        return RandomUtil.randomString(RandomUtil.BASE_CHAR.toUpperCase(), 6);
    }
}
