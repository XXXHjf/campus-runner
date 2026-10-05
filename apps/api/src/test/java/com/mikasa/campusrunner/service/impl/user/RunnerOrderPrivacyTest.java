package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.OrderException;
import com.mikasa.campusrunner.common.constant.MediaAssetConstant;
import com.mikasa.campusrunner.mapper.*;
import com.mikasa.campusrunner.pojo.entity.*;
import com.mikasa.campusrunner.pojo.vo.*;
import com.mikasa.campusrunner.service.MediaAssetService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RunnerOrderPrivacyTest {
    @Mock OrderMapper orders;
    @Mock TakeOrderMapper takes;
    @Mock UserMapper users;
    @Mock MediaAssetService media;
    @InjectMocks OrderServiceImpl orderService;
    @InjectMocks TakeOrderServiceImpl takeService;
    @AfterEach void clearIdentity() { BaseContext.removeCurrentId(); }
    Order source() { return Order.builder().id(1L).userId(10L).status(3).build(); }
    TakeOrder take() {
        TakeOrder take = new TakeOrder(); take.setId(2L); take.setOrderId(1L); take.setUserId(20L);
        take.setDeleted(0); take.setStatus(2); return take;
    }
    OrderShowVO detail() {
        OrderShowVO vo = new OrderShowVO(); vo.setId(1L); vo.setUserId(10L); vo.setStatus(0);
        vo.setPhone("private-phone"); vo.setRealname("private-name"); vo.setOrderNumber("PRIVATE1");
        vo.setNote("private-note"); vo.setPickUpAddress("校园 楼栋 私人门牌");
        vo.setImage("legacy-private-url"); vo.setImages(List.of("legacy-private-url"));
        vo.setImageAssetId(7L); vo.setImageAssetIds(List.of(7L)); return vo;
    }
    BoundMediaVO image() { return BoundMediaVO.builder().mediaId(7L).url("signed-private-url").build(); }
    void privateSetup() {
        when(orders.getById(1L)).thenReturn(source());
        when(takes.getByOrderId(1L)).thenReturn(take());
    }

    @ParameterizedTest @ValueSource(longs={10,20})
    void onlyParticipantsReadContactsPurchaseAndDeliveryProof(long caller) {
        BaseContext.setCurrentId(caller); privateSetup();
        TakeOrderUserInfoVO contact = new TakeOrderUserInfoVO(); contact.setId(2L);
        contact.setPhone("private-contact"); contact.setPurchaseProofImage("legacy-private-url");
        when(takes.getUserInfoByOrderId(1L,2L)).thenReturn(contact);
        when(media.resolveAuthorizedBinding(MediaAssetConstant.BOUND_TAKE_ORDER,2L,"PURCHASE_PROOF"))
                .thenReturn(List.of(image()));
        when(media.resolveAuthorizedBinding(MediaAssetConstant.BOUND_TAKE_ORDER,2L,"DELIVERY_PROOF"))
                .thenReturn(List.of(image()));
        assertEquals("private-contact",takeService.userInfo(1L).getPhone());
        assertEquals("signed-private-url",contact.getPurchaseProofImage());
        assertEquals("signed-private-url",takeService.getImageByOrderId(1L));
    }

    @ParameterizedTest @ValueSource(longs={30,99})
    void outsidersNeverReadContactProjectionOrSignAnyProof(long caller) {
        BaseContext.setCurrentId(caller); privateSetup();
        assertThrows(OrderException.class, () -> takeService.userInfo(1L));
        assertThrows(OrderException.class, () -> takeService.getImageByOrderId(1L));
        verify(takes,never()).getUserInfoByOrderId(any(),any());
        verifyNoInteractions(media);
    }
    @Test void anonymousCannotReadDetailsContactsOrPictures() {
        assertThrows(OrderException.class, () -> orderService.detail(1L));
        assertThrows(OrderException.class, () -> takeService.userInfo(1L));
        assertThrows(OrderException.class, () -> takeService.getImageByOrderId(1L));
        verifyNoInteractions(orders,takes,media);
    }

    @ParameterizedTest @ValueSource(strings={"cancelled","deleted","wrongOrder","missing","unknownStatus"})
    void invalidTakeDoesNotAuthorizePrivateRead(String defect) {
        BaseContext.setCurrentId(20L);
        when(orders.getById(1L)).thenReturn(source());
        TakeOrder take = take();
        switch (defect) {
            case "cancelled": take.setStatus(3); break;
            case "deleted": take.setDeleted(1); break;
            case "wrongOrder": take.setOrderId(9L); break;
            case "missing": take = null; break;
            case "unknownStatus": take.setStatus(null); break;
        }
        when(takes.getByOrderId(1L)).thenReturn(take);
        assertThrows(OrderException.class, () -> takeService.userInfo(1L));
        assertThrows(OrderException.class, () -> takeService.getImageByOrderId(1L));
        verifyNoInteractions(media);
    }

    @ParameterizedTest @ValueSource(longs={10,20})
    void participantsGetFullDetailButNeverLegacyImageFallback(long caller) {
        BaseContext.setCurrentId(caller);
        when(orders.detail(1L)).thenReturn(detail());
        if (caller == 20) when(takes.getByOrderId(1L)).thenReturn(take());
        OrderShowVO result = orderService.detail(1L);
        assertEquals("private-phone",result.getPhone());
        assertEquals("private-note",result.getNote());
        assertNull(result.getImage()); assertNull(result.getImageAssetId());
        assertTrue(result.getImages().isEmpty());
        verify(orders,never()).getPublicOrderById(any());
        verify(media).resolveAuthorizedBinding(MediaAssetConstant.BOUND_ORDER,1L,"ORDER_IMAGE",0);
    }

    @Test void unrelatedPendingDetailUsesPublicProjectionWithoutSigningPrivateMedia() {
        BaseContext.setCurrentId(30L);
        when(orders.detail(1L)).thenReturn(detail());
        when(orders.getPublicOrderById(1L)).thenReturn(new PublicOrderVO(1L,BigDecimal.ONE,
                BigDecimal.ZERO,"NORMAL",null,null,30,4L,"配送","校园 楼栋","校园 楼栋"));
        OrderShowVO result = orderService.detail(1L);
        assertEquals("校园 楼栋",result.getPickUpAddress());
        assertEquals(BigDecimal.ONE,result.getPrice());
        assertNull(result.getPhone()); assertNull(result.getRealname()); assertNull(result.getUsername());
        assertNull(result.getOrderNumber()); assertNull(result.getUserId()); assertNull(result.getNote());
        assertNull(result.getImage()); assertNull(result.getImageAssetId()); assertTrue(result.getImages().isEmpty());
        verifyNoInteractions(media);
    }
    @Test void unrelatedNonPublicOrderHasNoReadableDetail() {
        BaseContext.setCurrentId(30L);
        when(orders.detail(1L)).thenReturn(detail());
        assertThrows(OrderException.class, () -> orderService.detail(1L));
        verifyNoInteractions(media);
    }
    @Test void filteredLobbyListCannotExposePrivateFieldsOrImageUrls() {
        BaseContext.setCurrentId(30L);
        when(users.getSchoolId(30L)).thenReturn(1L);
        when(orders.showByCategory(4L,1L)).thenReturn(List.of(detail()));
        when(orders.getPublicOrderById(1L)).thenReturn(new PublicOrderVO(1L,BigDecimal.ONE,
                BigDecimal.ZERO,"NORMAL",null,null,30,4L,"配送","校园 楼栋","校园 楼栋"));
        var result = orderService.showByCategory(4L);
        assertEquals(1,result.size()); assertNull(result.get(0).getPhone());
        assertNull(result.get(0).getNote()); assertNull(result.get(0).getImage());
        verifyNoInteractions(media);
    }
    @Test void legacyPurchaseUrlIsRemovedWhenNoAuthorizedBindingExists() {
        BaseContext.setCurrentId(10L); privateSetup();
        TakeOrderUserInfoVO contact = new TakeOrderUserInfoVO(); contact.setId(2L);
        contact.setPurchaseProofImage("legacy-private-url");
        when(takes.getUserInfoByOrderId(1L,2L)).thenReturn(contact);
        assertNull(takeService.userInfo(1L).getPurchaseProofImage());
    }
    @Test void unfinishedDeliveryDoesNotIssuePictureUrl() {
        BaseContext.setCurrentId(10L);
        when(orders.getById(1L)).thenReturn(source());
        TakeOrder take = take(); take.setStatus(1);
        when(takes.getByOrderId(1L)).thenReturn(take);
        assertThrows(OrderException.class, () -> takeService.getImageByOrderId(1L));
        verifyNoInteractions(media);
    }
    @Test void removedOrderCannotAuthorizeContactOrEvidence() {
        BaseContext.setCurrentId(20L);
        assertThrows(OrderException.class, () -> takeService.userInfo(1L));
        assertThrows(OrderException.class, () -> takeService.getImageByOrderId(1L));
        verifyNoInteractions(takes,media);
    }
    @Test void cancelledFormerTakerListClearsPrivateFieldsAndOldPictures() {
        BaseContext.setCurrentId(20L);
        TakeOrderVO row = new TakeOrderVO(); row.setId(2L); row.setOrderId(1L);
        row.setPhone("private-phone"); row.setRealname("private-name"); row.setNote("private-note");
        row.setPickUpAddress("private-door"); row.setReciveAddress("private-door");
        row.setImage("old-url"); row.setTakeOrderImage("old-proof"); row.setPurchaseProofImage("old-receipt");
        TakeOrder cancelled = take(); cancelled.setStatus(3);
        when(takes.getMy(20L)).thenReturn(List.of(row));
        when(takes.getById(2L)).thenReturn(cancelled);
        when(orders.getById(1L)).thenReturn(source());
        var result = takeService.getMy().get(0);
        assertNull(result.getPhone()); assertNull(result.getRealname()); assertNull(result.getNote());
        assertNull(result.getPickUpAddress()); assertNull(result.getReciveAddress());
        assertNull(result.getImage()); assertNull(result.getTakeOrderImage()); assertNull(result.getPurchaseProofImage());
        verifyNoInteractions(media);
    }
    @Test void validTakerListSignsBoundEvidenceOnly() {
        BaseContext.setCurrentId(20L);
        TakeOrderVO row = new TakeOrderVO(); row.setId(2L); row.setOrderId(1L);
        when(takes.getMy(20L)).thenReturn(List.of(row));
        when(takes.getById(2L)).thenReturn(take());
        when(orders.getById(1L)).thenReturn(source());
        when(media.resolveAuthorizedBinding(MediaAssetConstant.BOUND_ORDER,1L,"ORDER_IMAGE"))
                .thenReturn(List.of());
        when(media.resolveAuthorizedBinding(MediaAssetConstant.BOUND_TAKE_ORDER,2L,"DELIVERY_PROOF"))
                .thenReturn(List.of(image()));
        when(media.resolveAuthorizedBinding(MediaAssetConstant.BOUND_TAKE_ORDER,2L,"PURCHASE_PROOF"))
                .thenReturn(List.of(image()));
        var result = takeService.getMy().get(0);
        assertEquals("signed-private-url",result.getTakeOrderImage());
        assertEquals("signed-private-url",result.getPurchaseProofImage());
    }

}
