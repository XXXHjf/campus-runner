package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.exception.SecondHandException;
import com.mikasa.campusrunner.mapper.SecondHandOrderMapper;
import com.mikasa.campusrunner.mapper.SecondHandProductMapper;
import com.mikasa.campusrunner.pojo.entity.SecondHandOrder;
import java.util.Objects;

/** Call inside a transaction. Product -> order -> payment/refund/transfer records. */
final class SecondHandOrderLocks {
    private SecondHandOrderLocks() {}

    static SecondHandOrder byId(SecondHandOrderMapper orders, SecondHandProductMapper products, Long id) {
        return lock(orders, products, orders.getById(id));
    }

    static SecondHandOrder byNumber(SecondHandOrderMapper orders, SecondHandProductMapper products, String number) {
        return lock(orders, products, orders.getByOrderNumber(number));
    }

    private static SecondHandOrder lock(SecondHandOrderMapper orders, SecondHandProductMapper products,
                                       SecondHandOrder reference) {
        if (reference == null) return null;
        if (products.getByIdForUpdate(reference.getProductId()) == null) {
            throw new SecondHandException("商品不存在，请联系客服");
        }
        // The first read locates the immutable product ID only; lifecycle decisions use this current read.
        SecondHandOrder current = orders.getByIdForUpdate(reference.getId());
        if (current != null && !Objects.equals(reference.getProductId(), current.getProductId())) {
            throw new SecondHandException("订单状态已变化，请刷新后重试");
        }
        return current;
    }
}
