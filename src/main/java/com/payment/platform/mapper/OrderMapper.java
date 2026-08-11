package com.payment.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.platform.entity.Order;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
