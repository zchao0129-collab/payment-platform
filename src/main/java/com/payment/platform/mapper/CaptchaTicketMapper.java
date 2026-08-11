package com.payment.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.platform.entity.CaptchaTicket;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CaptchaTicketMapper extends BaseMapper<CaptchaTicket> {
}
