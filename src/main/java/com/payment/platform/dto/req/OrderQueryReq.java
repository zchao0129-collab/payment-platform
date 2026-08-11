package com.payment.platform.dto.req;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderQueryReq {

    private String orderNo;
    private String productName;
    private BigDecimal orderAmount;
    private Integer orderStatus;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    private Long page = 1L;
    private Long size = 20L;
}
