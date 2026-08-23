package com.payment.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.platform.entity.WechatConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WechatConfigMapper extends BaseMapper<WechatConfig> {

    List<WechatConfig> selectByCondition(WechatConfig condition);

    List<WechatConfig> selectEnabled();

    WechatConfig selectByAppId(@Param("appId") String appId);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int updateTestResult(@Param("id") Long id, @Param("lastTestResult") Integer lastTestResult);

    int disableAll();
}
