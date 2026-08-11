package com.payment.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.common.BitMatrix;
import com.payment.platform.common.BusinessException;
import com.payment.platform.common.utils.CodeGenerator;
import com.payment.platform.entity.AlipayConfig;
import com.payment.platform.entity.Merchant;
import com.payment.platform.entity.Qrcode;
import com.payment.platform.mapper.AlipayConfigMapper;
import com.payment.platform.mapper.MerchantMapper;
import com.payment.platform.mapper.QrCodeMapper;
import com.payment.platform.service.QrCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class QrCodeServiceImpl implements QrCodeService {

    // === 中文字体（跨平台：内置字体 → 系统字体 → 回退 + 警告） ===
    private static final String CN_FONT = loadChineseFontName();

    /**
     * 加载可用于中文渲染的字体名称。
     * 1) 优先 classpath:/fonts/chinese.ttf（内置开源字体，如阿里巴巴普惠体）
     * 2) 尝试系统已安装的常见 CJK 字体
     * 3) 回退 SansSerif（日志告警）
     */
    private static String loadChineseFontName() {
        // 1. 尝试从 classpath 加载内置字体（.ttf 或 .ttc）
        for (String fontPath : new String[]{"/fonts/chinese.ttf", "/fonts/chinese.ttc"}) {
            try (InputStream is = QrCodeServiceImpl.class.getResourceAsStream(fontPath)) {
                if (is != null) {
                    Font bundled = Font.createFont(Font.TRUETYPE_FONT, is);
                    GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(bundled);
                    String name = bundled.getFontName();
                    log.info("✅ 加载内置中文字体[{}]: {}", fontPath, name);
                    return name;
                }
            } catch (Exception e) {
                log.warn("内置字体加载失败[{}]: {}", fontPath, e.getMessage());
            }
        }

        // 2. 遍历系统已安装字体，寻找能渲染中文的字体
        String[] availableFonts = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();
        for (String candidate : CJK_FONT_NAMES) {
            for (String sysFont : availableFonts) {
                if (sysFont.equalsIgnoreCase(candidate)) {
                    log.info("✅ 使用系统中文字体: {}", sysFont);
                    return sysFont;
                }
            }
        }

        // 3. 回退：记录完整的字体信息帮助排查
        log.error("❌ 未找到任何中文字体！码牌中文将显示为方框/乱码。");
        log.error("   → 方案A: 下载开源字体放到 src/main/resources/fonts/chinese.ttf (或 .ttc) 后重新部署");
        log.error("   → 方案B: 执行 yum install -y google-noto-sans-cjk-sc-fonts && fc-cache -fv");
        log.error("   → 已安装字体列表: {}", String.join(", ", availableFonts));
        return "SansSerif";
    }

    /** 常见中文字体名（按跨平台优先级排列） */
    private static final String[] CJK_FONT_NAMES = {
            // Windows
            "Microsoft YaHei", "SimHei", "SimSun", "FangSong", "KaiTi",
            // macOS
            "PingFang SC", "Heiti SC", "STHeiti", "STSong",
            // Linux (yum install google-noto-sans-cjk-sc-fonts)
            "Noto Sans CJK SC", "Noto Sans SC", "Noto Serif CJK SC",
            // Linux (yum install wqy-microhei-fonts)
            "WenQuanYi Micro Hei", "WenQuanYi Zen Hei",
            // Linux (yum install wqy-zenhei-fonts)
            "WenQuanYi Bitmap Song",
            // 阿里巴巴普惠体（Alibaba PuHuiTi）
            "Alibaba PuHuiTi",
            // 思源黑体（Source Han Sans）
            "Source Han Sans SC", "Source Han Sans CN",
    };

    // === Alipay brand colors ===
    private static final Color ALIPAY_BLUE   = new Color(22, 119, 255);  // #1677ff
    private static final Color ALIPAY_DARK   = new Color(0, 80, 200);    // logo shadow
    private static final Color CARD_BG       = Color.WHITE;
    private static final Color TEXT_TITLE    = new Color(22, 119, 255);
    private static final Color TEXT_MERCHANT = new Color(51, 51, 51);
    private static final Color TEXT_HINT     = new Color(153, 153, 153);
    private static final Color QR_BG         = Color.WHITE;

    // === Layout constants ===
    private static final int CARD_WIDTH      = 600;
    private static final int CARD_HEIGHT     = 780;
    private static final int QR_SIZE         = 400;
    private static final int QR_X            = (CARD_WIDTH - QR_SIZE) / 2;   // 100
    private static final int QR_Y            = 118;
    private static final int LOGO_SIZE       = 72;
    private static final int LOGO_ARC        = 16;
    private static final int LOGO_X          = (CARD_WIDTH - LOGO_SIZE) / 2;
    private static final int LOGO_Y          = QR_Y + (QR_SIZE - LOGO_SIZE) / 2;

    private final QrCodeMapper qrCodeMapper;
    private final MerchantMapper merchantMapper;
    private final AlipayConfigMapper alipayConfigMapper;

    @Value("${app.cashier-base-url}")
    private String cashierBaseUrl;

    @Override
    @Transactional
    public Qrcode generate(Long merchantId) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException("商户不存在");
        }
        // 查询已启用的支付宝配置
        AlipayConfig alipayConfig = alipayConfigMapper.selectOne(
                new LambdaQueryWrapper<AlipayConfig>()
                        .eq(AlipayConfig::getStatus, 1)
                        .last("LIMIT 1"));
        if (alipayConfig == null) {
            throw new BusinessException("暂无可用支付宝配置，请联系管理员");
        }
        // 停用旧码牌
        qrCodeMapper.update(null,
                new LambdaUpdateWrapper<Qrcode>()
                        .eq(Qrcode::getMerchantId, merchantId)
                        .eq(Qrcode::getStatus, 1)
                        .set(Qrcode::getStatus, 2));
        // 生成码牌数据（收银台完整 URL，供支付宝扫码打开）
        String qrcodeData = cashierBaseUrl + "/app/cashier?merchantNo=" + merchant.getMerchantNo()
                + "&alipayConfigId=" + alipayConfig.getId();
        // 生成支付宝风格码牌图片
        String qrcodeImage = generateAlipayStyleQrCode(qrcodeData, merchant.getMerchantName());
        // 保存码牌
        Qrcode qrcode = new Qrcode();
        qrcode.setQrcodeNo(CodeGenerator.generateQrcodeNo());
        qrcode.setMerchantId(merchantId);
        qrcode.setMerchantNo(merchant.getMerchantNo());
        qrcode.setAlipayConfigId(alipayConfig.getId());
        qrcode.setQrcodeData(qrcodeData);
        qrcode.setQrcodeImage(qrcodeImage);
        qrcode.setStatus(1);
        qrCodeMapper.insert(qrcode);
        log.info("码牌生成: qrcodeNo={}, merchantNo={}", qrcode.getQrcodeNo(), merchant.getMerchantNo());
        return qrcode;
    }

    @Override
    public Qrcode getByMerchantId(Long merchantId) {
        return qrCodeMapper.selectOne(
                new LambdaQueryWrapper<Qrcode>()
                        .eq(Qrcode::getMerchantId, merchantId)
                        .eq(Qrcode::getStatus, 1)
                        .orderByDesc(Qrcode::getCreatedAt)
                        .last("LIMIT 1"));
    }

    @Override
    public Qrcode parseQrcode(String qrcodeData) {
        if (qrcodeData == null || qrcodeData.isBlank()) {
            throw new BusinessException("码牌数据为空");
        }
        Qrcode qrcode = qrCodeMapper.selectOne(
                new LambdaQueryWrapper<Qrcode>()
                        .eq(Qrcode::getQrcodeData, qrcodeData)
                        .eq(Qrcode::getStatus, 1)
                        .last("LIMIT 1"));
        if (qrcode == null) {
            throw new BusinessException("无效的码牌");
        }
        return qrcode;
    }

    @Override
    public Map<String, Object> getCashierInfo(String merchantNo) {
        if (merchantNo == null || merchantNo.isBlank()) {
            throw new BusinessException("商户号不能为空");
        }
        // 查询商户
        Merchant merchant = merchantMapper.selectOne(
                new LambdaQueryWrapper<Merchant>()
                        .eq(Merchant::getMerchantNo, merchantNo)
                        .eq(Merchant::getStatus, 1)
                        .last("LIMIT 1"));
        if (merchant == null) {
            throw new BusinessException("商户不存在或已停用");
        }
        // 查询当前启用的支付宝配置
        AlipayConfig alipayConfig = alipayConfigMapper.selectOne(
                new LambdaQueryWrapper<AlipayConfig>()
                        .eq(AlipayConfig::getStatus, 1)
                        .last("LIMIT 1"));
        Map<String, Object> info = new HashMap<>();
        info.put("merchantId", merchant.getId());
        info.put("merchantNo", merchant.getMerchantNo());
        info.put("merchantName", merchant.getMerchantName());
        info.put("alipayConfigId", alipayConfig != null ? alipayConfig.getId() : null);
        return info;
    }

    // ======================== 私有方法 ========================

    /**
     * 生成支付宝商家码风格的复合码牌图片。
     *
     * @param content      QR码编码内容
     * @param merchantName 商户名称（显示在码牌下方）
     * @return data:image/png;base64,... 字符串
     */
    private String generateAlipayStyleQrCode(String content, String merchantName) {
        try {
            // 1. 生成高容错 QR 码矩阵（纠错级别 H = 30%）
            BitMatrix qrMatrix = buildQrMatrix(content);

            // 2. 创建整张卡片画布
            BufferedImage card = new BufferedImage(CARD_WIDTH, CARD_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = card.createGraphics();
            try {
                // 全局抗锯齿
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                // 3. 绘制白色卡片背景（圆角矩形）
                drawCardBackground(g);

                // 4. 绘制标题
                drawTitle(g);

                // 5. 绘制 QR 码方块到卡片
                drawQrMatrix(g, qrMatrix);

                // 6. 绘制中心 Logo
                drawCenterLogo(g);

                // 7. 绘制商户信息
                drawMerchantInfo(g, merchantName);

            } finally {
                g.dispose();
            }

            // 8. 编码为 Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(card, "PNG", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());

        } catch (Exception e) {
            log.error("码牌图片生成失败", e);
            throw new BusinessException("码牌图片生成失败: " + e.getMessage());
        }
    }

    /** 生成 QR 码矩阵（H 级纠错，30% 容错） */
    private BitMatrix buildQrMatrix(String content) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 0);
        return writer.encode(content, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);
    }

    /** 绘制白色圆角卡片背景 */
    private void drawCardBackground(Graphics2D g) {
        g.setColor(CARD_BG);
        g.fill(new RoundRectangle2D.Double(0, 0, CARD_WIDTH, CARD_HEIGHT, 24, 24));
    }

    /** 绘制标题 "支付宝商家收款" */
    private void drawTitle(Graphics2D g) {
        g.setColor(TEXT_TITLE);
        g.setFont(new Font(CN_FONT, Font.BOLD, 24));
        String title = "支付宝商家收款";
        FontMetrics fm = g.getFontMetrics();
        int titleW = fm.stringWidth(title);
        g.drawString(title, (CARD_WIDTH - titleW) / 2, 62);
    }

    /** 将 QR 码矩阵逐像素绘制到卡片上 */
    private void drawQrMatrix(Graphics2D g, BitMatrix matrix) {
        // QR 码白色背景
        int bgPad = 7; // 白色内边距
        g.setColor(QR_BG);
        g.fillRect(QR_X - bgPad, QR_Y - bgPad, QR_SIZE + bgPad * 2, QR_SIZE + bgPad * 2);

        // 逐像素绘制黑色模块
        for (int x = 0; x < QR_SIZE; x++) {
            for (int y = 0; y < QR_SIZE; y++) {
                if (matrix.get(x, y)) {
                    g.setColor(Color.BLACK);
                    g.fillRect(QR_X + x, QR_Y + y, 1, 1);
                }
            }
        }
    }

    /** 绘制中心支付宝 Logo（蓝色圆角方块 + 白色"支"字） */
    private void drawCenterLogo(Graphics2D g) {
        // Logo 白色底边（让 Logo 与 QR 码方块之间有隔离）
        g.setColor(QR_BG);
        int clearance = 4; // 白色隔离带宽度
        g.fillRoundRect(LOGO_X - clearance, LOGO_Y - clearance,
                LOGO_SIZE + clearance * 2, LOGO_SIZE + clearance * 2, LOGO_ARC + clearance, LOGO_ARC + clearance);

        // Logo 蓝色背景
        g.setColor(ALIPAY_BLUE);
        g.fillRoundRect(LOGO_X, LOGO_Y, LOGO_SIZE, LOGO_SIZE, LOGO_ARC, LOGO_ARC);

        // Logo 底部深色阴影条
        g.setColor(ALIPAY_DARK);
        g.fillRoundRect(LOGO_X, LOGO_Y + LOGO_SIZE - 20, LOGO_SIZE, 20, LOGO_ARC, LOGO_ARC);
        g.fillRect(LOGO_X, LOGO_Y + LOGO_SIZE - 20, LOGO_SIZE, 10);

        // 白色 "支" 字
        g.setColor(Color.WHITE);
        g.setFont(new Font(CN_FONT, Font.BOLD, 38));
        FontMetrics fm = g.getFontMetrics();
        String zhi = "支";
        int charW = fm.stringWidth(zhi);
        int charH = fm.getAscent();
        // 文字在 Logo 中偏上
        g.drawString(zhi, LOGO_X + (LOGO_SIZE - charW) / 2, LOGO_Y + (LOGO_SIZE + charH) / 2 - 6);
    }

    /** 绘制商户名称和底部提示 */
    private void drawMerchantInfo(Graphics2D g, String merchantName) {
        int infoY = QR_Y + QR_SIZE + 48;

        // 商户名称
        g.setColor(TEXT_MERCHANT);
        g.setFont(new Font(CN_FONT, Font.PLAIN, 18));
        FontMetrics fm = g.getFontMetrics();
        String label = "商户：" + (merchantName != null ? merchantName : "");
        int labelW = fm.stringWidth(label);
        g.drawString(label, (CARD_WIDTH - labelW) / 2, infoY);

        // 底部提示
        g.setColor(TEXT_HINT);
        g.setFont(new Font(CN_FONT, Font.PLAIN, 13));
        fm = g.getFontMetrics();
        String hint = "码牌永久有效 · 支付宝扫码支付";
        int hintW = fm.stringWidth(hint);
        g.drawString(hint, (CARD_WIDTH - hintW) / 2, infoY + 30);
    }
}
