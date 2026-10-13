package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.constant.OrderBusinessConstant;
import com.mikasa.campusrunner.common.exception.OrderException;
import com.mikasa.campusrunner.mapper.AdminSystemConfigMapper;
import com.mikasa.campusrunner.pojo.entity.Order;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** One settlement ceiling; existing order amounts are never recalculated with new fees. */
public final class RunnerAmountPolicy {
    private RunnerAmountPolicy() {}

    public static BigDecimal transferMax(AdminSystemConfigMapper configs) {
        var config = configs.getByConfigKey(OrderBusinessConstant.CONFIG_RUNNER_TRANSFER_SINGLE_MAX);
        if (config == null || config.getConfigValue() == null) return OrderBusinessConstant.DEFAULT_AMOUNT_LIMIT;
        try {
            BigDecimal value = new BigDecimal(config.getConfigValue()).setScale(2, RoundingMode.UNNECESSARY);
            if (value.signum() <= 0 || value.compareTo(new BigDecimal("21474836.47")) > 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException | ArithmeticException error) {
            throw new OrderException("收款额度配置有误，请联系平台处理");
        }
    }

    public static void validate(BigDecimal receivable, BigDecimal payAmount, boolean purchase, BigDecimal max) {
        if (receivable == null || payAmount == null || receivable.signum() < 0
                || payAmount.compareTo(receivable) < 0) {
            throw new OrderException("订单金额有误，请联系平台核对");
        }
        try {
            receivable.setScale(2, RoundingMode.UNNECESSARY);
            payAmount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException error) {
            throw new OrderException("订单金额有误，请联系平台核对");
        }
        if (receivable.compareTo(max) > 0 || (purchase && payAmount.compareTo(max) > 0)) {
            throw new OrderException("金额超出当前额度，请调整金额后重新下单");
        }
    }

    public static void beforePayment(Order order, AdminSystemConfigMapper configs) {
        BigDecimal price = order.getPrice() == null ? BigDecimal.ZERO : order.getPrice();
        BigDecimal product = order.getProductAmount() == null ? BigDecimal.ZERO : order.getProductAmount();
        if (price.signum() < 0 || product.signum() < 0) {
            throw new OrderException("订单金额有误，请联系平台核对");
        }
        BigDecimal receivable = price.add(product);
        BigDecimal fee = order.getServiceFee() == null ? BigDecimal.ZERO : order.getServiceFee();
        if (fee.signum() < 0 || order.getPayAmount() == null
                || receivable.add(fee).compareTo(order.getPayAmount()) != 0) {
            throw new OrderException("订单金额有误，请联系平台核对");
        }
        validate(receivable, order.getPayAmount(), OrderBusinessConstant.PURCHASE.equals(order.getBusinessType()),
                transferMax(configs));
    }
}
