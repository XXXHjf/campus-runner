package com.mikasa.campusrunner.service.admin;

import java.math.BigDecimal;

/**
 * author  Edith
 * created  2026/3/8 21:12
 */
public interface AdminSystemConfigService {

    /**
     * 获取服务费率
     * @return
     */
    String getServiceFeeRate();

    /**
     * 获取最低服务费
     * @return
     */
    String getServiceFeeMin();

    /**
     * 修改服务费率
     * @param rate
     */
    void updateServiceFeeRate(BigDecimal rate);

    /**
     * 修改最低服务费
     * @param serviceFeeMin
     */
    void updateServiceFeeMin(BigDecimal serviceFeeMin);
}
