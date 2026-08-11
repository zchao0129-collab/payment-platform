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
}
