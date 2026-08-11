package com.payment.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_commission_config")
public class CommissionConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private BigDecimal minAmount;

    private BigDecimal maxAmount;

    private BigDecimal commRate;

    private Integer sortOrder;

    /** 状态: 1-启用, 2-停用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updatedAt;
}
