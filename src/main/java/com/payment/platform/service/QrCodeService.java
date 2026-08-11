package com.payment.platform.service;

import com.payment.platform.entity.Qrcode;

public interface QrCodeService {

    /** 为商户生成码牌 */
    Qrcode generate(Long merchantId);

    /** 查询商户码牌 */
    Qrcode getByMerchantId(Long merchantId);

    /** 根据码牌数据解析商户信息（收银台用） */
    Qrcode parseQrcode(String qrcodeData);

    /** 收银台获取商户信息（公开接口） */
    java.util.Map<String, Object> getCashierInfo(String merchantNo);
}
