package com.payment.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.platform.entity.Merchant;

public interface MerchantService {

    /** 根据商户ID查询 */
    Merchant getById(Long merchantId);

    /** 修改商户信息 */
    void update(Merchant merchant);

    /** 修改密码 */
    void changePassword(Long merchantId, String oldPassword, String newPassword);

    /** 管理员查询商户列表 */
    Page<Merchant> queryPage(String merchantName, String phone, Long page, Long size);

    /** 管理员新增商户 */
    void create(Merchant merchant);

    /** 管理员停用/启用商户 */
    void toggleStatus(Long merchantId, Integer status);

    /** 管理员编辑商户信息（手机号变更时同步关联用户） */
    void adminUpdate(Merchant merchant);

    /** 配置商户开放API（开关/回调地址/IP白名单） */
    void updateApiConfig(Long merchantId, Integer apiEnabled, String notifyUrl, String ipWhitelist);

    /** 重置商户API密钥并返回新密钥 */
    String resetApiSecret(Long merchantId);

    /** 删除商户（级联清理关联用户/推荐关系/码牌/订单/佣金/提现等数据） */
    void deleteMerchant(Long merchantId);
}
