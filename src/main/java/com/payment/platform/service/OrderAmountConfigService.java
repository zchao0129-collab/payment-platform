package com.payment.platform.service;

import com.payment.platform.entity.Merchant;
import com.payment.platform.entity.OrderAmountConfig;

import java.math.BigDecimal;

public interface OrderAmountConfigService {

    /** 查询当前配置（单行） */
    OrderAmountConfig get();

    /** 保存配置 */
    void save(OrderAmountConfig config);

    /** 对订单金额应用上下浮动；未启用或金额为空时原样返回 */
    BigDecimal applyFloat(BigDecimal amount);

    /**
     * 判断该订单是否需要浮动。
     * 依据配置的判定主从 judgeMode：
     * - MERCHANT：以商户 floatEnabled 为准；
     * - URL：以 returnUrl/notifyUrl 是否命中配置关键字为准。
     */
    boolean shouldFloat(Merchant merchant, String returnUrl, String notifyUrl);
}
