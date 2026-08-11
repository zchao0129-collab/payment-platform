package com.payment.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_payment_log")
public class PaymentLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private String orderNo;

    private String alipayTradeNo;

    private String notifyContent;

    /** 处理状态: 1-成功, 2-验签失败, 3-业务处理失败 */
    private Integer notifyStatus;

    private String errorMsg;

    private Integer processTimeMs;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
