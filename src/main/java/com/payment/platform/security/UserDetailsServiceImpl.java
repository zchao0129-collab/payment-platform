package com.payment.platform.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.platform.entity.User;
import com.payment.platform.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
                        .or()
                        .eq(User::getPhone, username)
        );

        if (user == null) {
            throw new UsernameNotFoundException("账号不存在: " + username);
        }

        return new org.springframework.security.core.userdetails.User(
                String.valueOf(user.getId()),
                user.getPassword(),
                user.getStatus() == 1,
                true, true, true,
                Collections.emptyList()
        );
    }
}
