package com.payment.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.platform.common.BusinessException;
import com.payment.platform.entity.CommissionConfig;
import com.payment.platform.mapper.CommConfigMapper;
import com.payment.platform.service.CommConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommConfigServiceImpl implements CommConfigService {

    private final CommConfigMapper commConfigMapper;

    @Override
    public List<CommissionConfig> listAll() {
        return commConfigMapper.selectList(
                new LambdaQueryWrapper<CommissionConfig>()
                        .orderByAsc(CommissionConfig::getSortOrder)
                        .orderByAsc(CommissionConfig::getMinAmount));
    }

    @Override
    public void add(CommissionConfig config) {
        // 校验区间不重叠
        validateNoOverlap(config);
        commConfigMapper.insert(config);
        log.info("返佣区间新增: min={}, max={}, rate={}", config.getMinAmount(), config.getMaxAmount(), config.getCommRate());
    }

    @Override
    public void update(CommissionConfig config) {
        CommissionConfig db = commConfigMapper.selectById(config.getId());
        if (db == null) {
            throw new BusinessException("返佣区间不存在");
        }
        validateNoOverlap(config);
        db.setMinAmount(config.getMinAmount());
        db.setMaxAmount(config.getMaxAmount());
        db.setCommRate(config.getCommRate());
        db.setSortOrder(config.getSortOrder());
        db.setStatus(config.getStatus());
        commConfigMapper.updateById(db);
        log.info("返佣区间更新: id={}", config.getId());
    }

    @Override
    public void delete(Long id) {
        CommissionConfig config = commConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException("返佣区间不存在");
        }
        commConfigMapper.deleteById(id);
        log.info("返佣区间删除: id={}", id);
    }

    @Override
    public BigDecimal getRateByAmount(BigDecimal amount) {
        // 查询启用的，按金额区间匹配
        List<CommissionConfig> configs = commConfigMapper.selectList(
                new LambdaQueryWrapper<CommissionConfig>()
                        .eq(CommissionConfig::getStatus, 1)
                        .orderByAsc(CommissionConfig::getSortOrder));
        // 计算区间最大金额，超出不返佣
        BigDecimal maxAmount = null;
        for (CommissionConfig config : configs) {
            if (config.getMaxAmount() != null) {
                if (maxAmount == null || config.getMaxAmount().compareTo(maxAmount) > 0) {
                    maxAmount = config.getMaxAmount();
                }
            }
        }
        // 订单金额超出所有区间上限，不返佣
        if (maxAmount != null && amount.compareTo(maxAmount) > 0) {
            return BigDecimal.ZERO;
        }
        for (CommissionConfig config : configs) {
            if (amount.compareTo(config.getMinAmount()) >= 0
                    && (config.getMaxAmount() == null
                    || amount.compareTo(config.getMaxAmount()) <= 0)) {
                return config.getCommRate();
            }
        }
        return BigDecimal.ZERO;
    }

    // ---- 私有方法 ----

    private void validateNoOverlap(CommissionConfig config) {
        List<CommissionConfig> existing = commConfigMapper.selectList(
                new LambdaQueryWrapper<CommissionConfig>()
                        .eq(CommissionConfig::getStatus, 1)
                        .ne(config.getId() != null, CommissionConfig::getId, config.getId()));
        for (CommissionConfig e : existing) {
            boolean overlaps = config.getMinAmount().compareTo(e.getMaxAmount() != null ? e.getMaxAmount() : new BigDecimal("99999999")) <= 0
                    && (config.getMaxAmount() == null
                    || config.getMaxAmount().compareTo(e.getMinAmount()) >= 0);
            if (overlaps) {
                throw new BusinessException("返佣区间与已有区间重叠: ["
                        + e.getMinAmount() + ", " + e.getMaxAmount() + "]");
            }
        }
    }
}
