package com.payment.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.platform.entity.User;

public interface UserService {

    /** 用户列表 */
    Page<User> queryPage(String keyword, Integer role, Long page, Long size);

    /** 创建用户 */
    void create(User user);

    /** 编辑用户 */
    void update(User user);

    /** 停用/启用用户 */
    void toggleStatus(Long userId, Integer status);
}
