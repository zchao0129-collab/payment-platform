package com.payment.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.payment.platform.common.BusinessException;
import com.payment.platform.entity.AlipayConfig;
import com.payment.platform.mapper.AlipayConfigMapper;
import com.payment.platform.service.AlipayConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayConfigServiceImpl implements AlipayConfigService {

    private final AlipayConfigMapper alipayConfigMapper;

    /** 证书文件允许的扩展名 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".crt", ".cer", ".pem", ".p12", ".pfx", ".key"
    );

    /** 证书存储子目录 */
    private static final String CERT_SUB_DIR = "certs";

    @Value("${file.upload-path}")
    private String uploadPath;

    @Override
    public List<AlipayConfig> listAll() {
        return alipayConfigMapper.selectList(
                new LambdaQueryWrapper<AlipayConfig>()
                        .orderByDesc(AlipayConfig::getCreatedAt));
    }

    @Override
    public void save(AlipayConfig config) {
        if (config.getId() != null) {
            alipayConfigMapper.updateById(config);
            log.info("支付宝配置更新: id={}", config.getId());
        } else {
            // 新增配置默认停用，需手动启用
            if (config.getStatus() == null) {
                config.setStatus(2);
            }
            if (config.getWeight() == null) {
                config.setWeight(100);
            }
            alipayConfigMapper.insert(config);
            log.info("支付宝配置新增: id={}, status={}, weight={}", config.getId(), config.getStatus(), config.getWeight());
        }
    }

    @Override
    public void delete(Long id) {
        AlipayConfig config = alipayConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException("配置不存在");
        }
        alipayConfigMapper.deleteById(id);
        log.info("支付宝配置删除: id={}", id);
    }

    @Override
    @Transactional
    public void enable(Long id) {
        AlipayConfig config = alipayConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException("配置不存在");
        }
        // 停用所有其他配置
        alipayConfigMapper.update(null,
                new LambdaUpdateWrapper<AlipayConfig>()
                        .set(AlipayConfig::getStatus, 2));
        // 启用当前配置
        config.setStatus(1);
        alipayConfigMapper.updateById(config);
        log.info("支付宝配置启用: id={}", id);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        AlipayConfig config = alipayConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException("配置不存在");
        }
        if (status == 1) {
            // 启用: 先停用所有，再启用当前
            alipayConfigMapper.update(null,
                    new LambdaUpdateWrapper<AlipayConfig>()
                            .set(AlipayConfig::getStatus, 2));
            config.setStatus(1);
        } else {
            config.setStatus(2);
        }
        alipayConfigMapper.updateById(config);
        log.info("支付宝配置状态更新: id={}, status={}", id, status);
    }

    @Override
    public boolean testConnection(Long id) {
        AlipayConfig config = alipayConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException("配置不存在");
        }
        // TODO: 使用IJPay调用支付宝沙箱API验证连通性
        log.info("支付宝连通性测试: appId={}", config.getAppId());
        return true;
    }

    @Override
    public String uploadCertFile(MultipartFile file) {
        // 1. 非空校验
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的证书文件");
        }

        // 2. 扩展名校验
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new BusinessException("文件名不能为空");
        }
        String ext = "";
        int dot = originalName.lastIndexOf('.');
        if (dot >= 0) {
            ext = originalName.substring(dot).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException("不支持的文件格式: " + ext + "，允许: " + String.join(", ", ALLOWED_EXTENSIONS));
        }

        // 3. 大小限制 (最大 2 MB)
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new BusinessException("证书文件不能超过 2 MB");
        }

        // 4. 构建目标路径: {uploadPath}/certs/{yyyy-MM}/{uuid}{ext}
        String dateDir = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String savedName = UUID.randomUUID().toString().replace("-", "") + ext;
        Path targetDir = Paths.get(uploadPath, CERT_SUB_DIR, dateDir);
        Path targetFile = targetDir.resolve(savedName);

        try {
            Files.createDirectories(targetDir);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("证书文件写入失败: {}", targetFile, e);
            throw new BusinessException("文件保存失败，请稍后重试");
        }

        // 5. 返回相对路径（相对于 uploadPath）
        String relativePath = CERT_SUB_DIR + "/" + dateDir + "/" + savedName;
        log.info("证书上传成功: original={}, saved={}, size={}B", originalName, relativePath, file.getSize());
        return relativePath;
    }
}
