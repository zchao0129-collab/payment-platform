package com.payment.platform.enums;

import lombok.Getter;

@Getter
public enum WithdrawStatusEnum {
    UNWITHDRAWN(1, "未提现"),
    AUDITING(2, "审核中"),
    PAID(3, "已打款"),
    REJECTED(4, "已驳回");

    private final int code;
    private final String desc;

    WithdrawStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    // 提现审核状态（t_withdrawal.status）
    public static final int AUDIT_PENDING = 1;
    public static final int AUDIT_PASSED = 2;
    public static final int AUDIT_REJECTED = 3;
}
