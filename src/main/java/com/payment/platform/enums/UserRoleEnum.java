package com.payment.platform.enums;

import lombok.Getter;

@Getter
public enum UserRoleEnum {
    ADMIN(1, "管理员"),
    MERCHANT(2, "商户用户");

    private final int code;
    private final String desc;

    UserRoleEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
