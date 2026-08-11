package com.payment.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_qrcode")
public class Qrcode {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String qrcodeNo;

    private Long merchantId;

    private String merchantNo;

    private Long alipayConfigId;

    private String qrcodeData;

    private String qrcodeImage;

    /** 状态: 1-有效, 2-已停用 */
    private Integer status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updatedAt;
}
