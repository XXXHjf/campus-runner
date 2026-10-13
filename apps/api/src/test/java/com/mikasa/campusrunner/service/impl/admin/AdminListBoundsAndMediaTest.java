package com.mikasa.campusrunner.service.impl.admin;

import com.mikasa.campusrunner.common.exception.ParamException;
import com.mikasa.campusrunner.mapper.*;
import com.mikasa.campusrunner.pojo.dto.ListPageDTO;
import com.mikasa.campusrunner.pojo.entity.*;
import com.mikasa.campusrunner.pojo.vo.*;
import com.mikasa.campusrunner.pojo.vo.admin.*;
import com.mikasa.campusrunner.service.MediaAssetService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AdminListBoundsAndMediaTest {
    @Test void usersAndTakeOrdersResolveOneBatchPerPageAndKeepFullTotals() {
        var media=mock(MediaAssetService.class); var users=mock(UserMapper.class);
        var userService=new AdminUserServiceImpl();
        ReflectionTestUtils.setField(userService,"userMapper",users); ReflectionTestUtils.setField(userService,"mediaAssetService",media);
        var a=new AdminUserListVO(); a.setId(10L); var b=new AdminUserListVO(); b.setId(20L);
        when(users.getAllUsers(20,20,"name")).thenReturn(List.of(a,b));when(users.countAdminUsers(null,null,"name")).thenReturn(500L);
        when(media.resolvePublicBindings("USER_AVATAR",List.of(10L,20L),"AVATAR",0)).thenReturn(Map.of(10L,List.of(new BoundMediaVO(1L,"avatar",0))));
        var result=userService.listAll(2,20," name ");assertEquals(500L,result.getTotal());assertEquals("avatar",a.getHeadImg());assertNull(b.getHeadImg());
        verify(media).resolvePublicBindings("USER_AVATAR",List.of(10L,20L),"AVATAR",0); verifyNoMoreInteractions(media);
        reset(media); var takes=mock(TakeOrderMapper.class);var takeService=new AdminTakeOrderServiceImpl();
        ReflectionTestUtils.setField(takeService,"takeOrderMapper",takes);ReflectionTestUtils.setField(takeService,"mediaAssetService",media);
        var take=new AdminTakeOrderListVO();take.setId(30L);
        when(takes.listUnpaidTakeOrders(20,20,null)).thenReturn(List.of(take));when(takes.countAdminTakeOrders(true,null)).thenReturn(800L);
        when(media.resolveAuthorizedBindings("TAKE_ORDER",List.of(30L),"DELIVERY_PROOF",0)).thenReturn(Map.of(30L,List.of(new BoundMediaVO(2L,"private-proof",0))));
        assertEquals(800L,takeService.listUnpaid(2,20,null).getTotal());assertEquals("private-proof",take.getTakeOrderImage());
        verify(media).resolveAuthorizedBindings("TAKE_ORDER",List.of(30L),"DELIVERY_PROOF",0);verifyNoMoreInteractions(media);
    }

    @Test void malformedAndOverflowingAdminPagesFailBeforeAnyQuery() {
        var users=mock(UserMapper.class);var orders=mock(OrderMapper.class);var takes=mock(TakeOrderMapper.class);var media=mock(MediaAssetService.class);
        var userService=new AdminUserServiceImpl();var orderService=new AdminOrderServiceImpl();var takeService=new AdminTakeOrderServiceImpl();
        ReflectionTestUtils.setField(userService,"userMapper",users);ReflectionTestUtils.setField(orderService,"orderMapper",orders);ReflectionTestUtils.setField(takeService,"takeOrderMapper",takes);
        for (int[] page : List.of(new int[]{0,20},new int[]{1,51},new int[]{1,0},new int[]{Integer.MAX_VALUE,50})) {
            assertThrows(ParamException.class,()->userService.listAll(page[0],page[1],null));
            assertThrows(ParamException.class,()->userService.listAuthenticated(page[0],page[1],null));
            assertThrows(ParamException.class,()->userService.listPendingReview(page[0],page[1],null));
            assertThrows(ParamException.class,()->orderService.listAll(page[0],page[1],null));
            assertThrows(ParamException.class,()->orderService.listWaiting(page[0],page[1],null));
            assertThrows(ParamException.class,()->orderService.listInProgress(page[0],page[1],null));
            assertThrows(ParamException.class,()->orderService.listCompleted(page[0],page[1],null));
            assertThrows(ParamException.class,()->orderService.listCanceled(page[0],page[1],null));
            assertThrows(ParamException.class,()->takeService.listAll(page[0],page[1],null));
            assertThrows(ParamException.class,()->takeService.listUnpaid(page[0],page[1],null));
        }
        assertEquals(20,ListPageDTO.checkedOffset(2,20));verifyNoInteractions(users,orders,takes,media);
    }

    @Test void oldPendingApiBindsPagingAndSqlCapsOldQueryWithoutLosingLaterPages() throws Exception {
        var service=mock(com.mikasa.campusrunner.service.admin.AdminAuthService.class);
        var controller=new com.mikasa.campusrunner.controller.admin.AdminAuthController();ReflectionTestUtils.setField(controller,"adminAuthService",service);
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/admin/api/auth/pendingList").param("page","2").param("pageSize","50"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        verify(service).getPendingList(argThat(q->q.getOffset()==50 && q.getLimit()==50));
        var config=new org.apache.ibatis.session.Configuration();
        for(var name:List.of("UserMapper","OrderMapper","TakeOrderMapper")) {
            try(var xml=getClass().getResourceAsStream("/mapper/"+name+".xml")) { new org.apache.ibatis.builder.xml.XMLMapperBuilder(xml,config,"mapper/"+name+".xml",config.getSqlFragments()).parse(); }
        }
        String prefix="com.mikasa.campusrunner.mapper.UserMapper.";
        var query=new ListPageDTO();query.setPage(2);
        var page=config.getMappedStatement(prefix+"getPendingAuthPage").getBoundSql(Map.of("query",query)).getSql().replaceAll("\\s+"," ");
        assertTrue(page.contains("u.deleted = 0 and u.student_id_card_review = 1"));assertTrue(page.contains("order by u.create_time desc, u.id desc limit ? offset ?"));
        assertTrue(config.getMappedStatement(prefix+"getPendingAuthList").getBoundSql(null).getSql().contains("limit 20"));
        var params=new HashMap<String,Object>();params.put("offset",20);params.put("limit",20);params.put("keyword",null);params.put("statuses",List.of(0));
        for(var entry:Map.of("UserMapper.getAllUsers","u", "UserMapper.getAuthenticatedUsers","u", "UserMapper.getPendingReviewUsers","u", "OrderMapper.listAllOrders","o", "OrderMapper.listOrdersByStatus","o", "TakeOrderMapper.listAllTakeOrders","tk", "TakeOrderMapper.listUnpaidTakeOrders","tk").entrySet()) {
            var sql=config.getMappedStatement("com.mikasa.campusrunner.mapper."+entry.getKey()).getBoundSql(params).getSql().replaceAll("\\s+"," ");
            assertTrue(sql.contains("ORDER BY "+entry.getValue()+".create_time DESC, "+entry.getValue()+".id DESC"),entry.getKey());
        }
    }

    @Test void categoryAndBannerListsResolveBatchesAndKeepCompleteReferenceSets() {
        var media=mock(MediaAssetService.class);var categories=mock(CategoryMapper.class);
        var service=new com.mikasa.campusrunner.service.impl.user.CategoryServiceImpl();
        ReflectionTestUtils.setField(service,"categoryMapper",categories);ReflectionTestUtils.setField(service,"mediaAssetService",media);
        var category=new Category();category.setId(10L);when(categories.getEnabled()).thenReturn(List.of(category));
        when(media.resolvePublicBindings("ORDER_CATEGORY",List.of(10L),"ORDER_CATEGORY_ICON",0)).thenReturn(Map.of(10L,List.of(new BoundMediaVO(1L,"icon",0))));
        assertEquals("icon",service.getAll().get(0).getImage());verify(categories).getEnabled();verifyNoMoreInteractions(categories);verify(media).resolvePublicBindings("ORDER_CATEGORY",List.of(10L),"ORDER_CATEGORY_ICON",0);
        reset(media);
        var adminCategories=new AdminCategoryServiceImpl();
        ReflectionTestUtils.setField(adminCategories,"categoryMapper",categories);ReflectionTestUtils.setField(adminCategories,"mediaAssetService",media);
        when(categories.getAll()).thenReturn(List.of(category));
        when(media.resolvePublicBindings("ORDER_CATEGORY",List.of(10L),"ORDER_CATEGORY_ICON",0)).thenReturn(Map.of(10L,List.of(new BoundMediaVO(3L,"admin-icon",0))));
        assertEquals("admin-icon",adminCategories.list().get(0).getImage());verify(media).resolvePublicBindings("ORDER_CATEGORY",List.of(10L),"ORDER_CATEGORY_ICON",0);verifyNoMoreInteractions(media);
        reset(media);
        var secondCategories=mock(SecondHandCategoryMapper.class);var secondService=new com.mikasa.campusrunner.service.impl.user.SecondHandServiceImpl();
        ReflectionTestUtils.setField(secondService,"categoryMapper",secondCategories);ReflectionTestUtils.setField(secondService,"mediaAssetService",media);
        var secondCategory=new SecondHandCategory();secondCategory.setId(40L);when(secondCategories.list()).thenReturn(List.of(secondCategory));
        when(media.resolvePublicBindings("SECOND_HAND_CATEGORY",List.of(40L),"SECOND_HAND_CATEGORY_ICON",0)).thenReturn(Map.of(40L,List.of(new BoundMediaVO(4L,"second-icon",0))));
        assertEquals("second-icon",secondService.listCategories().get(0).getImage());verify(media).resolvePublicBindings("SECOND_HAND_CATEGORY",List.of(40L),"SECOND_HAND_CATEGORY_ICON",0);verifyNoMoreInteractions(media);
        reset(media);var banners=mock(BannerMapper.class);var bannerService=new AdminBannerServiceImpl();
        ReflectionTestUtils.setField(bannerService,"bannerMapper",banners);ReflectionTestUtils.setField(bannerService,"mediaAssetService",media);
        var banner=new Banner();banner.setId(30L);when(banners.getListBySchoolId(0L)).thenReturn(List.of(banner));
        when(media.resolvePublicBindings("BANNER",List.of(30L),"BANNER",0)).thenReturn(Map.of(30L,List.of(new BoundMediaVO(2L,"banner",0))));
        assertEquals("banner",bannerService.getListBySchoolId(0L).get(0).getImgUrl());verify(media).resolvePublicBindings("BANNER",List.of(30L),"BANNER",0);verifyNoMoreInteractions(media);
    }
}
