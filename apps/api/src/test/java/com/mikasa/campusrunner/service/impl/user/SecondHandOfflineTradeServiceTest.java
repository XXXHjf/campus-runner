package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.constant.SecondHandConstant;
import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.SecondHandException;
import com.mikasa.campusrunner.mapper.AdminSystemConfigMapper;
import com.mikasa.campusrunner.mapper.SecondHandOrderMapper;
import com.mikasa.campusrunner.mapper.SecondHandProductMapper;
import com.mikasa.campusrunner.mapper.UserMapper;
import com.mikasa.campusrunner.pojo.dto.SecondHandOrderCreateDTO;
import com.mikasa.campusrunner.pojo.entity.SecondHandOrder;
import com.mikasa.campusrunner.pojo.entity.SecondHandProduct;
import com.mikasa.campusrunner.pojo.entity.SystemConfig;
import com.mikasa.campusrunner.pojo.vo.SecondHandOrderVO;
import com.mikasa.campusrunner.pojo.vo.UserVO;
import com.mikasa.campusrunner.service.MediaAssetService;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecondHandOfflineTradeServiceTest {

    @Mock
    private SecondHandProductMapper productMapper;
    @Mock
    private SecondHandOrderMapper orderMapper;
    @Mock
    private AdminSystemConfigMapper configMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private CloseableHttpClient wxPayClient;
    @Mock
    private MediaAssetService mediaAssetService;

    @Mock
    private SecondHandSubscriptionService subscriptions;

    @InjectMocks
    private SecondHandServiceImpl service;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentId(100L);
        SystemConfig offline = new SystemConfig();
        offline.setConfigKey(SecondHandConstant.CONFIG_TRADE_MODE);
        offline.setConfigValue(SecondHandConstant.TRADE_MODE_OFFLINE);
        when(configMapper.getByConfigKey(anyString())).thenAnswer(invocation ->
                SecondHandConstant.CONFIG_TRADE_MODE.equals(invocation.getArgument(0)) ? offline : null);
        UserVO authenticatedUser = new UserVO();
        authenticatedUser.setAuthentication(1);
        when(userMapper.getById(anyLong())).thenReturn(authenticatedUser);
    }

    @AfterEach
    void tearDown() {
        BaseContext.removeCurrentId();
    }

    @Test
    void createsOfflineOrderWithoutPaymentOrServiceFeeAndLocksProductForTrading() throws Exception {
        SecondHandProduct product = SecondHandProduct.builder()
                .id(10L)
                .sellerId(200L)
                .price(new BigDecimal("88.00"))
                .pickupOnly(1)
                .pickupAddressSnapshot("大学城校区 宿舍 1栋")
                .status(SecondHandConstant.PRODUCT_ON_SALE)
                .build();
        when(productMapper.getById(10L)).thenReturn(product);
        when(productMapper.lockOnSaleProduct(10L)).thenReturn(1);
        doAnswer(invocation -> {
            invocation.<SecondHandOrder>getArgument(0).setId(99L);
            return null;
        }).when(orderMapper).insert(any(SecondHandOrder.class));
        when(orderMapper.detail(99L)).thenReturn(new SecondHandOrderVO());

        SecondHandOrderCreateDTO dto = new SecondHandOrderCreateDTO();
        dto.setProductId(10L);
        dto.setDeliveryMode(0);

        service.createOrder(dto);

        ArgumentCaptor<SecondHandOrder> captor = ArgumentCaptor.forClass(SecondHandOrder.class);
        verify(orderMapper).insert(captor.capture());
        SecondHandOrder saved = captor.getValue();
        assertEquals(SecondHandConstant.TRADE_MODE_OFFLINE, saved.getTradeMode());
        assertEquals(SecondHandConstant.ORDER_OFFLINE_WAIT_DELIVERY, saved.getStatus());
        assertEquals(0, saved.getServiceFeeRate().compareTo(BigDecimal.ZERO));
        assertEquals(0, saved.getServiceFee().compareTo(BigDecimal.ZERO));
        assertEquals(0, saved.getSellerIncome().compareTo(new BigDecimal("88.00")));
        assertEquals(0, saved.getTransferAttempt());
        verify(productMapper).markTrading(10L);
        verify(wxPayClient, never()).execute(any(HttpUriRequest.class));
    }

    @Test
    void keepsSellerDeliveryAndBuyerAddressSnapshotOnOfflineOrder() {
        SecondHandProduct product = SecondHandProduct.builder()
                .id(10L)
                .sellerId(200L)
                .price(new BigDecimal("88.00"))
                .pickupOnly(0)
                .pickupAddressSnapshot("大学城校区 宿舍 1栋")
                .status(SecondHandConstant.PRODUCT_ON_SALE)
                .build();
        when(productMapper.getById(10L)).thenReturn(product);
        when(productMapper.lockOnSaleProduct(10L)).thenReturn(1);
        doAnswer(invocation -> {
            invocation.<SecondHandOrder>getArgument(0).setId(99L);
            return null;
        }).when(orderMapper).insert(any(SecondHandOrder.class));
        when(orderMapper.detail(99L)).thenReturn(new SecondHandOrderVO());

        SecondHandOrderCreateDTO dto = new SecondHandOrderCreateDTO();
        dto.setProductId(10L);
        dto.setDeliveryMode(1);
        dto.setBuyerDeliveryAddressId(300L);
        dto.setBuyerDeliveryAddressSnapshot("大学城校区 2栋 201室");
        dto.setDeliveryRemark("晚上八点后送达");

        service.createOrder(dto);

        ArgumentCaptor<SecondHandOrder> captor = ArgumentCaptor.forClass(SecondHandOrder.class);
        verify(orderMapper).insert(captor.capture());
        SecondHandOrder saved = captor.getValue();
        assertEquals(1, saved.getDeliveryMode());
        assertEquals(300L, saved.getBuyerDeliveryAddressId());
        assertEquals("大学城校区 2栋 201室", saved.getBuyerDeliveryAddressSnapshot());
        assertEquals("晚上八点后送达", saved.getDeliveryRemark());
    }

    @Test
    void letsSellerCancelOfflineOrderBeforeDeliveryAndReleasesProduct() {
        BaseContext.setCurrentId(200L);
        SecondHandOrder order = offlineOrder(SecondHandConstant.ORDER_OFFLINE_WAIT_DELIVERY);
        when(orderMapper.listUnpaidTimeout(any(LocalDateTime.class))).thenReturn(List.of());
        when(orderMapper.getById(99L)).thenReturn(order);

        service.cancelOrder(99L, null);

        assertEquals(SecondHandConstant.ORDER_CANCELED, order.getStatus());
        assertEquals("卖家取消线下交易", order.getCancelReason());
        verify(subscriptions).order(order, 100L, "已取消", "卖家已取消订单", order.getCancelTime());
        verify(orderMapper).update(order);
        verify(productMapper).releaseRefundedProduct(10L);
    }

    @Test
    void letsBuyerCancelOfflineOrderBeforeDeliveryAndReleasesProduct() {
        SecondHandOrder order = offlineOrder(SecondHandConstant.ORDER_OFFLINE_WAIT_DELIVERY);
        when(orderMapper.listUnpaidTimeout(any(LocalDateTime.class))).thenReturn(List.of());
        when(orderMapper.getById(99L)).thenReturn(order);

        service.cancelOrder(99L, null);

        assertEquals(SecondHandConstant.ORDER_CANCELED, order.getStatus());
        assertEquals("买家取消线下交易", order.getCancelReason());
        verify(subscriptions).order(order, 200L, "已取消", "买家已取消订单", order.getCancelTime());
        verify(orderMapper).update(order);
        verify(productMapper).releaseRefundedProduct(10L);
    }

    @Test
    void exposesCounterpartyPhoneImmediatelyAfterOfflineOrderIsCreated() {
        SecondHandOrder order = offlineOrder(SecondHandConstant.ORDER_OFFLINE_WAIT_DELIVERY);
        SecondHandOrderVO detail = new SecondHandOrderVO();
        detail.setId(99L);
        detail.setProductId(10L);
        detail.setBuyerId(100L);
        detail.setSellerId(200L);
        detail.setTradeMode(SecondHandConstant.TRADE_MODE_OFFLINE);
        detail.setStatus(SecondHandConstant.ORDER_OFFLINE_WAIT_DELIVERY);
        detail.setCreateTime(LocalDateTime.now());
        UserVO seller = new UserVO();
        seller.setPhone("13800000000");
        when(orderMapper.listUnpaidTimeout(any(LocalDateTime.class))).thenReturn(List.of());
        when(orderMapper.getById(99L)).thenReturn(order);
        when(orderMapper.detail(99L)).thenReturn(detail);
        when(mediaAssetService.resolvePublicBinding(anyString(), anyLong(), anyString())).thenReturn(List.of());
        when(userMapper.getById(200L)).thenReturn(seller);

        SecondHandOrderVO result = service.orderDetail(99L);

        assertEquals("13800000000", result.getCounterpartyPhone());
    }

    @Test
    void completesOfflineOrderWithoutStartingSellerTransfer() throws Exception {
        SecondHandOrder order = offlineOrder(SecondHandConstant.ORDER_DELIVERED_WAIT_CONFIRM);
        when(orderMapper.listUnpaidTimeout(any(LocalDateTime.class))).thenReturn(List.of());
        when(orderMapper.getById(99L)).thenReturn(order);

        service.confirmOrder(99L);

        assertEquals(SecondHandConstant.ORDER_COMPLETED, order.getStatus());
        verify(orderMapper).update(order);
        verify(productMapper).markSold(10L);
        verify(subscriptions).order(order, 200L, "已完成", "交易已完成，感谢使用", order.getFinishTime());
        verify(wxPayClient, never()).execute(any(HttpUriRequest.class));
    }

    @Test
    void rejectsNewWechatPaymentWhileOfflineModeIsActive() throws Exception {
        SecondHandException error = assertThrows(SecondHandException.class, () -> service.jsapiPay(99L));
        assertEquals("当前仅支持线下交易，请与卖家协商完成付款", error.getMessage());
        verify(orderMapper, never()).getById(any());
        verify(wxPayClient, never()).execute(any(HttpUriRequest.class));
    }

    private SecondHandOrder offlineOrder(int status) {
        return SecondHandOrder.builder()
                .id(99L)
                .productId(10L)
                .buyerId(100L)
                .sellerId(200L)
                .tradeMode(SecondHandConstant.TRADE_MODE_OFFLINE)
                .status(status)
                .deleted(0)
                .createTime(LocalDateTime.now())
                .build();
    }
}
