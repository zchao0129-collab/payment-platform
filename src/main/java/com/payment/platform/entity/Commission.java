package com.payment.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_commission")
public class Commission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String commissionNo;

    private Long merchantId;

    private Long orderId;

    private String orderNo;

    private BigDecimal orderAmount;

    private BigDecimal commRate;

    private BigDecimal commAmount;

    /** 提现状态: 1-未提现, 2-审核中, 3-已打款, 4-已驳回 */
    private Integer withdrawStatus;

    private Long withdrawalId;

    private LocalDate settleDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updatedAt;
}
