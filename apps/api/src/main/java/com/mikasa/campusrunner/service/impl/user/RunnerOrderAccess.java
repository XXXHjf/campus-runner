package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.constant.DeleteConstant;
import com.mikasa.campusrunner.common.constant.TakeOrderStatusConstant;
import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.OrderException;
import com.mikasa.campusrunner.pojo.entity.TakeOrder;

/** User API roles only; administrative reads use their own authenticated routes. */
final class RunnerOrderAccess {
    private RunnerOrderAccess() {}
    static Long requireUser() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) throw new OrderException("登录已失效，请重新登录");
        return userId;
    }
    static boolean activeTake(TakeOrder take, Long orderId) {
        return take != null && orderId != null && orderId.equals(take.getOrderId())
                && DeleteConstant.UN_DELETED.equals(take.getDeleted())
                && java.util.Arrays.asList(TakeOrderStatusConstant.ALREADY_TAKE_ORDER,
                    TakeOrderStatusConstant.DELIVERYING, TakeOrderStatusConstant.ORDER_FINISH).contains(take.getStatus());
    }
    static boolean participant(Long caller, Long publisher, TakeOrder take, Long orderId) {
        return caller != null && (caller.equals(publisher)
                || (activeTake(take,orderId) && caller.equals(take.getUserId())));
    }
}
