package com.payment.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.platform.entity.UserToken;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserTokenMapper extends BaseMapper<UserToken> {
}
