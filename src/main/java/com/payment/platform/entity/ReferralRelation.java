package com.payment.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_referral_relation")
public class ReferralRelation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentMerchantId;

    private Long childMerchantId;

    private String childMerchantNo;

    /** 层级: 1-直接下级 */
    private Integer level;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
