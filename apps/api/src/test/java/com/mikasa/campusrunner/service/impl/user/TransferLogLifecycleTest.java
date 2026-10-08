package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.mapper.WxTransferLogMapper;
import com.mikasa.campusrunner.mapper.OrderMapper;
import com.mikasa.campusrunner.pojo.entity.Order;
import com.mikasa.campusrunner.pojo.entity.WxTransferLog;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.*;

class TransferLogLifecycleTest {
    @Test void duplicateNotificationsUpdateExistingLogAndLateInitiationCannotOverwriteSuccess() {
        WxTransferLogMapper mapper = mock(WxTransferLogMapper.class);
        WxTransferLogServiceImpl service = new WxTransferLogServiceImpl();
        ReflectionTestUtils.setField(service, "wxTransferLogMapper", mapper);
        OrderMapper orders = mock(OrderMapper.class);
        ReflectionTestUtils.setField(service, "orderMapper", orders);
        when(orders.getByOrderNumberForUpdate("O1")).thenReturn(Order.builder().id(1L).build());
        when(mapper.getByOrderNumber("O1")).thenReturn(WxTransferLog.builder().state("WAIT_USER_CONFIRM").build());
        service.savePaymentInfoLog("{\"out_bill_no\":\"O1\",\"state\":\"SUCCESS\",\"transfer_amount\":100}");
        verify(mapper).updateByOrderNumber(argThat(log -> "SUCCESS".equals(log.getState())));
        verify(mapper, never()).insert(any());
        clearInvocations(mapper);
        when(mapper.getByOrderNumber("O1")).thenReturn(WxTransferLog.builder().state("SUCCESS").build());
        service.savePaymentInfoLog("{\"out_bill_no\":\"O1\",\"state\":\"WAIT_USER_CONFIRM\"}");
        verify(mapper, never()).updateByOrderNumber(any());
        verify(mapper, never()).insert(any());
    }
}
