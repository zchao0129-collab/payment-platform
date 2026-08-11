package com.payment.platform.dto.req;

import lombok.Data;

@Data
public class MerchantQueryReq {

    private String merchantName;
    private String phone;
    private Long page = 1L;
    private Long size = 20L;
}
