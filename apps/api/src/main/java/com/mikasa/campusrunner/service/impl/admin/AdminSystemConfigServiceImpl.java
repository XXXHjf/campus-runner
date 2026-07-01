package com.mikasa.campusrunner.service.impl.admin;

import com.mikasa.campusrunner.common.properties.SystemConfigProperties;
import com.mikasa.campusrunner.mapper.AdminSystemConfigMapper;
import com.mikasa.campusrunner.pojo.entity.SystemConfig;
import com.mikasa.campusrunner.service.admin.AdminSystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * author  Edith
 * created  2026/3/8 21:13
 */
@Service
@Slf4j
public class AdminSystemConfigServiceImpl implements AdminSystemConfigService {

    @Autowired
    private AdminSystemConfigMapper adminSystemConfigMapper;

    @Autowired
    private SystemConfigProperties systemConfigProperties;
    /**
     * 获取服务费率
     * @return
     */
    @Override
    public String getServiceFeeRate() {
        SystemConfig systemConfig = adminSystemConfigMapper.getByConfigKey(systemConfigProperties.getServiceFeeRate());
        return systemConfig.getConfigValue();
    }

    /**
     * 获取最低服务费
     * @return
     */
    @Override
    public String getServiceFeeMin() {
        SystemConfig systemConfig = adminSystemConfigMapper.getByConfigKey(systemConfigProperties.getServiceFeeMin());
        return systemConfig.getConfigValue();
    }

    /**
     * 修改服务费率
     * @param rate
     */
    @Override
    public void updateServiceFeeRate(BigDecimal rate) {
        adminSystemConfigMapper.updateByConfigKey(systemConfigProperties.getServiceFeeRate(), rate.toString());
    }

    /**
     * 修改最低服务费
     * @param serviceFeeMin
     */
    @Override
    public void updateServiceFeeMin(BigDecimal serviceFeeMin) {
        adminSystemConfigMapper.updateByConfigKey(systemConfigProperties.getServiceFeeMin(), serviceFeeMin.toString());
    }
}
