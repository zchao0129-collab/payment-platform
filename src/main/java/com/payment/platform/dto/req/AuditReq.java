package com.payment.platform.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AuditReq {

    @NotNull(message = "提现ID不能为空")
    private Long withdrawalId;

    private String rejectReason;
}
