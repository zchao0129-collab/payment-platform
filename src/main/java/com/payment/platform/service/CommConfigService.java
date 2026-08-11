package com.payment.platform.service;

import com.payment.platform.entity.CommissionConfig;

import java.util.List;

public interface CommConfigService {

    /** 查询所有返佣区间 */
    List<CommissionConfig> listAll();

    /** 新增区间 */
    void add(CommissionConfig config);

    /** 修改区间 */
    void update(CommissionConfig config);

    /** 删除区间 */
    void delete(Long id);

    /** 根据金额匹配返佣比例 */
    java.math.BigDecimal getRateByAmount(java.math.BigDecimal amount);
}
