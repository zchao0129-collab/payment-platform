package com.payment.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.platform.entity.Commission;

import java.math.BigDecimal;

public interface CommissionService {

    /** 查询商户佣金列表 */
    Page<Commission> queryPage(Long merchantId, Long page, Long size);

    /** 获取商户佣金汇总 */
    BigDecimal getTotalCommission(Long merchantId);

    BigDecimal getWithdrawableAmount(Long merchantId);

    BigDecimal getWithdrawnAmount(Long merchantId);

    BigDecimal getAuditingAmount(Long merchantId);

    /** 每日佣金生成（定时任务） */
    void generateDailyCommissions();

    /** 发起提现 */
    void withdraw(Long merchantId, BigDecimal amount);
}
