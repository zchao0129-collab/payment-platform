package com.payment.platform.service;

import com.payment.platform.entity.WechatConfig;

import java.util.List;

public interface WechatConfigService {

    /** 查询所有配置 */
    List<WechatConfig> listAll();

    /** 保存配置 */
    void save(WechatConfig config);

    /** 删除配置 */
    void delete(Long id);

    /** 启用配置（自动停用其他） */
    void enable(Long id);

    /** 更新配置状态 */
    void updateStatus(Long id, Integer status);

    /** 连通性测试 */
    boolean testConnection(Long id);
}
