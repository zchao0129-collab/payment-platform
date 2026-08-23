-- ============================================================
-- 支付商户管理平台 — 测试数据（每表 10 条，覆盖不同状态/类型）
-- Database: payment_platform
-- 执行前确保已运行 schema.sql 初始化表结构
-- 所有商户/用户登录密码均为: admin123
-- ============================================================

USE payment_platform;

-- 清理旧测试数据（保留 schema.sql 中的默认管理员 admin 和初始返佣配置）
DELETE FROM t_referral_relation WHERE id > 0;
DELETE FROM t_operation_log WHERE id > 0;
DELETE FROM t_payment_log WHERE id > 0;
DELETE FROM t_commission WHERE id > 0;
DELETE FROM t_withdrawal WHERE id > 0;
DELETE FROM t_order WHERE id > 0;
DELETE FROM t_qrcode WHERE id > 0;
DELETE FROM t_captcha_ticket WHERE id > 0;
DELETE FROM t_sms_code WHERE id > 0;
DELETE FROM t_user_token WHERE id > 0;
DELETE FROM t_wechat_config WHERE id > 0;
DELETE FROM t_alipay_config WHERE id > 0;
DELETE FROM t_commission_config WHERE id > 0;
DELETE FROM t_merchant WHERE id > 0;
DELETE FROM t_user WHERE id > 1;                  -- 保留默认管理员 admin

-- 重置自增
ALTER TABLE t_referral_relation AUTO_INCREMENT = 1;
ALTER TABLE t_operation_log AUTO_INCREMENT = 1;
ALTER TABLE t_payment_log AUTO_INCREMENT = 1;
ALTER TABLE t_commission AUTO_INCREMENT = 1;
ALTER TABLE t_withdrawal AUTO_INCREMENT = 1;
ALTER TABLE t_order AUTO_INCREMENT = 1;
ALTER TABLE t_qrcode AUTO_INCREMENT = 1;
ALTER TABLE t_captcha_ticket AUTO_INCREMENT = 1;
ALTER TABLE t_sms_code AUTO_INCREMENT = 1;
ALTER TABLE t_user_token AUTO_INCREMENT = 1;
ALTER TABLE t_wechat_config AUTO_INCREMENT = 1;
ALTER TABLE t_alipay_config AUTO_INCREMENT = 1;
ALTER TABLE t_commission_config AUTO_INCREMENT = 1;
ALTER TABLE t_merchant AUTO_INCREMENT = 1;
ALTER TABLE t_user AUTO_INCREMENT = 2;

-- ============================================================
-- 1. t_merchant — 商户表（10条）
--    status: 1-正常, 2-停用 | api_enabled: 0-未开通, 1-已开通
-- ============================================================
INSERT INTO t_merchant
(merchant_no, merchant_name, phone, alipay_account, real_name, id_card_no, password, salt, referral_code, parent_referral, status, login_lock_until, login_fail_count, api_secret, notify_url, api_enabled, ip_whitelist, api_secret_updated_at, created_at, updated_at) VALUES
('M20240101001', '星辰科技有限公司', '13800010001', 'alipay@xingchen.com',  '张伟', '110101199001011234', '$2a$10$7FrlaRYmiSV4zOWCrboX2.84AH1/IpvtLc9XcI85G6zL0H0cqM..W', '', 'REF001', '',        1, NULL,  0, 'a1b2c3d4e5f60718293a4b5c6d7e8f90', 'https://xingchen.com/api/notify',     1, '1.2.3.4,5.6.7.8',            NOW() - INTERVAL 55 DAY, NOW() - INTERVAL 60 DAY, NOW()),
('M20240215001', '银河数码工作室', '13800010002', 'alipay@yinhe.com',     '李娜', '310104198803152345', '$2a$10$7FrlaRYmiSV4zOWCrboX2.84AH1/IpvtLc9XcI85G6zL0H0cqM..W', '', 'REF002', 'REF001',  1, NULL,  0, 'f0e9d8c7b6a594837261504132231a0b', 'https://yinhe.com/pay/notify',       1, '10.0.0.1',                    NOW() - INTERVAL 40 DAY, NOW() - INTERVAL 45 DAY, NOW()),
('M20240320001', '蓝海电商有限公司', '13800010003', 'alipay@lanhai.com',    '王强', '440305199205203456', '$2a$10$7FrlaRYmiSV4zOWCrboX2.84AH1/IpvtLc9XcI85G6zL0H0cqM..W', '', 'REF003', 'REF002',  2, NOW(), 5, '',                                              '',                                  0, '',                             NULL,                     NOW() - INTERVAL 30 DAY, NOW()),
('M20240401001', '云帆支付服务商', '13800010004', 'alipay@yunfan.com',    '刘洋', '510107198911274567', '$2a$10$7FrlaRYmiSV4zOWCrboX2.84AH1/IpvtLc9XcI85G6zL0H0cqM..W', '', 'REF004', 'REF001',  1, NULL,  0, '12ab34cd56ef78ab90cd12ef34ab56cd', 'https://yunfan.com/api/callback',    1, '',                             NOW() - INTERVAL 18 DAY, NOW() - INTERVAL 20 DAY, NOW()),
('M20240510001', '极光互动娱乐', '13800010005', 'alipay@jiguang.com',   '陈静', '330106199307085678', '$2a$10$7FrlaRYmiSV4zOWCrboX2.84AH1/IpvtLc9XcI85G6zL0H0cqM..W', '', 'REF005', '',        1, NULL,  0, '',                                              '',                                  0, '',                             NULL,                     NOW() - INTERVAL 15 DAY, NOW()),
('M20240601001', '鼎新财务咨询', '13800010006', 'alipay@dingxin.com',   '杨帆', '120103199012196789', '$2a$10$7FrlaRYmiSV4zOWCrboX2.84AH1/IpvtLc9XcI85G6zL0H0cqM..W', '', 'REF006', 'REF005',  1, NULL,  0, '89ef12cd34ab5678cd90ef12ab34cd56', 'https://dingxin.com/notify',         1, '192.168.1.100,192.168.1.101', NOW() - INTERVAL 8 DAY,  NOW() - INTERVAL 10 DAY, NOW()),
('M20240615001', '海川贸易有限公司', '13800010007', 'alipay@haichuan.com',  '赵磊', '210203198604237890', '$2a$10$7FrlaRYmiSV4zOWCrboX2.84AH1/IpvtLc9XcI85G6zL0H0cqM..W', '', 'REF007', '',        2, NOW(), 3, '',                                              '',                                  0, '',                             NULL,                     NOW() - INTERVAL 7 DAY,  NOW()),
('M20240701001', '繁花生活服务平台', '13800010008', 'alipay@fanhua.com',    '孙丽', '350203199505119012', '$2a$10$7FrlaRYmiSV4zOWCrboX2.84AH1/IpvtLc9XcI85G6zL0H0cqM..W', '', 'REF008', 'REF006',  1, NULL,  0, 'ef34cd12ab90ef56cd34ab12ef78cd90', 'https://fanhua.com/api/pay/notify',  1, '172.16.0.10',                  NOW() - INTERVAL 4 DAY,  NOW() - INTERVAL 5 DAY,  NOW()),
('M20240720001', '锐思数据科技', '13800010009', 'alipay@ruisi.com',     '周杰', '610102198708269123', '$2a$10$7FrlaRYmiSV4zOWCrboX2.84AH1/IpvtLc9XcI85G6zL0H0cqM..W', '', 'REF009', 'REF005',  1, NULL,  0, 'cd12ef34ab56cd78ef90ab12cd34ef56', 'https://ruisi.com/api/callback',     1, '',                             NOW() - INTERVAL 2 DAY,  NOW() - INTERVAL 3 DAY,  NOW()),
('M20240801001', '天工智能硬件', '13800010010', 'alipay@tiangong.com',  '吴敏', '500103199301011234', '$2a$10$7FrlaRYmiSV4zOWCrboX2.84AH1/IpvtLc9XcI85G6zL0H0cqM..W', '', 'REF010', 'REF008',  1, NULL,  0, 'ab90ef12cd34ef56ab78cd90ef12cd34', 'https://tiangong.com/pay/notify',    1, '10.10.10.10',                  NOW() - INTERVAL 12 HOUR, NOW() - INTERVAL 1 DAY,  NOW());

-- ============================================================
-- 2. t_user — 商户用户表（10条，不含管理员 admin；username=手机号，关联商户）
--    role: 1-管理员, 2-商户用户 | status: 1-正常, 2-停用
-- ============================================================
INSERT INTO t_user (username, phone, password, salt, role, merchant_id, status, login_lock_until, login_fail_count, last_login_time, last_login_ip, created_at, updated_at) VALUES
('13800010001', '13800010001', '$2a$10$7FrlaRYmiSV4zOWCrboX2.84AH1/IpvtLc9XcI85G6zL0H0cqM..W', '', 2, 1,  1, NULL,  0, NOW() - INTERVAL 1 HOUR,   '192.168.1.101', NOW() - INTERVAL 60 DAY, NOW()),
('13800010002', '13800010002', '$2a$10$7FrlaRYmiSV4zOWCrboX2.84AH1/IpvtLc9XcI85G6zL0H0cqM..W', '', 2, 2,  1, NULL,  0, NOW() - INTERVAL 5 HOUR,   '192.168.1.102', NOW() - INTERVAL 45 DAY, NOW()),
('13800010003', '13800010003', '$2a$10$7FrlaRYmiSV4zOWCrboX2.84AH1/IpvtLc9XcI85G6zL0H0cqM..W', '', 2, 3,  2, NOW(), 5, NOW() - INTERVAL 30 DAY,   '10.0.0.1',      NOW() - INTERVAL 30 DAY, NOW()),
('13800010004', '13800010004', '$2a$10$7FrlaRYmiSV4zOWCrboX2.84AH1/IpvtLc9XcI85G6zL0H0cqM..W', '', 2, 4,  1, NULL,  0, NOW() - INTERVAL 2 HOUR,   '192.168.2.1',   NOW() - INTERVAL 20 DAY, NOW()),
('13800010005', '13800010005', '$2a$10$7FrlaRYmiSV4zOWCrboX2.84AH1/IpvtLc9XcI85G6zL0H0cqM..W', '', 2, 5,  1, NULL,  0, NOW() - INTERVAL 30 MINUTE,   '10.10.0.5',     NOW() - INTERVAL 15 DAY, NOW()),
('13800010006', '13800010006', '$2a$10$7FrlaRYmiSV4zOWCrboX2.84AH1/IpvtLc9XcI85G6zL0H0cqM..W', '', 2, 6,  1, NULL,  0, NOW() - INTERVAL 6 HOUR,   '172.16.0.10',   NOW() - INTERVAL 10 DAY, NOW()),
('13800010007', '13800010007', '$2a$10$7FrlaRYmiSV4zOWCrboX2.84AH1/IpvtLc9XcI85G6zL0H0cqM..W', '', 2, 7,  2, NOW(), 3, NOW() - INTERVAL 7 DAY,    '10.0.0.99',     NOW() - INTERVAL 7 DAY,  NOW()),
('13800010008', '13800010008', '$2a$10$7FrlaRYmiSV4zOWCrboX2.84AH1/IpvtLc9XcI85G6zL0H0cqM..W', '', 2, 8,  1, NULL,  0, NOW() - INTERVAL 3 HOUR,   '192.168.3.50',  NOW() - INTERVAL 5 DAY,  NOW()),
('13800010009', '13800010009', '$2a$10$7FrlaRYmiSV4zOWCrboX2.84AH1/IpvtLc9XcI85G6zL0H0cqM..W', '', 2, 9,  1, NULL,  0, NOW() - INTERVAL 1 HOUR,   '172.20.0.8',    NOW() - INTERVAL 3 DAY,  NOW()),
('13800010010', '13800010010', '$2a$10$7FrlaRYmiSV4zOWCrboX2.84AH1/IpvtLc9XcI85G6zL0H0cqM..W', '', 2, 10, 1, NULL,  0, NOW() - INTERVAL 20 MINUTE,   '192.168.0.1',   NOW() - INTERVAL 1 DAY,  NOW());

-- ============================================================
-- 3. t_alipay_config — 支付宝配置表（10条）
--    config_type: 1-证书, 2-秘钥 | status: 1-启用, 2-停用
-- ============================================================
INSERT INTO t_alipay_config (config_name, config_type, app_id, uid, private_key, alipay_public_key, app_public_key, app_cert_path, root_cert_path, public_cert_path, status, weight, last_test_time, last_test_result, created_at, updated_at) VALUES
('主证书配置-星辰科技', 1, '2024010100012345', '2088000000000001', NULL, NULL, NULL, '/certs/app_cert_001.crt', '/certs/root_cert_001.crt', '/certs/alipay_public_cert_001.crt', 1, 70,  NOW() - INTERVAL 30 DAY, 1, NOW() - INTERVAL 60 DAY, NOW()),
('秘钥配置-银河数码',   2, '2024021500012345', '2088000000000002', 'MIIEvQIBADANBgkqhki...', 'MIIBIjANBgkqhkiG...', NULL, '', '', '', 1, 30,  NOW() - INTERVAL 20 DAY, 1, NOW() - INTERVAL 45 DAY, NOW()),
('证书配置-蓝海电商',   1, '2024032000012345', '2088000000000003', NULL, NULL, NULL, '/certs/app_cert_003.crt', '/certs/root_cert_003.crt', '/certs/alipay_public_cert_003.crt', 2, 50,  NOW() - INTERVAL 15 DAY, 2, NOW() - INTERVAL 30 DAY, NOW()),
('秘钥配置-云帆支付',   2, '2024040100012345', '2088000000000004', 'MIIEvQIBADANBgkqhki...', 'MIIBIjANBgkqhkiG...', NULL, '', '', '', 1, 100, NOW() - INTERVAL 10 DAY, 1, NOW() - INTERVAL 20 DAY, NOW()),
('证书配置-极光互动',   1, '2024051000012345', '2088000000000005', NULL, NULL, NULL, '/certs/app_cert_005.crt', '/certs/root_cert_005.crt', '/certs/alipay_public_cert_005.crt', 2, 0,   NULL, NULL, NOW() - INTERVAL 15 DAY, NOW()),
('秘钥配置-鼎新财务',   2, '2024060100012345', '2088000000000006', 'MIIEvQIBADANBgkqhki...', 'MIIBIjANBgkqhkiG...', NULL, '', '', '', 1, 80,  NOW() - INTERVAL 5 DAY, 1, NOW() - INTERVAL 10 DAY, NOW()),
('证书配置-海川贸易',   1, '2024061500012345', '2088000000000007', NULL, NULL, NULL, '/certs/app_cert_007.crt', '/certs/root_cert_007.crt', '/certs/private_key_007.pem', 1, 60,  NOW() - INTERVAL 3 DAY, 1, NOW() - INTERVAL 7 DAY, NOW()),
('秘钥配置-繁花生活',   2, '2024070100012345', '2088000000000008', 'MIIEvQIBADANBgkqhki...', 'MIIBIjANBgkqhkiG...', NULL, '', '', '', 2, 0,   NOW() - INTERVAL 2 DAY, 2, NOW() - INTERVAL 5 DAY, NOW()),
('证书配置-锐思数据',   1, '2024072000012345', '2088000000000009', NULL, NULL, NULL, '/certs/app_cert_009.crt', '/certs/root_cert_009.crt', '/certs/private_key_009.pem', 1, 90,  NOW() - INTERVAL 1 DAY, 1, NOW() - INTERVAL 3 DAY, NOW()),
('秘钥配置-天工智能',   2, '2024080100012345', '2088000000000010', 'MIIEvQIBADANBgkqhki...', 'MIIBIjANBgkqhkiG...', NULL, '', '', '', 1, 50,  NULL, NULL, NOW() - INTERVAL 1 DAY, NOW());

-- ============================================================
-- 4. t_qrcode — 码牌表（10条）
--    status: 1-有效, 2-已停用
-- ============================================================
INSERT INTO t_qrcode (qrcode_no, merchant_id, merchant_no, alipay_config_id, qrcode_data, qrcode_image, status, remark, created_at, updated_at) VALUES
('QR202408010001', 1,  'M20240101001', 1,  'https://qr.alipay.com/fkx001test', NULL, 1, '星辰科技-主收款码',     NOW() - INTERVAL 30 DAY, NOW()),
('QR202408010002', 1,  'M20240101001', 1,  'https://qr.alipay.com/fkx002test', NULL, 1, '星辰科技-备用收款码',   NOW() - INTERVAL 28 DAY, NOW()),
('QR202408010003', 2,  'M20240215001', 2,  'https://qr.alipay.com/fkx003test', NULL, 1, '银河数码-主收款码',     NOW() - INTERVAL 25 DAY, NOW()),
('QR202408010004', 3,  'M20240320001', 3,  'https://qr.alipay.com/fkx004test', NULL, 2, '蓝海电商-已停用码牌',   NOW() - INTERVAL 20 DAY, NOW()),
('QR202408010005', 4,  'M20240401001', 4,  'https://qr.alipay.com/fkx005test', NULL, 1, '云帆支付-主收款码',     NOW() - INTERVAL 15 DAY, NOW()),
('QR202408010006', 5,  'M20240510001', 5,  'https://qr.alipay.com/fkx006test', NULL, 1, '极光互动-游戏充值码',   NOW() - INTERVAL 10 DAY, NOW()),
('QR202408010007', 5,  'M20240510001', 5,  'https://qr.alipay.com/fkx007test', NULL, 2, '极光互动-已停用旧码',   NOW() - INTERVAL 8 DAY,  NOW()),
('QR202408010008', 6,  'M20240601001', 6,  'https://qr.alipay.com/fkx008test', NULL, 1, '鼎新财务-服务费收款码', NOW() - INTERVAL 5 DAY,  NOW()),
('QR202408010009', 8,  'M20240701001', 8,  'https://qr.alipay.com/fkx009test', NULL, 1, '繁花生活-平台结算码',   NOW() - INTERVAL 3 DAY,  NOW()),
('QR202408010010', 9,  'M20240720001', 9,  'https://qr.alipay.com/fkx010test', NULL, 1, '锐思数据-API收款码',    NOW() - INTERVAL 1 DAY,  NOW());

-- ============================================================
-- 5. t_order — 订单表（10条，覆盖 6 种状态）
--    order_status: 1-新建, 2-已支付, 3-已回调, 4-已退款, 5-已失效, 6-支付失败
--    pay_channel: ALIPAY, WECHAT | notify_status: 0-未通知, 1-成功, 2-失败待重试
-- ============================================================
INSERT INTO t_order
(order_no, merchant_id, merchant_no, product_name, order_amount, order_status, pay_channel, alipay_trade_no, channel_trade_no, pay_time, callback_time, refund_time, refund_amount, expire_time, fail_reason, qrcode_id, remark, notify_url, return_url, notify_status, notify_count, notify_time, created_at, updated_at) VALUES
('ORD202408010001', 1, 'M20240101001', 'VIP会员年费',          299.00, 1, 'ALIPAY', '',                              '',                                  NULL,                   NULL,                   NULL,        NULL,          NOW() + INTERVAL 2 HOUR,  '',                     1,  '待支付订单',         '',                                 'https://xingchen.com/order/result', 0, 0, NULL,                   NOW() - INTERVAL 10 MINUTE, NOW()),
('ORD202408010002', 2, 'M20240215001', '数码产品-无线耳机',    159.00, 2, 'ALIPAY', '2024080122001412345678900001', '2024080122001412345678900001', NOW() - INTERVAL 2 HOUR,  NULL,                   NULL,        NULL,          NOW() + INTERVAL 1 DAY,    '',                     3,  '已支付待回调',       '',                                 '',                                 0, 0, NULL,                   NOW() - INTERVAL 3 HOUR,  NOW() - INTERVAL 2 HOUR),
('ORD202408010003', 4, 'M20240401001', '云服务器月租',         599.00, 3, 'ALIPAY', '2024080122001412345678900002', '2024080122001412345678900002', NOW() - INTERVAL 5 HOUR,  NOW() - INTERVAL 4 HOUR,  NULL,        NULL,          NOW() + INTERVAL 1 DAY,    '',                     5,  '支付回调已完成',     '',                                 '',                                 1, 1, NOW() - INTERVAL 4 HOUR, NOW() - INTERVAL 6 HOUR,  NOW() - INTERVAL 4 HOUR),
('ORD202408010004', 1, 'M20240101001', '企业认证服务费',       99.00,  4, 'ALIPAY', '2024080122001412345678900003', '2024080122001412345678900003', NOW() - INTERVAL 2 DAY,   NOW() - INTERVAL 2 DAY,   NOW() - INTERVAL 1 DAY, 99.00,        NOW() + INTERVAL 1 DAY,    '客户申请退款',         1,  '全额退款',           'https://xingchen.com/api/notify',   '',                                 1, 1, NOW() - INTERVAL 1 DAY,  NOW() - INTERVAL 3 DAY,    NOW() - INTERVAL 1 DAY),
('ORD202408010005', 5, 'M20240510001', '游戏点卡100元',        100.00, 4, 'WECHAT', '',                              '4200001234202408011234567890', NOW() - INTERVAL 3 DAY,   NOW() - INTERVAL 3 DAY,   NOW() - INTERVAL 2 DAY, 50.00,        NOW() + INTERVAL 1 DAY,    '部分退款',             6,  '部分退款50元',       '',                                 '',                                 1, 1, NOW() - INTERVAL 2 DAY,  NOW() - INTERVAL 4 DAY,    NOW() - INTERVAL 2 DAY),
('ORD202408010006', 3, 'M20240320001', '电商订单-日用百货',    58.50,  5, 'ALIPAY', '',                              '',                                  NULL,                   NULL,                   NULL,        NULL,          NOW() - INTERVAL 1 HOUR,  '订单超时自动失效',     4,  '超时失效',           '',                                 '',                                 0, 0, NULL,                   NOW() - INTERVAL 3 HOUR,  NOW() - INTERVAL 1 HOUR),
('ORD202408010007', 7, 'M20240615001', '企业报税服务',         1200.00,6, 'ALIPAY', '',                              '',                                  NULL,                   NULL,                   NULL,        NULL,          NOW() + INTERVAL 1 DAY,    '支付宝风控拦截',       NULL,'支付被风控拦截',     '',                                 '',                                 0, 0, NULL,                   NOW() - INTERVAL 1 HOUR,  NOW()),
('ORD202408010008', 6, 'M20240601001', '财务软件授权费',       399.00, 3, 'ALIPAY', '2024080122001412345678900005', '2024080122001412345678900005', NOW() - INTERVAL 6 HOUR,  NOW() - INTERVAL 5 HOUR,  NULL,        NULL,          NOW() + INTERVAL 1 DAY,    '',                     8,  '正常完成',           '',                                 '',                                 1, 1, NOW() - INTERVAL 5 HOUR, NOW() - INTERVAL 7 HOUR,  NOW() - INTERVAL 5 HOUR),
('ORD202408010009', 8, 'M20240701001', '生活服务-家政清洁',    200.00, 1, 'WECHAT', '',                              '',                                  NULL,                   NULL,                   NULL,        NULL,          NOW() + INTERVAL 4 HOUR,  '',                     9,  '新建待支付',         '',                                 'https://fanhua.com/pay/result',     0, 0, NULL,                   NOW() - INTERVAL 5 MINUTE,   NOW()),
('ORD202408010010', 9, 'M20240720001', '数据分析报告定制',     888.00, 2, 'ALIPAY', '2024080122001412345678900006', '2024080122001412345678900006', NOW() - INTERVAL 1 HOUR,  NULL,                   NULL,        NULL,          NOW() + INTERVAL 2 DAY,    '',                     10, '已支付待回调',       'https://ruisi.com/api/callback',     '',                                 2, 2, NOW() - INTERVAL 30 MINUTE, NOW() - INTERVAL 2 HOUR,  NOW() - INTERVAL 30 MINUTE);

-- ============================================================
-- 6. t_payment_log — 支付日志表（10条）
-- ============================================================
INSERT INTO t_payment_log (order_id, order_no, request_body, response_body, notify_body, ip, created_at) VALUES
(2,  'ORD202408010002', '{"appId":"M20240215001","timestamp":"1754400000","nonce":"abc123","amount":"159.00","payChannel":"ALIPAY","productName":"数码产品-无线耳机"}', '{"code":200,"msg":"success","data":{"orderNo":"ORD202408010002","payUrl":"https://qr.alipay.com/fkx003test"}}', NULL,                                                                                                    '120.10.1.2',   NOW() - INTERVAL 3 HOUR),
(3,  'ORD202408010003', '{"appId":"M20240401001","timestamp":"1754400000","nonce":"def456","amount":"599.00","payChannel":"ALIPAY","productName":"云服务器月租"}', '{"code":200,"msg":"success","data":{"orderNo":"ORD202408010003","payUrl":"https://qr.alipay.com/fkx005test"}}', '{"orderNo":"ORD202408010003","tradeNo":"2024080122001412345678900002","amount":"599.00","status":"SUCCESS"}', '120.10.4.2',   NOW() - INTERVAL 4 HOUR),
(4,  'ORD202408010004', '{"appId":"M20240101001","timestamp":"1754400000","nonce":"ghi789","amount":"99.00","payChannel":"ALIPAY","productName":"企业认证服务费"}', '{"code":200,"msg":"success","data":{"orderNo":"ORD202408010004","payUrl":"https://qr.alipay.com/fkx001test"}}', '{"orderNo":"ORD202408010004","tradeNo":"2024080122001412345678900003","amount":"99.00","status":"SUCCESS"}',  '120.10.1.2',   NOW() - INTERVAL 2 DAY),
(5,  'ORD202408010005', '{"appId":"M20240510001","timestamp":"1754400000","nonce":"jkl012","amount":"100.00","payChannel":"WECHAT","productName":"游戏点卡100元"}', '{"code":200,"msg":"success","data":{"orderNo":"ORD202408010005","payUrl":"weixin://wxpay/bizpayurl?pr=abc123"}}', '{"orderNo":"ORD202408010005","tradeNo":"4200001234202408011234567890","amount":"100.00","status":"SUCCESS"}', '120.10.5.2',   NOW() - INTERVAL 3 DAY),
(8,  'ORD202408010008', '{"appId":"M20240601001","timestamp":"1754400000","nonce":"mno345","amount":"399.00","payChannel":"ALIPAY","productName":"财务软件授权费"}', '{"code":200,"msg":"success","data":{"orderNo":"ORD202408010008","payUrl":"https://qr.alipay.com/fkx008test"}}', '{"orderNo":"ORD202408010008","tradeNo":"2024080122001412345678900005","amount":"399.00","status":"SUCCESS"}', '120.10.6.2',   NOW() - INTERVAL 6 HOUR),
(10, 'ORD202408010010', '{"appId":"M20240720001","timestamp":"1754400000","nonce":"pqr678","amount":"888.00","payChannel":"ALIPAY","productName":"数据分析报告定制"}', '{"code":200,"msg":"success","data":{"orderNo":"ORD202408010010","payUrl":"https://qr.alipay.com/fkx010test"}}', '{"orderNo":"ORD202408010010","tradeNo":"2024080122001412345678900006","amount":"888.00","status":"SUCCESS"}', '120.10.9.2',   NOW() - INTERVAL 1 HOUR),
(6,  'ORD202408010006', '{"appId":"M20240320001","timestamp":"1754400000","nonce":"stu901","amount":"58.50","payChannel":"ALIPAY","productName":"电商订单-日用百货"}', '{"code":200,"msg":"success","data":{"orderNo":"ORD202408010006","payUrl":"https://qr.alipay.com/fkx004test"}}', NULL,                                                                                                    '120.10.3.2',   NOW() - INTERVAL 3 HOUR),
(7,  'ORD202408010007', '{"appId":"M20240615001","timestamp":"1754400000","nonce":"vwx234","amount":"1200.00","payChannel":"ALIPAY","productName":"企业报税服务"}', '{"code":200,"msg":"success","data":{"orderNo":"ORD202408010007","payUrl":""}}', NULL,                                                                                                    '120.10.7.2',   NOW() - INTERVAL 1 HOUR),
(9,  'ORD202408010009', '{"appId":"M20240701001","timestamp":"1754400000","nonce":"yza567","amount":"200.00","payChannel":"WECHAT","productName":"生活服务-家政清洁"}', '{"code":200,"msg":"success","data":{"orderNo":"ORD202408010009","payUrl":"weixin://wxpay/bizpayurl?pr=def456"}}', NULL,                                                                                                    '120.10.8.2',   NOW() - INTERVAL 5 MINUTE),
(1,  'ORD202408010001', '{"appId":"M20240101001","timestamp":"1754400000","nonce":"bcd890","amount":"299.00","payChannel":"ALIPAY","productName":"VIP会员年费"}', '{"code":200,"msg":"success","data":{"orderNo":"ORD202408010001","payUrl":"https://qr.alipay.com/fkx001test"}}', NULL,                                                                                                    '120.10.1.2',   NOW() - INTERVAL 10 MINUTE);

-- ============================================================
-- 7. t_commission — 佣金表（10条）
--    withdraw_status: 1-未提现, 2-审核中, 3-已打款, 4-已驳回
-- ============================================================
INSERT INTO t_commission (commission_no, merchant_id, order_id, order_no, order_amount, comm_rate, comm_amount, withdraw_status, withdrawal_id, settle_date, created_at, updated_at) VALUES
('COM202408010001', 1, 3,  'ORD202408010003', 599.00,  0.0065, 3.89,  1, NULL, CURDATE(),           NOW() - INTERVAL 4 HOUR,  NOW()),
('COM202408010002', 2, 2,  'ORD202408010002', 159.00,  0.0065, 1.03,  1, NULL, CURDATE(),           NOW() - INTERVAL 2 HOUR,  NOW()),
('COM202408010003', 4, 3,  'ORD202408010003', 599.00,  0.0050, 3.00,  1, NULL, CURDATE(),           NOW() - INTERVAL 4 HOUR,  NOW()),
('COM202408010004', 5, 5,  'ORD202408010005', 100.00,  0.0050, 0.50,  3, 1,    CURDATE() - INTERVAL 1 DAY, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY),
('COM202408010005', 6, 8,  'ORD202408010008', 399.00,  0.0065, 2.59,  2, 2,    CURDATE() - INTERVAL 1 DAY, NOW() - INTERVAL 5 HOUR, NOW()),
('COM202408010006', 1, 4,  'ORD202408010004', 99.00,   0.0050, 0.50,  4, NULL, CURDATE() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY, NOW()),
('COM202408010007', 8, 8,  'ORD202408010008', 399.00,  0.0065, 2.59,  1, NULL, CURDATE() - INTERVAL 1 DAY, NOW() - INTERVAL 5 HOUR, NOW()),
('COM202408010008', 1, 1,  'ORD202408010001', 299.00,  0.0065, 1.94,  1, NULL, CURDATE(),           NOW() - INTERVAL 10 MINUTE, NOW()),
('COM202408010009', 9, 10, 'ORD202408010010', 888.00,  0.0080, 7.10,  1, NULL, CURDATE(),           NOW() - INTERVAL 1 HOUR,  NOW()),
('COM202408010010', 4, 5,  'ORD202408010005', 100.00,  0.0050, 0.50,  3, 1,    CURDATE() - INTERVAL 2 DAY, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 1 DAY);

-- ============================================================
-- 8. t_withdrawal — 提现表（10条）
--    status: 1-待审核, 2-已打款, 3-已驳回
-- ============================================================
INSERT INTO t_withdrawal (withdrawal_no, merchant_id, merchant_no, amount, alipay_account, status, audit_user_id, audit_time, reject_reason, payment_proof, created_at, updated_at) VALUES
('WD202408010001', 1,  'M20240101001', 500.00,  'alipay@xingchen.com',  2, 1, NOW() - INTERVAL 1 DAY,  '',             'PROOF-001-transfer', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY),
('WD202408010002', 2,  'M20240215001', 200.00,  'alipay@yinhe.com',     2, 1, NOW() - INTERVAL 3 DAY,  '',             'PROOF-002-transfer', NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 3 DAY),
('WD202408010003', 4,  'M20240401001', 800.00,  'alipay@yunfan.com',    1, NULL, NULL,                   '',             '',                   NOW() - INTERVAL 1 DAY, NOW()),
('WD202408010004', 5,  'M20240510001', 350.00,  'alipay@jiguang.com',   1, NULL, NULL,                   '',             '',                   NOW() - INTERVAL 6 HOUR, NOW()),
('WD202408010005', 6,  'M20240601001', 1200.00, 'alipay@dingxin.com',   2, 1, NOW() - INTERVAL 2 DAY,  '',             'PROOF-005-transfer', NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 2 DAY),
('WD202408010006', 8,  'M20240701001', 600.00,  'alipay@fanhua.com',    1, NULL, NULL,                   '',             '',                   NOW() - INTERVAL 2 HOUR, NOW()),
('WD202408010007', 9,  'M20240720001', 1500.00, 'alipay@ruisi.com',     3, 1, NOW() - INTERVAL 1 DAY,  '提现金额超过未结算佣金总额', '',             NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY),
('WD202408010008', 1,  'M20240101001', 300.00,  'alipay@xingchen.com',  1, NULL, NULL,                   '',             '',                   NOW() - INTERVAL 3 HOUR, NOW()),
('WD202408010009', 3,  'M20240320001', 100.00,  'alipay@lanhai.com',    3, 1, NOW() - INTERVAL 4 HOUR,  '商户账户已停用，审核驳回', '',             NOW() - INTERVAL 5 HOUR, NOW()),
('WD202408010010', 10, 'M20240801001', 250.00,  'alipay@tiangong.com',  1, NULL, NULL,                   '',             '',                   NOW() - INTERVAL 30 MINUTE, NOW());

-- ============================================================
-- 9. t_referral_relation — 推荐关系表（7条，child_merchant_id 唯一）
--    level: 1-直接下级 | 仅根商户(1/5/7)无上级
-- ============================================================
INSERT INTO t_referral_relation (parent_merchant_id, child_merchant_id, child_merchant_no, level, created_at) VALUES
(1, 2,  'M20240215001', 1, NOW() - INTERVAL 45 DAY),
(1, 4,  'M20240401001', 1, NOW() - INTERVAL 20 DAY),
(2, 3,  'M20240320001', 1, NOW() - INTERVAL 30 DAY),
(5, 6,  'M20240601001', 1, NOW() - INTERVAL 10 DAY),
(5, 9,  'M20240720001', 1, NOW() - INTERVAL 3 DAY),
(6, 8,  'M20240701001', 1, NOW() - INTERVAL 5 DAY),
(8, 10, 'M20240801001', 1, NOW() - INTERVAL 1 DAY);

-- ============================================================
-- 10. t_operation_log — 操作日志表（10条）
--     module/action 覆盖: 登录/提现/配置/订单/用户 等
-- ============================================================
INSERT INTO t_operation_log (user_id, username, module, action, target_id, detail, ip, created_at) VALUES
(1,  'admin',        '登录', 'LOGIN',        NULL, '管理员登录系统',                                    '192.168.0.1',   NOW() - INTERVAL 10 MINUTE),
(2,  '13800010001',  '登录', 'LOGIN',        NULL, '商户用户登录',                                      '192.168.1.101', NOW() - INTERVAL 1 HOUR),
(1,  'admin',        '提现', 'AUDIT_PASS',   1,    '审核通过提现单 WD202408010001，金额 500.00',          '192.168.0.1',   NOW() - INTERVAL 1 DAY),
(1,  'admin',        '提现', 'AUDIT_REJECT', 9,    '审核驳回提现单: 商户账户已停用',                      '192.168.0.1',   NOW() - INTERVAL 4 HOUR),
(1,  'admin',        '提现', 'AUDIT_PASS',   5,    '审核通过提现单 WD202408010005，金额 1200.00',         '192.168.0.1',   NOW() - INTERVAL 2 DAY),
(1,  'admin',        '配置', 'UPDATE',       1,    '更新支付宝配置ID=1: 更新证书路径',                  '192.168.0.1',   NOW() - INTERVAL 10 DAY),
(6,  '13800010005',  '订单', 'CREATE',       5,    '创建游戏点卡订单，金额100.00',                      '192.168.2.5',   NOW() - INTERVAL 4 DAY),
(1,  'admin',        '用户', 'DISABLE',      8,    '停用商户用户 13800010007',                          '192.168.0.1',   NOW() - INTERVAL 7 DAY),
(9,  '13800010008',  '登录', 'LOGOUT',       NULL, '用户主动登出',                                      '192.168.3.50',  NOW() - INTERVAL 3 HOUR),
(4,  '13800010003',  '登录', 'LOGIN_FAIL',   NULL, '登录失败：密码错误（第3次），账户已锁定',            '10.0.0.1',      NOW() - INTERVAL 30 DAY);

-- ============================================================
-- 11. t_sms_code — 短信验证码表（10条）
--     scene: 1-注册, 2-登录, 3-修改密码 | is_used: 0-否, 1-是
-- ============================================================
INSERT INTO t_sms_code (phone, code, scene, ip, is_used, verify_fail, expire_time, created_at) VALUES
('13800010001', '4592', 1, '192.168.1.101', 0, 0, NOW() + INTERVAL 5 MINUTE,  NOW() - INTERVAL 1 MINUTE),
('13800010002', '7831', 2, '192.168.1.102', 1, 0, NOW() + INTERVAL 5 MINUTE,  NOW() - INTERVAL 2 HOUR),
('13800010003', '1267', 2, '10.0.0.1',      0, 2, NOW() + INTERVAL 5 MINUTE,  NOW() - INTERVAL 10 MINUTE),
('13800010004', '5409', 1, '192.168.2.1',   1, 0, NOW() - INTERVAL 1 DAY,   NOW() - INTERVAL 20 DAY),
('13800010005', '9382', 3, '10.10.0.5',     1, 0, NOW() - INTERVAL 1 DAY,   NOW() - INTERVAL 5 DAY),
('13800010006', '2148', 2, '172.16.0.10',   0, 1, NOW() + INTERVAL 5 MINUTE,  NOW() - INTERVAL 5 MINUTE),
('13800010007', '6703', 1, '10.0.0.99',     0, 0, NOW() + INTERVAL 5 MINUTE,  NOW() - INTERVAL 2 MINUTE),
('13800010008', '3056', 3, '192.168.3.50',  1, 0, NOW() - INTERVAL 1 DAY,   NOW() - INTERVAL 3 DAY),
('13800010009', '8514', 2, '172.20.0.8',    0, 0, NOW() + INTERVAL 5 MINUTE,  NOW() - INTERVAL 3 MINUTE),
('13800010010', '4891', 2, '192.168.0.1',   1, 0, NOW() - INTERVAL 1 DAY,   NOW() - INTERVAL 10 DAY);

-- ============================================================
-- 12. t_captcha_ticket — 验证票据表（10条）
--     scene: 1-注册, 2-登录, 3-找回密码, 4-获取验证码 | is_used: 0-否, 1-是
-- ============================================================
INSERT INTO t_captcha_ticket (ticket, scene, is_used, expire_time, created_at) VALUES
('CAPTCHA-TKT-001-A1B2C3D4', 1, 0, NOW() + INTERVAL 5 MINUTE,  NOW() - INTERVAL 1 MINUTE),
('CAPTCHA-TKT-002-E5F6G7H8', 2, 1, NOW() + INTERVAL 5 MINUTE,  NOW() - INTERVAL 2 HOUR),
('CAPTCHA-TKT-003-I9J0K1L2', 2, 0, NOW() + INTERVAL 5 MINUTE,  NOW() - INTERVAL 3 MINUTE),
('CAPTCHA-TKT-004-M3N4O5P6', 1, 1, NOW() - INTERVAL 1 DAY,   NOW() - INTERVAL 20 DAY),
('CAPTCHA-TKT-005-Q7R8S9T0', 4, 0, NOW() + INTERVAL 5 MINUTE,  NOW() - INTERVAL 1 MINUTE),
('CAPTCHA-TKT-006-U1V2W3X4', 3, 0, NOW() + INTERVAL 5 MINUTE,  NOW() - INTERVAL 2 MINUTE),
('CAPTCHA-TKT-007-Y5Z6A7B8', 2, 1, NOW() - INTERVAL 1 DAY,   NOW() - INTERVAL 3 DAY),
('CAPTCHA-TKT-008-C9D0E1F2', 4, 0, NOW() - INTERVAL 1 HOUR,  NOW() - INTERVAL 2 HOUR),
('CAPTCHA-TKT-009-G3H4I5J6', 1, 0, NOW() + INTERVAL 5 MINUTE,  NOW() - INTERVAL 4 MINUTE),
('CAPTCHA-TKT-010-K7L8M9N0', 3, 1, NOW() - INTERVAL 1 DAY,   NOW() - INTERVAL 5 DAY);

-- ============================================================
-- 13. t_user_token — 用户Token表（10条）
--     is_logout: 0-否, 1-是
-- ============================================================
INSERT INTO t_user_token (user_id, access_token, refresh_token, access_expire, refresh_expire, login_ip, device_info, is_logout, created_at) VALUES
(1,  'AT-ADMIN-001-abc123def456ghi789',       'RT-ADMIN-001-rst123uvw456xyz789',       NOW() + INTERVAL 2 HOUR, NOW() + INTERVAL 7 DAY, '192.168.0.1',   'Windows/Chrome',  0, NOW() - INTERVAL 10 MINUTE),
(2,  'AT-MERCHANT01-002-jkl012mno345pqr678',  'RT-MERCHANT01-002-stu901vwx234yza567',  NOW() + INTERVAL 2 HOUR, NOW() + INTERVAL 7 DAY, '192.168.1.101', 'Mac/Safari',      0, NOW() - INTERVAL 1 HOUR),
(3,  'AT-MERCHANT02-003-bcd345efg678hij901',  'RT-MERCHANT02-003-klm234nop567qrs890',  NOW() - INTERVAL 1 HOUR, NOW() + INTERVAL 7 DAY, '192.168.1.102', 'Windows/Edge',    0, NOW() - INTERVAL 5 HOUR),
(4,  'AT-MERCHANT03-004-lmn678opq901rst234',  'RT-MERCHANT03-004-uvw567xyz890abc123',  NOW() - INTERVAL 1 DAY,  NOW() - INTERVAL 1 DAY, '192.168.2.1',   'iPhone/Safari',   1, NOW() - INTERVAL 20 DAY),
(5,  'AT-MERCHANT04-005-def901ghi234jkl567',  'RT-MERCHANT04-005-mno890pqr123stu456',  NOW() + INTERVAL 2 HOUR, NOW() + INTERVAL 7 DAY, '10.10.0.5',     'Android/Chrome',  0, NOW() - INTERVAL 30 MINUTE),
(6,  'AT-MERCHANT05-006-ghi234jkl567mno890',  'RT-MERCHANT05-006-pqr123stu456vwx789',  NOW() - INTERVAL 1 DAY,  NOW() - INTERVAL 2 DAY, '172.16.0.10',   'Windows/Chrome',  1, NOW() - INTERVAL 10 DAY),
(7,  'AT-MERCHANT06-007-jkl567mno890pqr123',  'RT-MERCHANT06-007-stu456vwx789yza012',  NOW() - INTERVAL 1 DAY,  NOW() - INTERVAL 1 DAY, '192.168.3.50',  'Mac/Chrome',      1, NOW() - INTERVAL 3 DAY),
(8,  'AT-MERCHANT07-008-mno890pqr123stu456',  'RT-MERCHANT07-008-vwx789yza012bcd345',  NOW() + INTERVAL 2 HOUR, NOW() + INTERVAL 7 DAY, '172.20.0.8',    'Windows/Firefox', 0, NOW() - INTERVAL 45 MINUTE),
(9,  'AT-MERCHANT08-009-pqr123stu456vwx789',  'RT-MERCHANT08-009-yza012bcd345efg678',  NOW() - INTERVAL 1 DAY,  NOW() - INTERVAL 1 DAY, '192.168.3.50',  'iPhone/Safari',   1, NOW() - INTERVAL 8 HOUR),
(10, 'AT-OPERADMIN-010-stu456vwx789yza012',   'RT-OPERADMIN-010-bcd345efg678hij901',   NOW() + INTERVAL 2 HOUR, NOW() + INTERVAL 7 DAY, '192.168.0.1',   'Windows/Chrome',  0, NOW() - INTERVAL 10 MINUTE);

-- ============================================================
-- 14. t_commission_config — 返佣配置表（10条）
--     status: 1-启用, 2-停用 | 前4条为默认费率区间
-- ============================================================
INSERT INTO t_commission_config (min_amount, max_amount, comm_rate, sort_order, status, created_at, updated_at) VALUES
(0.01,    10.00,    0.0038, 1, 1, NOW() - INTERVAL 120 DAY, NOW()),
(10.01,   50.00,    0.0050, 2, 1, NOW() - INTERVAL 120 DAY, NOW()),
(50.01,   200.00,   0.0065, 3, 1, NOW() - INTERVAL 120 DAY, NOW()),
(200.01,  NULL,     0.0080, 4, 1, NOW() - INTERVAL 120 DAY, NOW()),
(500.01,  1000.00,  0.0090, 5, 1, NOW() - INTERVAL 50 DAY, NOW()),
(1000.01, 3000.00,  0.0100, 6, 1, NOW() - INTERVAL 50 DAY, NOW()),
(3000.01, 5000.00,  0.0120, 7, 1, NOW() - INTERVAL 50 DAY, NOW()),
(5000.01, NULL,     0.0150, 8, 1, NOW() - INTERVAL 50 DAY, NOW()),
(0.01,    999999.99, 0.0200, 9, 2, NOW() - INTERVAL 30 DAY, NOW()),
(0.01,    100000.00, 0.0025, 10, 2, NOW() - INTERVAL 60 DAY, NOW());

-- ============================================================
-- 15. t_wechat_config — 微信支付配置表（10条）
--     status: 1-启用, 2-停用
-- ============================================================
INSERT INTO t_wechat_config (config_name, app_id, mch_id, api_v3_key, serial_no, private_key, status, weight, last_test_time, last_test_result, created_at, updated_at) VALUES
('微信主配置-星辰科技', 'wx2024010100000001', '1900000001', 'a1b2c3d4e5f60718293a4b5c6d7e8f90', '5F4A3B2C1D0E9F8A7B6C5D4E3F2A1B0C9D8E7F6', 'MIIEvQIBADANBgkqhki...', 1, 70,  NOW() - INTERVAL 30 DAY, 1, NOW() - INTERVAL 60 DAY, NOW()),
('微信配置-银河数码',   'wx2024021500000002', '1900000002', 'f0e9d8c7b6a594837261504132231a0b', '4E3D2C1B0A9F8E7D6C5B4A39281706F5E4D3C2B1', 'MIIEvQIBADANBgkqhki...', 1, 30,  NOW() - INTERVAL 20 DAY, 1, NOW() - INTERVAL 45 DAY, NOW()),
('微信配置-蓝海电商',   'wx2024032000000003', '1900000003', '1234567890abcdef1234567890abcdef', '3D2C1B0A9F8E7D6C5B4A39281706F5E4D3C2B1A', 'MIIEvQIBADANBgkqhki...', 2, 50,  NOW() - INTERVAL 15 DAY, 2, NOW() - INTERVAL 30 DAY, NOW()),
('微信配置-云帆支付',   'wx2024040100000004', '1900000004', '12ab34cd56ef78ab90cd12ef34ab56cd', '2C1B0A9F8E7D6C5B4A39281706F5E4D3C2B1A09', 'MIIEvQIBADANBgkqhki...', 1, 100, NOW() - INTERVAL 10 DAY, 1, NOW() - INTERVAL 20 DAY, NOW()),
('微信配置-极光互动',   'wx2024051000000005', '1900000005', '89ef12cd34ab5678cd90ef12ab34cd56', '1B0A9F8E7D6C5B4A39281706F5E4D3C2B1A098F', 'MIIEvQIBADANBgkqhki...', 2, 0,   NULL, NULL, NOW() - INTERVAL 15 DAY, NOW()),
('微信配置-鼎新财务',   'wx2024060100000006', '1900000006', 'ef34cd12ab90ef56cd34ab12ef78cd90', '0A9F8E7D6C5B4A39281706F5E4D3C2B1A098F7E', 'MIIEvQIBADANBgkqhki...', 1, 80,  NOW() - INTERVAL 5 DAY, 1, NOW() - INTERVAL 10 DAY, NOW()),
('微信配置-海川贸易',   'wx2024061500000007', '1900000007', 'cd12ef34ab56cd78ef90ab12cd34ef56', '9F8E7D6C5B4A39281706F5E4D3C2B1A098F7E6D', 'MIIEvQIBADANBgkqhki...', 1, 60,  NOW() - INTERVAL 3 DAY, 1, NOW() - INTERVAL 7 DAY, NOW()),
('微信配置-繁花生活',   'wx2024070100000008', '1900000008', 'ab90ef12cd34ef56ab78cd90ef12cd34', '8E7D6C5B4A39281706F5E4D3C2B1A098F7E6D5C', 'MIIEvQIBADANBgkqhki...', 2, 0,   NOW() - INTERVAL 2 DAY, 2, NOW() - INTERVAL 5 DAY, NOW()),
('微信配置-锐思数据',   'wx2024072000000009', '1900000009', '56ab78cd90ef12cd34ab56cd78ef90ab', '7D6C5B4A39281706F5E4D3C2B1A098F7E6D5C4B', 'MIIEvQIBADANBgkqhki...', 1, 90,  NOW() - INTERVAL 1 DAY, 1, NOW() - INTERVAL 3 DAY, NOW()),
('微信配置-天工智能',   'wx2024080100000010', '1900000010', 'ef90ab12cd34ef56ab78cd90ef12cd34', '6C5B4A39281706F5E4D3C2B1A098F7E6D5C4B3A', 'MIIEvQIBADANBgkqhki...', 1, 50,  NULL, NULL, NOW() - INTERVAL 1 DAY, NOW());

-- ============================================================
-- 验证数据
-- ============================================================
SELECT '=== 数据统计 ===' AS '';
SELECT 't_merchant' AS table_name, COUNT(*) AS count FROM t_merchant
UNION ALL SELECT 't_user', COUNT(*) FROM t_user
UNION ALL SELECT 't_alipay_config', COUNT(*) FROM t_alipay_config
UNION ALL SELECT 't_wechat_config', COUNT(*) FROM t_wechat_config
UNION ALL SELECT 't_qrcode', COUNT(*) FROM t_qrcode
UNION ALL SELECT 't_order', COUNT(*) FROM t_order
UNION ALL SELECT 't_payment_log', COUNT(*) FROM t_payment_log
UNION ALL SELECT 't_commission', COUNT(*) FROM t_commission
UNION ALL SELECT 't_withdrawal', COUNT(*) FROM t_withdrawal
UNION ALL SELECT 't_referral_relation', COUNT(*) FROM t_referral_relation
UNION ALL SELECT 't_operation_log', COUNT(*) FROM t_operation_log
UNION ALL SELECT 't_sms_code', COUNT(*) FROM t_sms_code
UNION ALL SELECT 't_captcha_ticket', COUNT(*) FROM t_captcha_ticket
UNION ALL SELECT 't_user_token', COUNT(*) FROM t_user_token
UNION ALL SELECT 't_commission_config', COUNT(*) FROM t_commission_config;
