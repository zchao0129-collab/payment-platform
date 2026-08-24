package com.payment.platform.enums;

import lombok.Getter;

/**
 * 订单来源
 */
@Getter
public enum OrderSourceEnum {

    /** 收银台/码牌下单（平台自有渠道） */
    CASHIER("CASHIER", "收银台/码牌"),

    /** 开放API下单（外部商户系统调用） */
    OPEN_API("OPEN_API", "开放API");

    private final String code;
    private final String desc;

    OrderSourceEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /** 是否为开放API订单 */
    public static boolean isOpenApi(String orderSource) {
        return OPEN_API.code.equalsIgnoreCase(orderSource);
    }
}
