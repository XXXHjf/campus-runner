package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.exception.OrderException;
import com.mikasa.campusrunner.common.exception.ParamException;
import com.mikasa.campusrunner.common.properties.SystemConfigProperties;
import com.mikasa.campusrunner.mapper.AdminSystemConfigMapper;
import com.mikasa.campusrunner.pojo.entity.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RunnerAmountPolicyTest {
    private final AdminSystemConfigMapper configs = mock(AdminSystemConfigMapper.class);
    private static BigDecimal money(String value) { return new BigDecimal(value); }
    private void limit(String value) {
        SystemConfig config = new SystemConfig(); config.setConfigValue(value);
        when(configs.getByConfigKey("runner_transfer_single_max")).thenReturn(config);
    }
    @ParameterizedTest @ValueSource(strings={"", " ", "bad", "0", "-1", "200.001", "21474836.48"})
    void invalidConfigNeverRoundsOrAllowsPayment(String value) {
        limit(value);
        assertThrows(OrderException.class, () -> RunnerAmountPolicy.transferMax(configs));
    }
    @Test void exactTrailingZerosAreAllowedAndMissingConfigKeepsExistingDefault() {
        assertEquals(0, money("200").compareTo(RunnerAmountPolicy.transferMax(configs)));
        limit("200.000");
        assertEquals(money("200.00"), RunnerAmountPolicy.transferMax(configs));
    }
    @Test void normalAndPurchasePreviewApplyReceivableLimitButNormalFeeMayExceedIt() {
        limit("200");
        SystemConfigProperties props = new SystemConfigProperties(); props.setServiceFeeRate("rate"); props.setServiceFeeMin("min");
        SystemConfig rate = new SystemConfig(); rate.setConfigValue("0.10");
        when(configs.getByConfigKey("rate")).thenReturn(rate);
        OrderAmountServiceImpl service = new OrderAmountServiceImpl(configs, props);
        Category normal = new Category(); normal.setEnabled(1); normal.setCategoryCode("DELIVERY");
        var quote = service.calculate(normal, money("200"), null);
        assertEquals(money("200.00"), quote.getRunnerReceivable());
        assertEquals(money("220.00"), quote.getPayAmount());
        assertThrows(ParamException.class, () -> service.calculate(normal, money("200.01"), null));
        Category purchase = new Category(); purchase.setEnabled(1); purchase.setCategoryCode("PURCHASE");
        assertThrows(ParamException.class, () -> service.calculate(purchase, money("10"), money("190")));
    }
    @Test void paymentUsesStoredFeeAndRejectsLoweredLimitWithoutChangingOrder() {
        Order order=Order.builder().businessType("NORMAL").price(money("150")).productAmount(money("0"))
                .serviceFee(money("25")).payAmount(money("175")).build();
        limit("200"); RunnerAmountPolicy.beforePayment(order, configs);
        limit("149.99");
        assertThrows(OrderException.class, () -> RunnerAmountPolicy.beforePayment(order, configs));
        assertEquals(money("175"), order.getPayAmount());
        assertEquals(money("25"), order.getServiceFee());
        verify(configs, never()).getByConfigKey("service_fee_rate");
    }
    @Test void invalidStoredAmountIsRejectedInsteadOfNettingNegativeComponents() {
        Order order=Order.builder().businessType("PURCHASE").price(money("-1")).productAmount(money("2"))
                .serviceFee(money("0")).payAmount(money("1")).build();
        assertThrows(OrderException.class, () -> RunnerAmountPolicy.beforePayment(order, configs));
        verifyNoInteractions(configs);
    }
}
