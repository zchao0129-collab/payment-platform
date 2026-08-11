package com.payment.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.platform.common.BusinessException;
import com.payment.platform.entity.User;
import com.payment.platform.mapper.UserMapper;
import com.payment.platform.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Page<User> queryPage(String keyword, Integer role, Long page, Long size) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w
                    .like(User::getUsername, keyword)
                    .or()
                    .like(User::getPhone, keyword));
        }
        if (role != null) {
            wrapper.eq(User::getRole, role);
        }
        wrapper.orderByDesc(User::getCreatedAt);
        return userMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public void create(User user) {
        // 检查手机号唯一
        if (userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone, user.getPhone())) > 0) {
            throw new BusinessException("该手机号已存在");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(1);
        userMapper.insert(user);
        log.info("用户创建: username={}, role={}", user.getUsername(), user.getRole());
    }

    @Override
    public void update(User user) {
        User db = userMapper.selectById(user.getId());
        if (db == null) {
            throw new BusinessException("用户不存在");
        }
        db.setUsername(user.getUsername());
        db.setPhone(user.getPhone());
        db.setRole(user.getRole());
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            db.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        userMapper.updateById(db);
        log.info("用户更新: id={}", user.getId());
    }

    @Override
    public void toggleStatus(Long userId, Integer status) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
        log.info("用户状态变更: id={}, status={}", userId, status);
    }
}
