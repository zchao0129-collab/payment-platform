package com.payment.platform.enums;

import lombok.Getter;

@Getter
public enum OrderStatusEnum {
    NEW(1, "新建"),
    PAID(2, "已支付"),
    CALLBACK(3, "已回调"),
    REFUNDED(4, "已退款"),
    EXPIRED(5, "已失效"),
    PAY_FAILED(6, "支付失败");

    private final int code;
    private final String desc;

    OrderStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
