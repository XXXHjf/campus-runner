package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.ParamException;
import com.mikasa.campusrunner.mapper.*;
import com.mikasa.campusrunner.pojo.dto.SecondHandOrderQueryDTO;
import com.mikasa.campusrunner.pojo.entity.SecondHandOrder;
import com.mikasa.campusrunner.pojo.vo.UserVO;
import com.mikasa.campusrunner.service.MediaAssetService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SecondHandOrderPaginationTest {
    @Test void normalizationMatchesExistingWhitespaceSearchAndLiteralWildcards() {
        var query = new SecondHandOrderQueryDTO(); query.setKeyword(" AB\tC　%_!");
        assertEquals("abc!%!_!!", query.getSearchKeyword());
        query.setKeyword(String.join("", query.getWhitespaceCharacters()) + "A");
        assertEquals("a", query.getSearchKeyword());
        assertEquals(20, query.getLimit()); assertEquals(0, query.getOffset());
        query.setGroup("unknown"); assertThrows(ParamException.class, query::getOffset);
        query.setGroup("all"); query.setKeyword("a".repeat(201)); assertThrows(ParamException.class, query::getOffset);
    }
    private org.apache.ibatis.session.Configuration configuration() throws Exception {
        var config = new org.apache.ibatis.session.Configuration();
        try (var xml = getClass().getResourceAsStream("/mapper/SecondHandOrderMapper.xml")) {
            new org.apache.ibatis.builder.xml.XMLMapperBuilder(xml, config, "mapper/SecondHandOrderMapper.xml", config.getSqlFragments()).parse();
        }
        return config;
    }
    @Test void actualSqlKeepsOwnerTradeModeGroupsNormalizedSearchAndPageBounds() throws Exception {
        var config = configuration(); var query = new SecondHandOrderQueryDTO();
        query.setGroup("refund"); query.setKeyword("book 100%"); query.setPage(2); query.getOffset();
        for (String method : List.of("listByBuyer", "listBySeller")) {
            var bound = config.getMappedStatement("com.mikasa.campusrunner.mapper.SecondHandOrderMapper." + method)
                    .getBoundSql(Map.of("query", query, "buyerId", 10L, "sellerId", 10L));
            String text = bound.getSql().replaceAll("\\s+", " ");
            assertTrue(text.contains(method.equals("listByBuyer") ? "o.buyer_id = ?" : "o.seller_id = ?"));
            assertTrue(text.contains("o.deleted = 0")); assertTrue(text.contains("= 'OFFLINE'"));
            assertTrue(text.contains("o.status in (1,2,11)")); assertTrue(text.contains("o.status in (5,6,7)"));
            assertTrue(text.contains("coalesce(" + (method.equals("listByBuyer") ? "seller" : "buyer") + ".username, '')"));
            assertTrue(text.contains("binary lower(")); assertTrue(text.contains("like binary concat"));
            assertTrue(text.contains("escape '!'")); assertTrue(text.contains("order by o.create_time desc, o.id desc limit ? offset ?"));
            var mappings = bound.getParameterMappings();
            assertEquals(25, mappings.stream().filter(item -> item.getProperty().startsWith("__frch_space_")).count());
            assertEquals("query.limit", mappings.get(mappings.size()-2).getProperty());
            assertEquals("query.offset", mappings.get(mappings.size()-1).getProperty());
        }
    }
    @Test void allAndEmptySearchDoNotHideUnknownStatusesAndTimeoutQueriesAreBounded() throws Exception {
        var config = configuration(); var query = new SecondHandOrderQueryDTO();
        String sql = config.getMappedStatement("com.mikasa.campusrunner.mapper.SecondHandOrderMapper.listByBuyer")
                .getBoundSql(Map.of("query",query,"buyerId",10L)).getSql();
        assertFalse(sql.contains("case when")); assertFalse(sql.contains("like binary"));
        String owner = config.getMappedStatement("com.mikasa.campusrunner.mapper.SecondHandOrderMapper.listUnpaidTimeoutForOwner")
                .getBoundSql(Map.of("time",LocalDateTime.now(),"ownerId",10L,"limit",20)).getSql();
        assertTrue(owner.contains("buyer_id = ? or seller_id = ?")); assertTrue(owner.contains("limit ?"));
        String global = config.getMappedStatement("com.mikasa.campusrunner.mapper.SecondHandOrderMapper.listUnpaidTimeoutBatch")
                .getBoundSql(Map.of("time",LocalDateTime.now(),"afterId",100L,"limit",100)).getSql();
        assertTrue(global.contains("id > ?")); assertTrue(global.contains("order by id asc")); assertTrue(global.contains("limit ?"));
    }
    @Test void listOnlyRefreshesCurrentAccountsTimeoutsBeforeReadingItsPage() {
        BaseContext.setCurrentId(10L);
        try {
            var users = mock(UserMapper.class); var orders = mock(SecondHandOrderMapper.class);
            var user = new UserVO(); user.setAuthentication(1); when(users.getById(10L)).thenReturn(user);
            var service = new SecondHandServiceImpl();
            ReflectionTestUtils.setField(service,"userMapper",users); ReflectionTestUtils.setField(service,"orderMapper",orders);
            ReflectionTestUtils.setField(service,"configMapper",mock(AdminSystemConfigMapper.class));
            ReflectionTestUtils.setField(service,"mediaAssetService",mock(MediaAssetService.class));
            var query = new SecondHandOrderQueryDTO(); query.setPage(3);
            service.listBuyerOrders(query); service.listSellerOrders(query);
            verify(orders,times(2)).listUnpaidTimeoutForOwner(any(),eq(10L),eq(20));
            verify(orders).listByBuyer(10L,query); verify(orders).listBySeller(10L,query);
            verify(orders,never()).listUnpaidTimeoutBatch(any(),anyLong(),anyInt());
            query.setGroup("invalid"); assertThrows(ParamException.class,()->service.listBuyerOrders(query));
            verifyNoMoreInteractions(orders);
        } finally { BaseContext.removeCurrentId(); }
    }
    @Test void globalTimeoutCursorAdvancesPastFullBatchEvenIfCandidatesNoLongerExist() {
        var orders = mock(SecondHandOrderMapper.class); var service = new SecondHandServiceImpl();
        ReflectionTestUtils.setField(service,"orderMapper",orders);
        ReflectionTestUtils.setField(service,"productMapper",mock(SecondHandProductMapper.class));
        ReflectionTestUtils.setField(service,"configMapper",mock(AdminSystemConfigMapper.class));
        var tx = mock(org.springframework.transaction.PlatformTransactionManager.class);
        when(tx.getTransaction(any())).thenReturn(new org.springframework.transaction.support.SimpleTransactionStatus());
        ReflectionTestUtils.setField(service,"transactionManager",tx);
        List<SecondHandOrder> first = java.util.stream.LongStream.rangeClosed(1,100).mapToObj(id->{var order=new SecondHandOrder();order.setId(id);return order;}).toList();
        when(orders.listUnpaidTimeoutBatch(any(),eq(0L),eq(100))).thenReturn(first);
        when(orders.listUnpaidTimeoutBatch(any(),eq(100L),eq(100))).thenReturn(List.of());
        ReflectionTestUtils.invokeMethod(service,"refreshUnpaidTimeouts");
        verify(orders).listUnpaidTimeoutBatch(any(),eq(0L),eq(100));
        verify(orders).listUnpaidTimeoutBatch(any(),eq(100L),eq(100));
        verify(orders,never()).listUnpaidTimeout(any());
    }
}
