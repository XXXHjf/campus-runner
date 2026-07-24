package com.mikasa.campusrunner.mapper;

import com.mikasa.campusrunner.pojo.entity.WxTransferLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * author  Edith
 * created  2025/3/3 10:18
 */
@Mapper
public interface WxTransferLogMapper {
    /**
     * 根据业务订单号查询转账日志
     * @param orderNumber
     * @return
     */
    WxTransferLog getByOrderNumber(String orderNumber);

    /**
     * 更新日志
     * @param wxTransferLog
     */
    void updateByOrderNumber(WxTransferLog wxTransferLog);

    /**
     * 插入
     * @param wxTransferLog
     */
    void insert(WxTransferLog wxTransferLog);
}
