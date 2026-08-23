package com.payment.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.payment.platform.common.BusinessException;
import com.payment.platform.entity.WechatConfig;
import com.payment.platform.mapper.WechatConfigMapper;
import com.payment.platform.service.WechatConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WechatConfigServiceImpl implements WechatConfigService {

    private final WechatConfigMapper wechatConfigMapper;

    @Override
    public List<WechatConfig> listAll() {
        return wechatConfigMapper.selectList(
                new LambdaQueryWrapper<WechatConfig>()
                        .orderByDesc(WechatConfig::getCreatedAt));
    }

    @Override
    public void save(WechatConfig config) {
        if (config.getId() != null) {
            wechatConfigMapper.updateById(config);
            log.info("微信配置更新: id={}", config.getId());
        } else {
            if (config.getStatus() == null) {
                config.setStatus(2);
            }
            if (config.getWeight() == null) {
                config.setWeight(100);
            }
            wechatConfigMapper.insert(config);
            log.info("微信配置新增: id={}, status={}, weight={}", config.getId(), config.getStatus(), config.getWeight());
        }
    }

    @Override
    public void delete(Long id) {
        WechatConfig config = wechatConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException("配置不存在");
        }
        wechatConfigMapper.deleteById(id);
        log.info("微信配置删除: id={}", id);
    }

    @Override
    @Transactional
    public void enable(Long id) {
        WechatConfig config = wechatConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException("配置不存在");
        }
        // 停用所有其他配置
        wechatConfigMapper.update(null,
                new LambdaUpdateWrapper<WechatConfig>()
                        .set(WechatConfig::getStatus, 2));
        // 启用当前配置
        config.setStatus(1);
        wechatConfigMapper.updateById(config);
        log.info("微信配置启用: id={}", id);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        WechatConfig config = wechatConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException("配置不存在");
        }
        if (status == 1) {
            wechatConfigMapper.update(null,
                    new LambdaUpdateWrapper<WechatConfig>()
                            .set(WechatConfig::getStatus, 2));
            config.setStatus(1);
        } else {
            config.setStatus(2);
        }
        wechatConfigMapper.updateById(config);
        log.info("微信配置状态更新: id={}, status={}", id, status);
    }

    @Override
    public boolean testConnection(Long id) {
        WechatConfig config = wechatConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException("配置不存在");
        }
        log.info("微信支付连通性测试: appId={}, mchId={}", config.getAppId(), config.getMchId());
        // TODO: 调用微信支付API验证连通性
        return true;
    }
}
