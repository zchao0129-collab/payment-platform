package com.payment.platform.service;

import com.payment.platform.dto.resp.StatsResp;

import java.util.List;
import java.util.Map;

public interface StatisticsService {

    /** 营收统计 */
    StatsResp revenueStats(Long merchantId);

    /** 订单排行 TOP10 */
    List<Map<String, Object>> orderRankTop10();

    /** 提现排行 TOP10 */
    List<Map<String, Object>> withdrawRankTop10();
}
