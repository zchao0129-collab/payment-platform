package com.payment.platform.service;

import com.payment.platform.entity.AlipayConfig;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AlipayConfigService {

    /** 查询所有配置 */
    List<AlipayConfig> listAll();

    /** 保存配置 */
    void save(AlipayConfig config);

    /** 删除配置 */
    void delete(Long id);

    /** 启用配置（自动停用其他） */
    void enable(Long id);

    /** 更新配置状态 */
    void updateStatus(Long id, Integer status);

    /** 连通性测试 */
    boolean testConnection(Long id);

    /** 上传证书文件，返回存储路径 */
    String uploadCertFile(MultipartFile file);
}
