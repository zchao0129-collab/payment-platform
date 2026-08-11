package com.payment.platform.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResp {

    private Long userId;
    private String username;
    private String phone;
    private Integer role;
    private Long merchantId;
    private String merchantNo;
    private String merchantName;
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
}
