package com.payment.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.platform.entity.Qrcode;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QrCodeMapper extends BaseMapper<Qrcode> {
}
