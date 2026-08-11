package com.payment.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String username;

    private String opType;

    private String opTarget;

    private String opDetail;

    private String ip;

    private String userAgent;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
