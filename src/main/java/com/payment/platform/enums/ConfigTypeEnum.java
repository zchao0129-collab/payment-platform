package com.payment.platform.enums;

import lombok.Getter;

@Getter
public enum ConfigTypeEnum {
    CERT(1, "证书"),
    KEY(2, "秘钥");

    private final int code;
    private final String desc;

    ConfigTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
