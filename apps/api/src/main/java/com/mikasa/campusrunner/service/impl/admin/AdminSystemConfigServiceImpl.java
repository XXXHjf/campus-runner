package com.mikasa.campusrunner.service.impl.admin;

import com.mikasa.campusrunner.common.constant.SecondHandConstant;
import com.mikasa.campusrunner.common.constant.OrderBusinessConstant;
import com.mikasa.campusrunner.common.exception.ParamException;
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

    @Override
    public String getSecondHandServiceFeeRate() {
        SystemConfig config = adminSystemConfigMapper.getByConfigKey(SecondHandConstant.CONFIG_SERVICE_FEE_RATE);
        return config == null || config.getConfigValue() == null
                ? SecondHandConstant.DEFAULT_SERVICE_FEE_RATE
                : config.getConfigValue();
    }

    @Override
    public String getRunnerTransferSingleMax() {
        return getAmountConfig(OrderBusinessConstant.CONFIG_RUNNER_TRANSFER_SINGLE_MAX);
    }

    /**
     * 修改服务费率
     * @param rate
     */
    @Override
    public void updateServiceFeeRate(BigDecimal rate) {
        validateRate(rate);
        adminSystemConfigMapper.updateByConfigKey(systemConfigProperties.getServiceFeeRate(), rate.toString());
    }

    /**
     * 修改最低服务费
     * @param serviceFeeMin
     */
    @Override
    public void updateServiceFeeMin(BigDecimal serviceFeeMin) {
        if (serviceFeeMin == null || serviceFeeMin.signum() < 0) {
            throw new ParamException("最低服务费不能小于0");
        }
        adminSystemConfigMapper.updateByConfigKey(systemConfigProperties.getServiceFeeMin(), serviceFeeMin.toString());
    }

    @Override
    public void updateSecondHandServiceFeeRate(BigDecimal rate) {
        validateRate(rate);
        adminSystemConfigMapper.updateByConfigKey(SecondHandConstant.CONFIG_SERVICE_FEE_RATE, rate.toString());
    }

    @Override
    public void updateRunnerTransferSingleMax(BigDecimal amount) {
        updatePositiveAmount(OrderBusinessConstant.CONFIG_RUNNER_TRANSFER_SINGLE_MAX, amount);
    }

    private String getAmountConfig(String key) {
        SystemConfig config = adminSystemConfigMapper.getByConfigKey(key);
        return config == null || config.getConfigValue() == null
                ? OrderBusinessConstant.DEFAULT_AMOUNT_LIMIT.toPlainString()
                : config.getConfigValue();
    }

    private void updatePositiveAmount(String key, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2) {
            throw new ParamException("金额必须大于0且最多保留两位小数");
        }
        adminSystemConfigMapper.updateByConfigKey(key, amount.toPlainString());
    }

    private void validateRate(BigDecimal rate) {
        if (rate == null || rate.signum() < 0 || rate.compareTo(BigDecimal.ONE) > 0) {
            throw new ParamException("服务费率应在0到1之间");
        }
    }
}
