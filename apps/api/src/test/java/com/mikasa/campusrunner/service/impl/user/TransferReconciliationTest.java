package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.mapper.WxTransferLogMapper;
import com.mikasa.campusrunner.pojo.entity.*;
import com.mikasa.campusrunner.service.user.*;
import com.mikasa.campusrunner.task.OrderTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.mockito.Mockito.*;

class TransferReconciliationTest {
    @ParameterizedTest @ValueSource(strings={"FAIL", "CANCELLED"})
    void providerFailureOrCancellationDoesNotRemainPendingCollection(String state) throws Exception {
        WeChatTransferServiceImpl service = spy(new WeChatTransferServiceImpl());
        WxTransferLogMapper mapper = mock(WxTransferLogMapper.class);
        WxTransferLogService logs = mock(WxTransferLogService.class);
        OrderService orders = mock(OrderService.class);
        ReflectionTestUtils.setField(service, "wxTransferLogMapper", mapper);
        ReflectionTestUtils.setField(service, "wxTransferLogService", logs);
        ReflectionTestUtils.setField(service, "orderService", orders);
        Order order = Order.builder().orderNumber("O1").status(5).build();
        when(mapper.getByOrderNumber("O1")).thenReturn(WxTransferLog.builder().state("WAIT_USER_CONFIRM").build());
        String response = "{\"out_bill_no\":\"O1\",\"state\":\"" + state + "\"}";
        doReturn(response).when(service).queryOrder("O1");
        service.checkOrderWithdrawalState(order);
        verify(orders).updateStatusByOrderNumber("O1", 7);
        verify(logs).savePaymentInfoLog(response);
    }

    @Test void failedOrderCheckDoesNotBlockOtherOrdersInSameScheduledBatch() throws Exception {
        OrderService orders = mock(OrderService.class);
        WeChatTransferService transfers = mock(WeChatTransferService.class);
        OrderTask task = new OrderTask();
        ReflectionTestUtils.setField(task, "orderService", orders);
        ReflectionTestUtils.setField(task, "weChatTransferService", transfers);
        Order first = Order.builder().orderNumber("O1").build(), second = Order.builder().orderNumber("O2").build();
        when(orders.getNoWithdrawal()).thenReturn(List.of(first, second));
        doThrow(new java.io.IOException("provider unavailable")).when(transfers).checkOrderWithdrawalState(first);
        task.processWithdrawalState();
        verify(transfers).checkOrderWithdrawalState(second);
    }
}
