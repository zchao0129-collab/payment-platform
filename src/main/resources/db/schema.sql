-- ============================================================
-- 支付商户管理平台 — 数据库初始化脚本
-- Database: payment_platform
-- ============================================================

CREATE DATABASE IF NOT EXISTS payment_platform
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;
USE payment_platform;

-- 商户表
CREATE TABLE IF NOT EXISTS t_merchant (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    merchant_no     VARCHAR(32)     NOT NULL    COMMENT '商户号',
    merchant_name   VARCHAR(64)     DEFAULT ''  COMMENT '商户名称',
    phone           VARCHAR(20)     NOT NULL    COMMENT '手机号',
    alipay_account  VARCHAR(64)     DEFAULT ''  COMMENT '支付宝账号',
    real_name       VARCHAR(50)     DEFAULT ''  COMMENT '真实姓名',
    id_card_no      VARCHAR(18)     DEFAULT ''  COMMENT '身份证号码',
    password        VARCHAR(255)    NOT NULL    COMMENT '密码(BCrypt)',
    salt            VARCHAR(32)     DEFAULT ''  COMMENT '盐(保留)',
    referral_code   VARCHAR(16)     NOT NULL    COMMENT '推荐码',
    parent_referral VARCHAR(16)     DEFAULT ''  COMMENT '上级推荐码',
    status          TINYINT         DEFAULT 1   COMMENT '状态: 1-正常, 2-停用',
    login_lock_until DATETIME      NULL        COMMENT '登录锁定至',
    login_fail_count INT            DEFAULT 0   COMMENT '登录失败次数',
    api_secret      VARCHAR(64)     DEFAULT ''  COMMENT 'API签名密钥(HMAC-SHA256)',
    notify_url      VARCHAR(256)    DEFAULT ''  COMMENT '支付回调地址',
    api_enabled     TINYINT         DEFAULT 0   COMMENT '是否开通开放API: 0-未开通, 1-已开通',
    ip_whitelist    VARCHAR(512)    DEFAULT ''  COMMENT '调用IP白名单, 逗号分隔, 空=不限',
    api_secret_updated_at DATETIME  NULL        COMMENT 'API密钥更新时间',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_merchant_no (merchant_no),
    UNIQUE KEY uk_phone (phone),
    UNIQUE KEY uk_referral_code (referral_code)
) ENGINE=InnoDB COMMENT='商户表';

-- 用户表
CREATE TABLE IF NOT EXISTS t_user (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(64)     NOT NULL    COMMENT '用户名',
    phone           VARCHAR(20)     DEFAULT ''  COMMENT '手机号',
    password        VARCHAR(255)    NOT NULL    COMMENT '密码(BCrypt)',
    salt            VARCHAR(32)     DEFAULT ''  COMMENT '盐(保留)',
    role            TINYINT         DEFAULT 2   COMMENT '角色: 1-管理员, 2-商户用户',
    merchant_id     BIGINT          NULL        COMMENT '关联商户ID',
    status          TINYINT         DEFAULT 1   COMMENT '状态: 1-正常, 2-停用',
    login_lock_until DATETIME      NULL        COMMENT '登录锁定至',
    login_fail_count INT            DEFAULT 0   COMMENT '登录失败次数',
    last_login_time DATETIME       NULL        COMMENT '最近登录时间',
    last_login_ip   VARCHAR(64)     DEFAULT ''  COMMENT '最近登录IP',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username),
    KEY idx_merchant_id (merchant_id)
) ENGINE=InnoDB COMMENT='用户表';

-- 用户Token表
CREATE TABLE IF NOT EXISTS t_user_token (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT          NOT NULL    COMMENT '用户ID',
    access_token    VARCHAR(512)    NOT NULL    COMMENT '访问令牌',
    refresh_token   VARCHAR(512)    NOT NULL    COMMENT '刷新令牌',
    access_expire   DATETIME        NOT NULL    COMMENT '访问令牌过期时间',
    refresh_expire  DATETIME        NOT NULL    COMMENT '刷新令牌过期时间',
    login_ip        VARCHAR(64)     DEFAULT ''  COMMENT '登录IP',
    device_info     VARCHAR(256)    DEFAULT ''  COMMENT '设备信息',
    is_logout       TINYINT         DEFAULT 0   COMMENT '是否已登出: 0-否, 1-是',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user_id (user_id),
    KEY idx_access_token (access_token(255)),
    KEY idx_refresh_token (refresh_token(255))
) ENGINE=InnoDB COMMENT='用户Token表';

-- 短信验证码表
CREATE TABLE IF NOT EXISTS t_sms_code (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    phone           VARCHAR(20)     NOT NULL    COMMENT '手机号',
    code            VARCHAR(10)     NOT NULL    COMMENT '验证码',
    scene           TINYINT         NOT NULL    COMMENT '场景: 1-注册, 2-登录, 3-修改密码',
    ip              VARCHAR(64)     DEFAULT ''  COMMENT '请求IP',
    is_used         TINYINT         DEFAULT 0   COMMENT '是否已使用',
    verify_fail     INT             DEFAULT 0   COMMENT '校验失败次数',
    expire_time     DATETIME        NOT NULL    COMMENT '过期时间',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    KEY idx_phone_scene (phone, scene)
) ENGINE=InnoDB COMMENT='短信验证码表';

-- 验证票据表（滑块/图形验证码通过后签发）
CREATE TABLE IF NOT EXISTS t_captcha_ticket (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    ticket          VARCHAR(64)     NOT NULL    COMMENT '票据',
    scene           TINYINT         NOT NULL    COMMENT '场景',
    is_used         TINYINT         DEFAULT 0   COMMENT '是否已使用',
    expire_time     DATETIME        NOT NULL    COMMENT '过期时间',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ticket (ticket)
) ENGINE=InnoDB COMMENT='验证票据表';

-- 订单表
CREATE TABLE IF NOT EXISTS t_order (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    order_no        VARCHAR(32)     NOT NULL    COMMENT '订单号',
    merchant_id     BIGINT          NOT NULL    COMMENT '商户ID',
    merchant_no     VARCHAR(32)     NOT NULL    COMMENT '商户号',
    product_name    VARCHAR(128)    DEFAULT ''  COMMENT '商品名称',
    order_amount    DECIMAL(12,2)   NOT NULL    COMMENT '订单金额',
    order_status    TINYINT         DEFAULT 1   COMMENT '状态: 1-新建, 2-已支付, 3-已回调, 4-已退款, 5-已失效, 6-支付失败',
    pay_channel     VARCHAR(16)     DEFAULT 'ALIPAY' COMMENT '支付通道: ALIPAY, WECHAT',
    trade_type      VARCHAR(16)     DEFAULT 'WAP'  COMMENT '交易类型: WAP-手机网站支付, F2F-当面付(支付宝)',
    alipay_trade_no VARCHAR(64)     DEFAULT ''  COMMENT '支付宝交易号',
    channel_trade_no VARCHAR(64)    DEFAULT ''  COMMENT '通道交易号(微信/支付宝通用)',
    pay_time        DATETIME        NULL        COMMENT '支付时间',
    callback_time   DATETIME        NULL        COMMENT '回调时间',
    refund_time     DATETIME        NULL        COMMENT '退款时间',
    refund_amount   DECIMAL(12,2)   NULL        COMMENT '退款金额',
    expire_time     DATETIME        NULL        COMMENT '过期时间',
    fail_reason     VARCHAR(256)    DEFAULT ''  COMMENT '失败原因',
    qrcode_id       BIGINT          NULL        COMMENT '码牌ID',
    remark          VARCHAR(256)    DEFAULT ''  COMMENT '备注',
    notify_url      VARCHAR(256)    DEFAULT ''  COMMENT '本单回调地址(空则用商户默认)',
    return_url      VARCHAR(256)    DEFAULT ''  COMMENT '支付完成跳转地址',
    notify_status   TINYINT         DEFAULT 0   COMMENT '回调状态: 0-未通知, 1-成功, 2-失败待重试',
    notify_count    INT             DEFAULT 0   COMMENT '回调重试次数',
    notify_time     DATETIME        NULL        COMMENT '最近回调时间',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_merchant_id (merchant_id),
    KEY idx_merchant_no (merchant_no),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='订单表';

-- 支付日志表
CREATE TABLE IF NOT EXISTS t_payment_log (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    order_id        BIGINT          NOT NULL    COMMENT '订单ID',
    order_no        VARCHAR(32)     NOT NULL    COMMENT '订单号',
    request_body    TEXT            NULL        COMMENT '请求原文',
    response_body   TEXT            NULL        COMMENT '响应原文',
    notify_body     TEXT            NULL        COMMENT '回调内容',
    ip              VARCHAR(64)     DEFAULT ''  COMMENT '来源IP',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    KEY idx_order_id (order_id)
) ENGINE=InnoDB COMMENT='支付日志表';

-- 佣金表
CREATE TABLE IF NOT EXISTS t_commission (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    commission_no   VARCHAR(32)     NOT NULL    COMMENT '佣金编号',
    merchant_id     BIGINT          NOT NULL    COMMENT '商户ID',
    order_id        BIGINT          NOT NULL    COMMENT '订单ID',
    order_no        VARCHAR(32)     NOT NULL    COMMENT '订单号',
    order_amount    DECIMAL(12,2)   NOT NULL    COMMENT '订单金额',
    comm_rate       DECIMAL(6,4)    NOT NULL    COMMENT '佣金比例',
    comm_amount     DECIMAL(12,2)   NOT NULL    COMMENT '佣金金额',
    withdraw_status TINYINT         DEFAULT 1   COMMENT '提现状态: 1-未提现, 2-审核中, 3-已打款, 4-已驳回',
    withdrawal_id   BIGINT          NULL        COMMENT '关联提现单ID',
    settle_date     DATE            NULL        COMMENT '结算日期',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_commission_no (commission_no),
    KEY idx_merchant_id (merchant_id),
    KEY idx_withdrawal_id (withdrawal_id)
) ENGINE=InnoDB COMMENT='佣金表';

-- 提现表
CREATE TABLE IF NOT EXISTS t_withdrawal (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    withdrawal_no   VARCHAR(32)     NOT NULL    COMMENT '提现单号',
    merchant_id     BIGINT          NOT NULL    COMMENT '商户ID',
    merchant_no     VARCHAR(32)     NOT NULL    COMMENT '商户号',
    amount          DECIMAL(12,2)   NOT NULL    COMMENT '提现金额',
    alipay_account  VARCHAR(64)     NOT NULL    COMMENT '支付宝账号',
    status          TINYINT         DEFAULT 1   COMMENT '状态: 1-待审核, 2-已打款, 3-已驳回',
    audit_user_id   BIGINT          NULL        COMMENT '审核人ID',
    audit_time      DATETIME        NULL        COMMENT '审核时间',
    reject_reason   VARCHAR(256)    DEFAULT ''  COMMENT '驳回原因',
    payment_proof   VARCHAR(512)    DEFAULT ''  COMMENT '付款凭证',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_withdrawal_no (withdrawal_no),
    KEY idx_merchant_id (merchant_id),
    KEY idx_status (status)
) ENGINE=InnoDB COMMENT='提现表';

-- 码牌表
CREATE TABLE IF NOT EXISTS t_qrcode (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    qrcode_no       VARCHAR(32)     NOT NULL    COMMENT '码牌编号',
    merchant_id     BIGINT          NOT NULL    COMMENT '商户ID',
    merchant_no     VARCHAR(32)     NOT NULL    COMMENT '商户号',
    alipay_config_id BIGINT         NULL        COMMENT '关联支付宝配置ID',
    qrcode_data     VARCHAR(512)    NOT NULL    COMMENT '码牌数据',
    qrcode_image    MEDIUMTEXT      NULL        COMMENT '二维码图片(Base64)',
    status          TINYINT         DEFAULT 1   COMMENT '状态: 1-有效, 2-已停用',
    remark          VARCHAR(256)    DEFAULT ''  COMMENT '备注',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_qrcode_no (qrcode_no),
    KEY idx_merchant_id (merchant_id)
) ENGINE=InnoDB COMMENT='码牌表';

-- 支付宝配置表
CREATE TABLE IF NOT EXISTS t_alipay_config (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    config_name     VARCHAR(64)     NOT NULL    COMMENT '配置名称',
    config_type     TINYINT         DEFAULT 2   COMMENT '配置类型: 1-证书, 2-秘钥',
    app_id          VARCHAR(32)     NOT NULL    COMMENT '支付宝AppId',
    uid             VARCHAR(32)     DEFAULT ''  COMMENT '商户UID(PID)',
    private_key     TEXT            NULL        COMMENT '应用私钥',
    alipay_public_key TEXT         NULL        COMMENT '支付宝公钥',
    app_public_key  TEXT            NULL        COMMENT '应用公钥',
    app_cert_path   VARCHAR(256)    DEFAULT ''  COMMENT '应用证书路径',
    root_cert_path  VARCHAR(256)    DEFAULT ''  COMMENT '根证书路径',
    public_cert_path VARCHAR(256)  DEFAULT ''  COMMENT '支付宝公钥证书路径',
    status          TINYINT         DEFAULT 2   COMMENT '状态: 1-启用, 2-停用',
    weight          INT             DEFAULT 100 COMMENT '权重: 流量分配比例，0=不使用',
    last_test_time  DATETIME        NULL        COMMENT '最后测试时间',
    last_test_result TINYINT        NULL        COMMENT '最后测试结果: 1-成功, 2-失败',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='支付宝配置表';

-- 微信支付配置表
CREATE TABLE IF NOT EXISTS t_wechat_config (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    config_name     VARCHAR(64)     NOT NULL    COMMENT '配置名称',
    app_id          VARCHAR(32)     NOT NULL    COMMENT '微信公众号/小程序AppId',
    mch_id          VARCHAR(32)     NOT NULL    COMMENT '商户号',
    api_v3_key      VARCHAR(64)     NOT NULL    COMMENT 'APIv3密钥',
    serial_no       VARCHAR(64)     NOT NULL    COMMENT '商户证书序列号',
    private_key     TEXT            NOT NULL    COMMENT '商户私钥(PEM格式)',
    status          TINYINT         DEFAULT 2   COMMENT '状态: 1-启用, 2-停用',
    weight          INT             DEFAULT 100 COMMENT '权重: 流量分配比例，0=不使用',
    last_test_time  DATETIME        NULL        COMMENT '最后测试时间',
    last_test_result TINYINT        NULL        COMMENT '最后测试结果: 1-成功, 2-失败',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='微信支付配置表';

-- 返佣配置表
CREATE TABLE IF NOT EXISTS t_commission_config (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    min_amount      DECIMAL(12,2)   NOT NULL    COMMENT '区间最小值(含)',
    max_amount      DECIMAL(12,2)   NULL        COMMENT '区间最大值(含), NULL=无上限',
    comm_rate       DECIMAL(6,4)    NOT NULL    COMMENT '返佣比例',
    sort_order      INT             DEFAULT 0   COMMENT '排序',
    status          TINYINT         DEFAULT 1   COMMENT '状态: 1-启用, 2-停用',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='返佣配置表';

-- 推荐关系表
CREATE TABLE IF NOT EXISTS t_referral_relation (
    id                  BIGINT      AUTO_INCREMENT PRIMARY KEY,
    parent_merchant_id  BIGINT      NOT NULL    COMMENT '上级商户ID',
    child_merchant_id   BIGINT      NOT NULL    COMMENT '下级商户ID',
    child_merchant_no   VARCHAR(32) NOT NULL    COMMENT '下级商户号',
    level               TINYINT     DEFAULT 1   COMMENT '层级',
    created_at          DATETIME    DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_child (child_merchant_id),
    KEY idx_parent (parent_merchant_id)
) ENGINE=InnoDB COMMENT='推荐关系表';

-- 操作日志表
CREATE TABLE IF NOT EXISTS t_operation_log (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT          NULL        COMMENT '操作人ID',
    username        VARCHAR(64)     DEFAULT ''  COMMENT '操作人用户名',
    module          VARCHAR(32)     NOT NULL    COMMENT '操作模块',
    action          VARCHAR(32)     NOT NULL    COMMENT '操作类型',
    target_id       BIGINT          NULL        COMMENT '目标ID',
    detail          TEXT            NULL        COMMENT '操作详情',
    ip              VARCHAR(64)     DEFAULT ''  COMMENT 'IP地址',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user_id (user_id),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='操作日志表';

-- 插入默认管理员 (密码: admin123, BCrypt加密)
INSERT INTO t_user (username, phone, password, role, status) VALUES
('admin', '13800000000', '$2a$10$7FrlaRYmiSV4zOWCrboX2.84AH1/IpvtLc9XcI85G6zL0H0cqM..W', 1, 1);

-- 插入默认返佣配置
INSERT INTO t_commission_config (min_amount, max_amount, comm_rate, sort_order, status) VALUES
(0.01, 10.00, 0.0038, 1, 1),
(10.01, 50.00, 0.0050, 2, 1),
(50.01, 200.00, 0.0065, 3, 1),
(200.01, NULL, 0.0080, 4, 1);
