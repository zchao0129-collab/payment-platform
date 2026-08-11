package com.payment.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.platform.entity.Withdrawal;

public interface WithdrawalService {

    /** 商户提现申请 */
    void submitWithdraw(Long merchantId, java.math.BigDecimal amount);

    /** 管理员审核通过 */
    void approve(Long withdrawalId, Long auditUserId);

    /** 管理员审核驳回 */
    void reject(Long withdrawalId, Long auditUserId, String reason);

    /** 查询提现列表 */
    Page<Withdrawal> queryPage(Long merchantId, Integer status, Long page, Long size);
}
