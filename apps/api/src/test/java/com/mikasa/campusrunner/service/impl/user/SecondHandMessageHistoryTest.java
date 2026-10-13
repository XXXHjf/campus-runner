package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.ParamException;
import com.mikasa.campusrunner.common.exception.SecondHandException;
import com.mikasa.campusrunner.mapper.*;
import com.mikasa.campusrunner.pojo.dto.MessagePageDTO;
import com.mikasa.campusrunner.pojo.entity.SecondHandProduct;
import com.mikasa.campusrunner.pojo.vo.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class SecondHandMessageHistoryTest {
    private final UserMapper users = mock(UserMapper.class);
    private final SecondHandMessageMapper messages = mock(SecondHandMessageMapper.class);
    private final SecondHandProductMapper products = mock(SecondHandProductMapper.class);
    private final SecondHandServiceImpl service = new SecondHandServiceImpl();

    @BeforeEach void setup() {
        BaseContext.setCurrentId(10L);
        ReflectionTestUtils.setField(service, "userMapper", users);
        ReflectionTestUtils.setField(service, "messageMapper", messages);
        ReflectionTestUtils.setField(service, "productMapper", products);
        var user = new UserVO(); user.setAuthentication(1);
        when(users.getById(10L)).thenReturn(user);
        when(products.getById(30L)).thenReturn(SecondHandProduct.builder().id(30L).sellerId(20L).build());
    }
    @AfterEach void cleanup() { BaseContext.removeCurrentId(); }

    private SecondHandMessageVO row(long id, long sender, long receiver) {
        var row = new SecondHandMessageVO(); row.setId(id); row.setProductId(30L);
        row.setSenderId(sender); row.setReceiverId(receiver);
        row.setCreateTime(LocalDateTime.of(2026, 10, 11, 12, 0)); return row;
    }

    @Test void recentPageIsChronologicalAndOnlyFetchedIncomingMessagesBecomeRead() {
        var query = new MessagePageDTO(); var newest = row(22, 10, 20); var older = row(21, 20, 10);
        when(messages.listMessagePage(30L, 10L, 20L, query, null)).thenReturn(List.of(newest, older));
        var result = service.listConversationMessages(30L, 20L, query);
        assertEquals(List.of(21L, 22L), result.stream().map(SecondHandMessageVO::getId).toList());
        verify(messages).markMessagesRead(10L, List.of(21L));
        assertNotNull(result.get(0).getReadTime()); assertNull(result.get(1).getReadTime());
        verify(messages, never()).markConversationRead(any(), any(), any());
        verify(messages, never()).listConversation(any(), any(), any());
    }

    @Test void earlierPagesAcceptDeletedAnchorAndForwardPagesRetainAscendingOrder() {
        var anchor = row(21, 20, 10); when(messages.getCursorForUser(21L, 10L)).thenReturn(anchor);
        var before = new MessagePageDTO(); before.setBeforeId(21L);
        when(messages.listMessagePage(30L, 10L, 20L, before, anchor)).thenReturn(List.of(row(20,10,20), row(19,10,20)));
        assertEquals(List.of(19L,20L), service.listConversationMessages(30L,20L,before).stream().map(SecondHandMessageVO::getId).toList());
        var after = new MessagePageDTO(); after.setAfterId(21L);
        when(messages.listMessagePage(30L,10L,20L,after,anchor)).thenReturn(List.of(row(22,10,20),row(23,10,20)));
        assertEquals(List.of(22L,23L), service.listConversationMessages(30L,20L,after).stream().map(SecondHandMessageVO::getId).toList());
    }

    @Test void foreignCursorOrPageRowsCannotBeReadOrMarked() {
        var query = new MessagePageDTO(); query.setBeforeId(21L);
        when(messages.getCursorForUser(21L,10L)).thenReturn(row(21,10,99));
        assertThrows(ParamException.class, () -> service.listConversationMessages(30L,20L,query));
        verify(messages, never()).listMessagePage(any(),any(),any(),any(),any());
        query.setBeforeId(null);
        when(messages.listMessagePage(30L,10L,20L,query,null)).thenReturn(List.of(row(1,20,99)));
        assertThrows(SecondHandException.class, () -> service.listConversationMessages(30L,20L,query));
        verify(messages, never()).markMessagesRead(any(),any());
    }

    @Test void invalidPageFailsBeforeAuthenticationAndLegacyProductHistoryNeverMarksRead() {
        clearInvocations(users,products);
        var query = new MessagePageDTO(); query.setPageSize(51);
        assertThrows(ParamException.class, () -> service.listProductMessages(30L,query));
        verifyNoInteractions(users,products,messages);
        query.setPageSize(20); query.setBeforeId(1L); query.setAfterId(2L);
        assertThrows(ParamException.class,query::getLimit);
        query.setBeforeId(null); query.setAfterId(null);
        when(messages.listMessagePage(30L,10L,null,query,null)).thenReturn(List.of(row(1,20,10)));
        assertEquals(1,service.listProductMessages(30L,query).size());
        verify(messages, never()).markMessagesRead(any(),any()); verifyNoInteractions(products);
    }

    @Test void actualSqlPreservesOwnerPairDeletionTimeTieNullTimeAndBoundedReadUpdate() throws Exception {
        var config = new org.apache.ibatis.session.Configuration();
        try(var xml=getClass().getResourceAsStream("/mapper/SecondHandMessageMapper.xml")) {
            new org.apache.ibatis.builder.xml.XMLMapperBuilder(xml,config,"mapper/SecondHandMessageMapper.xml",config.getSqlFragments()).parse();
        }
        var query = new MessagePageDTO(); query.setBeforeId(21L);
        var params = new HashMap<String,Object>(); params.put("productId",30L); params.put("userId",10L);
        params.put("counterpartyId",20L); params.put("query",query); params.put("cursor",row(21,20,10));
        String statement="com.mikasa.campusrunner.mapper.SecondHandMessageMapper.";
        var sql=config.getMappedStatement(statement+"listMessagePage").getBoundSql(params).getSql().replaceAll("\\s+"," ");
        for(var part:List.of("m.deleted = 0 and m.product_id = ?","m.sender_id = ? or m.receiver_id = ?",
                "m.create_time < ? or m.create_time is null", "m.create_time = ? and m.id < ?", "order by m.create_time desc, m.id desc", "limit ?")) assertTrue(sql.contains(part),part);
        ((SecondHandMessageVO)params.get("cursor")).setCreateTime(null);
        sql=config.getMappedStatement(statement+"listMessagePage").getBoundSql(params).getSql().replaceAll("\\s+"," ");
        assertTrue(sql.contains("m.create_time is null and m.id < ?"));
        query.setBeforeId(null);query.setAfterId(21L);
        sql=config.getMappedStatement(statement+"listMessagePage").getBoundSql(params).getSql().replaceAll("\\s+"," ");
        assertTrue(sql.contains("m.create_time is not null or")); assertTrue(sql.contains("order by m.create_time asc, m.id asc"));
        var update=config.getMappedStatement(statement+"markMessagesRead").getBoundSql(Map.of("userId",10L,"ids",List.of(21L,22L))).getSql().replaceAll("\\s+"," ");
        assertTrue(update.contains("receiver_id = ? and read_time is null"));assertTrue(update.contains("id in"));
        var anchor=config.getMappedStatement(statement+"getCursorForUser").getBoundSql(Map.of("userId",10L,"id",21L)).getSql();
        assertFalse(anchor.contains("content"));assertFalse(anchor.contains("deleted = 0"));
    }

    @Test void controllerBindsCursorsForBothEndpoints() throws Exception {
        var api=mock(com.mikasa.campusrunner.service.user.SecondHandService.class);
        var controller=new com.mikasa.campusrunner.controller.user.SecondHandController();
        ReflectionTestUtils.setField(controller,"secondHandService",api);
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/second-hand/conversations/30/20/messages").param("beforeId","21").param("pageSize","50"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        verify(api).listConversationMessages(eq(30L),eq(20L),argThat(q -> q.getBeforeId()==21L && q.getLimit()==50));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/second-hand/products/30/messages").param("afterId","21"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        verify(api).listProductMessages(eq(30L),argThat(q -> q.getAfterId()==21L));
    }
}
