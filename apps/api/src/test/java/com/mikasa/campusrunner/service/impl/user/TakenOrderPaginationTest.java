package com.mikasa.campusrunner.service.impl.user;
import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.*;
import com.mikasa.campusrunner.mapper.*;
import com.mikasa.campusrunner.pojo.dto.TakenOrderPageDTO;
import com.mikasa.campusrunner.pojo.vo.TakeOrderVO;
import com.mikasa.campusrunner.service.MediaAssetService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class TakenOrderPaginationTest {
    private org.apache.ibatis.session.Configuration configuration() throws Exception {
        var config=new org.apache.ibatis.session.Configuration();
        try(var xml=getClass().getResourceAsStream("/mapper/TakeOrderMapper.xml")) {
            new org.apache.ibatis.builder.xml.XMLMapperBuilder(xml,config,"mapper/TakeOrderMapper.xml",config.getSqlFragments()).parse();
        }
        return config;
    }
    @Test void actualSqlPreservesOwnerPrivacySearchAndStablePageBounds() throws Exception {
        var config=configuration();var q=new TakenOrderPageDTO();q.setKeyword(" AB %_");q.setPage(2);
        assertEquals(20,q.getOffset());assertEquals("ab!%!_",q.getSearchKeyword());
        String sql=config.getMappedStatement("com.mikasa.campusrunner.mapper.TakeOrderMapper.getMyPage")
            .getBoundSql(Map.of("id",20L,"query",q)).getSql().replaceAll("\\s+"," ");
        for(String part:List.of("take_order.user_id = ?","take_order.deleted = 0","orders.deleted = 0",
            "take_order.taker_id","take_order.take_status","IF(take_order.take_status IN (0,1,2),COALESCE(orders.note,''),'')",
            "take_order.take_status IN (0,1,2) AND LOCATE", "orders.order_number","DATE_FORMAT(take_order.take_order_create_time",
            "BINARY LOWER","ESCAPE '!'","take_order.take_order_create_time desc, take_order.id desc","LIMIT ? OFFSET ?")) assertTrue(sql.contains(part),part);
    }
    @Test void statusFiveGroupUsesReceivableWithoutNormalOrderGoods() throws Exception {
        var config=configuration();var q=new TakenOrderPageDTO();q.setGroup("completed");
        String sql=config.getMappedStatement("com.mikasa.campusrunner.mapper.TakeOrderMapper.getMyPage")
            .getBoundSql(Map.of("id",20L,"query",q)).getSql().replaceAll("\\s+"," ");
        assertTrue(sql.contains("orders.status = 6 OR (orders.status = 5"));
        assertTrue(sql.contains("CASE WHEN orders.business_type='PURCHASE' THEN COALESCE(orders.product_amount,0) ELSE 0 END"));
        assertTrue(sql.contains("<= 0"));q.setGroup("exception");
        sql=config.getMappedStatement("com.mikasa.campusrunner.mapper.TakeOrderMapper.getMyPage").getBoundSql(Map.of("id",20L,"query",q)).getSql();
        assertTrue(sql.contains("orders.status IS NULL OR orders.status NOT IN (1,2,3,4,5,6)"));
        q.setGroup("unknown");assertThrows(ParamException.class,q::getOffset);
        q.setGroup("all");q.setPageSize(51);assertThrows(ParamException.class,q::getOffset);
    }
    @Test void refusesForeignRowBeforeSigningAndDoesNotExposeAccessMetadata() throws Exception {
        var row=new TakeOrderVO();row.setId(1L);row.setOrderId(2L);row.setTakerId(21L);row.setTakeStatus(0);
        BaseContext.setCurrentId(20L);
        try {
            var mapper=mock(TakeOrderMapper.class);var media=mock(MediaAssetService.class);var service=new TakeOrderServiceImpl();
            ReflectionTestUtils.setField(service,"takeOrderMapper",mapper);ReflectionTestUtils.setField(service,"mediaAssetService",media);
            when(mapper.getMyPage(eq(20L),any())).thenReturn(List.of(row));
            assertThrows(OrderException.class,service::getMy);verifyNoInteractions(media);
            String json=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(row);
            assertFalse(json.contains("takerId"));assertFalse(json.contains("takeStatus"));
        } finally {BaseContext.removeCurrentId();}
    }
    @Test void batchesOnlyActiveTakesAndNeverLoadsPrivateRowsIndividually() {
        BaseContext.setCurrentId(20L);
        try {
            var mapper=mock(TakeOrderMapper.class);var orders=mock(OrderMapper.class);var media=mock(MediaAssetService.class);var service=new TakeOrderServiceImpl();
            ReflectionTestUtils.setField(service,"takeOrderMapper",mapper);ReflectionTestUtils.setField(service,"orderMapper",orders);ReflectionTestUtils.setField(service,"mediaAssetService",media);
            var active=new TakeOrderVO();active.setId(1L);active.setOrderId(11L);active.setUserId(10L);active.setTakerId(20L);active.setTakeStatus(0);
            var canceled=new TakeOrderVO();canceled.setId(2L);canceled.setOrderId(12L);canceled.setUserId(10L);canceled.setTakerId(20L);canceled.setTakeStatus(3);canceled.setNote("private");
            when(mapper.getMyPage(eq(20L),any())).thenReturn(List.of(active,canceled));
            when(media.resolveAuthorizedBindings(anyString(),anyList(),anyString(),anyInt())).thenReturn(Map.of());
            when(media.resolvePublicBindings(anyString(),anyList(),anyString(),anyInt())).thenReturn(Map.of());
            assertEquals(2,service.getMy().size());assertNull(canceled.getNote());
            verify(media).resolveAuthorizedBindings(eq("ORDER"),eq(List.of(11L)),eq("ORDER_IMAGE"),eq(320));
            verify(media).resolveAuthorizedBindings(eq("TAKE_ORDER"),eq(List.of(1L)),eq("DELIVERY_PROOF"),eq(320));
            verify(media).resolveAuthorizedBindings(eq("TAKE_ORDER"),eq(List.of(1L)),eq("PURCHASE_PROOF"),eq(320));
            verify(mapper,never()).getById(any());verifyNoInteractions(orders);
        } finally {BaseContext.removeCurrentId();}
    }
    @Test void conditionalAndNotWithdrawnSqlStayOwnedBoundedAndPreserveSelection() throws Exception {
        var config=configuration();var query=new com.mikasa.campusrunner.pojo.dto.TakeOrderQueryDTO();
        query.setStatus(2);query.setPage(3);assertEquals(40,query.getOffset());
        var conditional=config.getMappedStatement("com.mikasa.campusrunner.mapper.TakeOrderMapper.queryPage")
                .getBoundSql(Map.of("id",20L,"query",query));
        String sql=conditional.getSql().replaceAll("\\s+"," ");
        for(String text:List.of("take_order.user_id = ?","take_order.deleted = 0","orders.deleted = 0",
                "take_order.take_status = ?","take_order.taker_id","ORDER BY take_order.take_order_create_time desc, take_order.id desc LIMIT ? OFFSET ?")) assertTrue(sql.contains(text),text);
        var unpaid=config.getMappedStatement("com.mikasa.campusrunner.mapper.TakeOrderMapper.getNoWithdrawnPage")
                .getBoundSql(Map.of("userId",20L,"schoolId",1L,"orderStatus",5,"query",query));
        sql=unpaid.getSql().replaceAll("\\s+"," ");
        for(String text:List.of("user_id = ? AND STATUS = 2","school_id = ?","tb_orders.status = ?",
                "tb_orders.`price` IS NOT NULL","tb_orders.`price` != 0","tb_orders.deleted = 0",
                "tk.taker_id","tk.take_status","ORDER BY tk.create_time desc, tk.id desc LIMIT ? OFFSET ?")) assertTrue(sql.contains(text),text);
        assertEquals(2,unpaid.getParameterMappings().stream().filter(m->m.getProperty().equals("schoolId")).count());
    }
    @Test void otherTakenListsReuseWholePageAuthorizationAndBatching() {
        BaseContext.setCurrentId(20L);try {
            var mapper=mock(TakeOrderMapper.class);var users=mock(UserMapper.class);var media=mock(MediaAssetService.class);var orders=mock(OrderMapper.class);var service=new TakeOrderServiceImpl();
            ReflectionTestUtils.setField(service,"takeOrderMapper",mapper);ReflectionTestUtils.setField(service,"userMapper",users);
            ReflectionTestUtils.setField(service,"mediaAssetService",media);ReflectionTestUtils.setField(service,"orderMapper",orders);
            var user=new com.mikasa.campusrunner.pojo.vo.UserVO();user.setSchoolId(1L);when(users.getById(20L)).thenReturn(user);
            var row=new TakeOrderVO();row.setId(1L);row.setOrderId(11L);row.setTakerId(20L);row.setUserId(10L);row.setTakeStatus(2);
            when(mapper.queryPage(eq(20L),any())).thenReturn(List.of(row));
            when(mapper.getNoWithdrawnPage(eq(20L),eq(1L),eq(5),any())).thenReturn(List.of(row));
            when(media.resolveAuthorizedBindings(anyString(),anyList(),anyString(),anyInt())).thenReturn(Map.of());
            when(media.resolvePublicBindings(anyString(),anyList(),anyString(),anyInt())).thenReturn(Map.of());
            service.query(new com.mikasa.campusrunner.pojo.dto.TakeOrderQueryDTO());service.getNoWithdrawn();
            verify(media,times(2)).resolveAuthorizedBindings(eq("ORDER"),eq(List.of(11L)),eq("ORDER_IMAGE"),eq(320));
            verify(mapper,never()).getById(any());verifyNoInteractions(orders);
            row.setTakerId(99L);clearInvocations(media);
            assertThrows(OrderException.class,()->service.query(new com.mikasa.campusrunner.pojo.dto.TakeOrderQueryDTO()));verifyNoInteractions(media);
        }finally{BaseContext.removeCurrentId();}
    }
    @Test void invalidOrAnonymousQueryNeverReachesMappers() {
        var service=new TakeOrderServiceImpl();var mapper=mock(TakeOrderMapper.class);var users=mock(UserMapper.class);
        ReflectionTestUtils.setField(service,"takeOrderMapper",mapper);ReflectionTestUtils.setField(service,"userMapper",users);
        var query=new com.mikasa.campusrunner.pojo.dto.TakeOrderQueryDTO();query.setStatus(4);
        assertThrows(ParamException.class,()->service.query(query));query.setStatus(null);query.setPageSize(51);
        assertThrows(ParamException.class,()->service.query(query));
        assertThrows(OrderException.class,service::getNoWithdrawn);verifyNoInteractions(mapper,users);
    }

}
