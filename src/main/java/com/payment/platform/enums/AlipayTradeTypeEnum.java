package com.payment.platform.enums;

import lombok.Getter;

/**
 * 支付宝交易类型
 */
@Getter
public enum AlipayTradeTypeEnum {

    /** 手机网站支付（alipay.trade.wap.pay） */
    WAP("WAP", "手机网站支付"),

    /** 当面付（alipay.trade.precreate） */
    F2F("F2F", "当面付");

    private final String code;
    private final String desc;

    AlipayTradeTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /** 是否为当面付 */
    public static boolean isF2F(String tradeType) {
        return F2F.code.equalsIgnoreCase(tradeType);
    }
}
