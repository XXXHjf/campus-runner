package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.exception.ParamException;
import com.mikasa.campusrunner.mapper.CategoryMapper;
import com.mikasa.campusrunner.mapper.OrderMapper;
import com.mikasa.campusrunner.mapper.AddressBookMapper;
import com.mikasa.campusrunner.mapper.OrderAddressSnapshotMapper;
import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.pojo.vo.UserVO;
import com.mikasa.campusrunner.service.user.UserService;
import com.mikasa.campusrunner.pojo.dto.OrderSubmitDTO;
import com.mikasa.campusrunner.pojo.entity.Category;
import com.mikasa.campusrunner.service.MediaAssetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import com.mikasa.campusrunner.common.exception.UserException;
import com.mikasa.campusrunner.common.exception.AddressException;
import java.math.BigDecimal;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderExpectedDeliveryTimeTest {
    @Mock OrderMapper orderMapper;
    @Mock CategoryMapper categoryMapper;
    @Mock MediaAssetService mediaAssetService;
    @Mock UserService userService;
    @Mock AddressBookMapper addressBookMapper;
    @Mock OrderAddressSnapshotMapper orderAddressSnapshotMapper;
    @InjectMocks OrderServiceImpl service;

    @BeforeEach void category() {
        BaseContext.setCurrentId(10L);
        UserVO user = UserVO.builder().id(10L).deleted(0).profileCompleted(true)
                .authentication(1).schoolId(20L).build();
        lenient().when(userService.getCurrentUser()).thenReturn(user);
        lenient().when(addressBookMapper.countUsableOrderAddress(30L, 10L, 20L)).thenReturn(1);
        lenient().when(addressBookMapper.countUsableOrderAddress(31L, 10L, 20L)).thenReturn(1);
        lenient().when(orderAddressSnapshotMapper.lockUsableAddresses(30L, 31L, 10L, 20L))
                .thenReturn(List.of(30L, 31L));
        lenient().when(orderAddressSnapshotMapper.capture(any(), any(), any())).thenReturn(1);
        Category category = new Category();
        category.setEnabled(1);
        lenient().when(categoryMapper.getById(1L)).thenReturn(category);
    }

    @AfterEach void clearContext() { BaseContext.removeCurrentId(); }

    private OrderSubmitDTO request() {
        OrderSubmitDTO dto = new OrderSubmitDTO();
        dto.setCategoryId(1L);
        dto.setNote("请帮忙取件");
        dto.setImageAssetIds(List.of(1L));
        dto.setPickUpAddress(30L);
        dto.setReciveAddress(31L);
        dto.setUsername("同学");
        dto.setPhone("13800138000");
        dto.setDoorAccess(0);
        return dto;
    }

    @Test void fixedDeadlineOverridesClientGapAndKeepsMinutePrecision() {
        OrderSubmitDTO dto = request();
        LocalDateTime deadline = LocalDateTime.now().plusHours(2).withSecond(0).withNano(0);
        dto.setExpectedDeliveryTime(deadline);
        dto.setGap(999);
        var order = service.submit(dto);
        assertEquals(deadline, order.getExpectedDeliveryTime());
        assertEquals(deadline, order.getExceedTime());
        assertTrue(order.getGap() >= 119 && order.getGap() <= 120);
        assertNull(order.getDeliveryTime());
        verify(orderMapper).insert(order);
        var sequence = inOrder(orderAddressSnapshotMapper, orderMapper, mediaAssetService);
        sequence.verify(orderAddressSnapshotMapper).lockUsableAddresses(30L, 31L, 10L, 20L);
        sequence.verify(orderMapper).insert(order);
        sequence.verify(orderAddressSnapshotMapper).capture(order.getId(), 30L, "PICKUP");
        sequence.verify(orderAddressSnapshotMapper).capture(order.getId(), 31L, "RECEIVE");
    }

    @Test void expiredDeadlineCannotCreateOrderEvenWithPositiveGap() {
        OrderSubmitDTO dto = request();
        dto.setExpectedDeliveryTime(LocalDateTime.now().minusMinutes(1));
        dto.setGap(30);
        assertThrows(ParamException.class, () -> service.submit(dto));
        verify(orderMapper, never()).insert(any());
        verifyNoInteractions(mediaAssetService);
    }

    @Test void oldClientKeepsPositiveGapWithoutInventingChosenTime() {
        OrderSubmitDTO dto = request();
        dto.setGap(30);
        var order = service.submit(dto);
        assertNull(order.getExpectedDeliveryTime());
        assertEquals(30, order.getGap());
    }

    @Test void missingTimeIsRejected() {
        assertThrows(ParamException.class, () -> service.submit(request()));
        verify(orderMapper, never()).insert(any());
    }
    static Stream<Consumer<OrderSubmitDTO>> illegalRequests() {
        return Stream.of(
                d -> d.setUsername("  "),
                d -> d.setUsername("名".repeat(51)),
                d -> d.setPhone(null),
                d -> d.setPhone("12800138000"),
                d -> d.setPhone("1380013800x"),
                d -> d.setDoorAccess(null),
                d -> d.setDoorAccess(-1),
                d -> d.setDoorAccess(2),
                d -> d.setCategoryId(null),
                d -> d.setCategoryId(0L),
                d -> d.setCancelTime(LocalDateTime.now().minusSeconds(1)),
                d -> d.setGap(0),
                d -> d.setGap(-1),
                d -> d.setPrice(new BigDecimal("-1")),
                d -> d.setPrice(new BigDecimal("0.001")),
                d -> d.setProductAmount(BigDecimal.ONE),
                d -> d.setExpectedDeliveryTime(LocalDateTime.MAX));
    }

    @ParameterizedTest @MethodSource("illegalRequests")
    void rejectsInvalidRequestBeforePersistence(Consumer<OrderSubmitDTO> mutate) {
        OrderSubmitDTO dto = request();
        dto.setGap(30);
        mutate.accept(dto);
        assertThrows(ParamException.class, () -> service.submit(dto));
        verifyNoInteractions(orderMapper, mediaAssetService);
    }

    static Stream<Consumer<UserVO>> ineligibleUsers() {
        return Stream.of(u -> u.setDeleted(1), u -> u.setProfileCompleted(false),
                u -> u.setProfileCompleted(null), u -> u.setAuthentication(0),
                u -> u.setAuthentication(null), u -> u.setSchoolId(null));
    }

    @ParameterizedTest @MethodSource("ineligibleUsers")
    void rejectsIneligiblePublisher(Consumer<UserVO> mutate) {
        UserVO user = userService.getCurrentUser();
        mutate.accept(user);
        assertThrows(UserException.class, () -> service.submit(request()));
        verifyNoInteractions(orderMapper, mediaAssetService, addressBookMapper);
    }

    @Test void rejectsMissingLoginAndMissingUser() {
        BaseContext.removeCurrentId();
        assertThrows(UserException.class, () -> service.submit(request()));
        BaseContext.setCurrentId(10L);
        when(userService.getCurrentUser()).thenReturn(null);
        assertThrows(UserException.class, () -> service.submit(request()));
        verifyNoInteractions(orderMapper, mediaAssetService, addressBookMapper);
    }

    @Test void rejectsMissingBody() {
        assertThrows(ParamException.class, () -> service.submit(null));
        verifyNoInteractions(orderMapper, mediaAssetService, userService, addressBookMapper);
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(longs = {30L, 31L})
    void checksBothAddressesWithTrustedPublisherAndSchool(Long id) {
        OrderSubmitDTO dto = request();
        dto.setGap(30);
        doReturn(0).when(addressBookMapper).countUsableOrderAddress(id, 10L, 20L);
        assertThrows(AddressException.class, () -> service.submit(dto));
        verifyNoInteractions(orderMapper, mediaAssetService);
    }

    @Test void addressDeletedOrChangedBeforeLockPreventsPersistence() {
        when(orderAddressSnapshotMapper.lockUsableAddresses(30L, 31L, 10L, 20L))
                .thenReturn(List.of(30L));
        var dto = request();
        dto.setGap(30);
        assertThrows(AddressException.class, () -> service.submit(dto));
        verifyNoInteractions(orderMapper, mediaAssetService);
        verify(orderAddressSnapshotMapper, never()).capture(any(), any(), any());
    }

    @Test void incompleteSnapshotFailsSubmissionBeforeBindingImages() {
        when(orderAddressSnapshotMapper.capture(any(), eq(31L), eq("RECEIVE"))).thenReturn(0);
        var dto = request();
        dto.setGap(30);
        assertThrows(AddressException.class, () -> service.submit(dto));
        verifyNoInteractions(mediaAssetService);
    }

    @Test void sameAddressStillCapturesBothRoles() {
        when(orderAddressSnapshotMapper.lockUsableAddresses(30L, 30L, 10L, 20L))
                .thenReturn(List.of(30L));
        var dto = request();
        dto.setReciveAddress(30L);
        dto.setGap(30);
        var order = service.submit(dto);
        verify(orderAddressSnapshotMapper).capture(order.getId(), 30L, "PICKUP");
        verify(orderAddressSnapshotMapper).capture(order.getId(), 30L, "RECEIVE");
    }

    @Test void rejectsMissingOrInvalidAddressId() {
        OrderSubmitDTO dto = request();
        dto.setPickUpAddress(null);
        assertThrows(AddressException.class, () -> service.submit(dto));
        dto.setPickUpAddress(30L);
        dto.setReciveAddress(-1L);
        assertThrows(AddressException.class, () -> service.submit(dto));
        verifyNoInteractions(orderMapper, mediaAssetService);
    }

    @Test void disabledOrMissingCategoryCannotPublish() {
        Category category = new Category();
        category.setEnabled(0);
        when(categoryMapper.getById(1L)).thenReturn(category, null);
        assertThrows(ParamException.class, () -> service.submit(request()));
        assertThrows(ParamException.class, () -> service.submit(request()));
        verifyNoInteractions(orderMapper, mediaAssetService);
    }

    @Test void trimsContactAndDefaultsCancellationWithoutTrustingFeeSnapshots() {
        OrderSubmitDTO dto = request();
        dto.setGap(30);
        dto.setUsername(" 同学 ");
        dto.setPhone(" 13800138000 ");
        dto.setServiceFee(BigDecimal.TEN);
        dto.setPayAmount(BigDecimal.TEN);
        var order = service.submit(dto);
        assertEquals("同学", order.getUsername());
        assertEquals("13800138000", order.getPhone());
        assertEquals(10L, order.getUserId());
        assertEquals(0, order.getPayAmount().signum());
        assertEquals(order.getCreateTime().plusHours(24), order.getCancelTime());
    }

}
