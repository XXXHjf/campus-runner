package com.mikasa.campusrunner.mapper;

import com.mikasa.campusrunner.pojo.entity.SystemConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

/**
 * author  Edith
 * created  2026/3/8 21:14
 */
@Mapper
public interface AdminSystemConfigMapper {

    /**
     * 获取服务费率
     * 根据配置键获取配置信息
     * @return
     */
    SystemConfig getByConfigKey(String serviceFeeRate);

    /**
     * 修改服务费率
     * 根据配置键修改配置值
     * @param serviceFeeRate
     * @param rate
     */
    void updateByConfigKey(@Param("configKey") String serviceFeeRate, @Param("configValue") String rate);
}
