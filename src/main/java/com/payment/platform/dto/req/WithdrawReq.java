package com.payment.platform.dto.req;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WithdrawReq {

    @NotNull(message = "提现金额不能为空")
    @DecimalMin(value = "0.01", message = "提现金额至少0.01元")
    private BigDecimal amount;
}
