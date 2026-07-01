package com.mikasa.campusrunner.service.user;

import com.mikasa.campusrunner.pojo.entity.Order;
import com.mikasa.campusrunner.pojo.vo.WeChatTransferVO;

import java.security.GeneralSecurityException;
import java.util.Map;

/**
 * author  Edith
 * created  2025/3/3 8:58
 */
public interface WeChatTransferService {

    /**
     * 商家发起转账
     * @param orderId
     * @return
     */
    WeChatTransferVO wxTransfer(Long orderId) throws Exception;

    /**
     * 撤销转账
     * @param orderNumber
     */
    void closeTransfer(String orderNumber) throws Exception;

    /**
     * 根据商户单号查询账单
     * @param orderNumber
     */
    String queryOrder(String orderNumber) throws Exception;

    /**
     * 回调通知
     * 处理订单
     * @param bodyMap
     */
    void processOrder(Map<String, Object> bodyMap) throws GeneralSecurityException;

    /**
     * 核实当前订单是否已提现
     * @param order
     */
    void checkOrderWithdrawalState(Order order) throws Exception;
}
