package com.payment.platform.enums;

import lombok.Getter;

@Getter
public enum SmsSceneEnum {
    REGISTER(1, "注册"),
    LOGIN(2, "登录"),
    RESET_PASSWORD(3, "找回密码"),
    CHANGE_PHONE(4, "修改手机号");

    private final int code;
    private final String desc;

    SmsSceneEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
