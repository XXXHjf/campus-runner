package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.constant.OrderBusinessConstant;
import com.mikasa.campusrunner.common.exception.ParamException;
import com.mikasa.campusrunner.common.properties.SystemConfigProperties;
import com.mikasa.campusrunner.mapper.AdminSystemConfigMapper;
import com.mikasa.campusrunner.pojo.entity.Category;
import com.mikasa.campusrunner.pojo.entity.SystemConfig;
import com.mikasa.campusrunner.pojo.vo.OrderAmountVO;
import com.mikasa.campusrunner.service.user.OrderAmountService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class OrderAmountServiceImpl implements OrderAmountService {
    private final AdminSystemConfigMapper configMapper;
    private final SystemConfigProperties configProperties;

    public OrderAmountServiceImpl(AdminSystemConfigMapper configMapper, SystemConfigProperties configProperties) {
        this.configMapper = configMapper;
        this.configProperties = configProperties;
    }

    @Override
    public OrderAmountVO calculate(Category category, BigDecimal runnerFee, BigDecimal productAmount) {
        if (category == null || !Integer.valueOf(1).equals(category.getEnabled())) {
            throw new ParamException("该跑腿类型暂不可发布");
        }
        BigDecimal normalizedRunnerFee = money(runnerFee, "请输入正确的跑腿费");
        boolean purchase = OrderBusinessConstant.CATEGORY_PURCHASE.equals(category.getCategoryCode());
        BigDecimal normalizedProductAmount = purchase
                ? money(productAmount, "请输入正确的商品金额")
                : BigDecimal.ZERO.setScale(2);
        BigDecimal rate = nonNegativeConfig(configProperties.getServiceFeeRate(), BigDecimal.ZERO, false);
        BigDecimal minimumFee = nonNegativeConfig(configProperties.getServiceFeeMin(), BigDecimal.ZERO, false);
        BigDecimal serviceFee = normalizedRunnerFee.multiply(rate).setScale(2, RoundingMode.HALF_UP).max(minimumFee);
        BigDecimal payAmount = normalizedProductAmount.add(normalizedRunnerFee).add(serviceFee).setScale(2, RoundingMode.HALF_UP);
        BigDecimal runnerReceivable = normalizedProductAmount.add(normalizedRunnerFee).setScale(2, RoundingMode.HALF_UP);
        BigDecimal transferMax = nonNegativeConfig(OrderBusinessConstant.CONFIG_RUNNER_TRANSFER_SINGLE_MAX,
                OrderBusinessConstant.DEFAULT_AMOUNT_LIMIT, true);
        BigDecimal payMax = transferMax;
        if (purchase && payAmount.compareTo(payMax) > 0) {
            throw new ParamException("预计实付不能超过" + payMax.stripTrailingZeros().toPlainString() + "元");
        }
        if (purchase && runnerReceivable.compareTo(transferMax) > 0) {
            throw new ParamException("商品金额与跑腿费合计不能超过" + transferMax.stripTrailingZeros().toPlainString() + "元");
        }
        return OrderAmountVO.builder()
                .businessType(purchase ? OrderBusinessConstant.PURCHASE : OrderBusinessConstant.NORMAL)
                .productAmount(normalizedProductAmount)
                .runnerFee(normalizedRunnerFee)
                .serviceFeeRate(rate)
                .serviceFee(serviceFee)
                .payAmount(payAmount)
                .runnerReceivable(runnerReceivable)
                .purchaseOrderPayMax(payMax)
                .runnerTransferSingleMax(transferMax)
                .build();
    }

    private BigDecimal money(BigDecimal value, String message) {
        if (value == null || value.signum() <= 0 || value.scale() > 2) {
            throw new ParamException(message);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nonNegativeConfig(String key, BigDecimal fallback, boolean positive) {
        SystemConfig config = configMapper.getByConfigKey(key);
        if (config == null || config.getConfigValue() == null || config.getConfigValue().isBlank()) {
            return fallback.setScale(2, RoundingMode.HALF_UP);
        }
        try {
            BigDecimal value = new BigDecimal(config.getConfigValue()).setScale(2, RoundingMode.HALF_UP);
            if (value.signum() < 0 || (positive && value.signum() == 0)) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new ParamException("系统金额配置有误，请联系管理员");
        }
    }
}
