package com.payment.platform.dto.resp;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderResp {

    private Long id;
    private String orderNo;
    private Long merchantId;
    private String merchantNo;
    private String merchantName;
    private String productName;
    private BigDecimal orderAmount;
    private Integer orderStatus;
    private String orderStatusDesc;
    private String alipayTradeNo;
    private LocalDateTime payTime;
    private LocalDateTime callbackTime;
    private LocalDateTime createdAt;
}
