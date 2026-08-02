package com.mikasa.campusrunner.service.user;

import com.mikasa.campusrunner.pojo.dto.TakeOrderQueryDTO;
import com.mikasa.campusrunner.pojo.dto.TakeOrderUpdateStatusDTO;
import com.mikasa.campusrunner.pojo.vo.TakeOrderUserInfoVO;
import com.mikasa.campusrunner.pojo.vo.TakeOrderVO;

import java.util.List;

/**
 * author  Edith
 * created  2024/4/26 19:49
 */
public interface TakeOrderService {

    /**
     * 接单
     * @param id
     */
    void take(Long id);

    /**
     * 修改状态
     * @param takeOrderUpdateStatusDTO
     */
    void updateStatus(TakeOrderUpdateStatusDTO takeOrderUpdateStatusDTO);

    /**
     * 查看我的接单
     * @return
     */
    List<TakeOrderVO> getMy();

    /**
     * 条件查询
     * @param takeOrderQueryDTO
     * @return
     */
    List<TakeOrderVO> query(TakeOrderQueryDTO takeOrderQueryDTO);

    /**
     * 根据orderId查询，返回用户信息
     * @param orderId
     * @return
     */
    TakeOrderUserInfoVO userInfo(Long orderId);

    /**
     * 根据订单id查询送达图片
     * @param orderId
     * @return
     */
    String getImageByOrderId(Long orderId);

    /**
     * 查询当前用户接单已完成但未提现订单
     * @return
     */
    List<TakeOrderVO> getNoWithdrawn();
}
