package com.payment.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_withdrawal")
public class Withdrawal {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String withdrawalNo;

    private Long merchantId;

    private String merchantNo;

    private BigDecimal amount;

    private String alipayAccount;

    /** 状态: 1-待审核, 2-审核通过(已打款), 3-审核驳回 */
    private Integer status;

    private Long auditUserId;

    private LocalDateTime auditTime;

    private String rejectReason;

    private String paymentProof;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updatedAt;
}
