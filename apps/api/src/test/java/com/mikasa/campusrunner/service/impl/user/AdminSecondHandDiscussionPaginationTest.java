package com.mikasa.campusrunner.service.impl.user;
import com.mikasa.campusrunner.mapper.*;
import com.mikasa.campusrunner.pojo.dto.*;
import com.mikasa.campusrunner.pojo.vo.*;
import com.mikasa.campusrunner.service.user.SecondHandService;
import com.mikasa.campusrunner.controller.admin.AdminSecondHandController;
import com.mikasa.campusrunner.common.exception.ParamException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class AdminSecondHandDiscussionPaginationTest {
 private AdminSecondHandBargainStatsVO stats(){var stats=new AdminSecondHandBargainStatsVO();stats.setTotal(125);stats.setPendingBargains(75);return stats;}
 @Test void pagedDiscussionListsKeepFullCountsAndExistingOrderLinksWithoutLegacyReads() {
  var bargains=mock(SecondHandBargainMapper.class);var messages=mock(SecondHandMessageMapper.class);var service=new SecondHandServiceImpl();ReflectionTestUtils.setField(service,"bargainMapper",bargains);ReflectionTestUtils.setField(service,"messageMapper",messages);
  var query=new ListPageDTO();query.setPage(2);query.setPageSize(5);var a=new SecondHandBargainVO();a.setId(21L);a.setOrderId(11L);var b=new SecondHandBargainVO();b.setId(21L);b.setOrderId(12L);
  when(bargains.listAdminPage(query)).thenReturn(List.of(a,b));when(bargains.summarizeAdmin()).thenReturn(stats());var result=service.adminListBargains(query);assertEquals(125,result.getTotal());assertEquals(75,result.getPendingBargains());assertEquals(2,result.getPage());assertEquals(5,result.getPageSize());assertEquals(List.of(11L,12L),result.getList().stream().map(SecondHandBargainVO::getOrderId).toList());verify(bargains,never()).listAdmin();
  var messageQuery=new AdminSecondHandMessageQueryDTO();messageQuery.setPage(3);messageQuery.setProductId(7L);var row=new SecondHandMessageVO();row.setId(41L);when(messages.listAdminPage(messageQuery)).thenReturn(List.of(row));when(messages.countAdmin(messageQuery)).thenReturn(137L);
  var page=service.adminListMessages(messageQuery);assertEquals(137,page.getTotal());assertEquals(3,page.getPage());assertEquals(20,page.getPageSize());assertEquals(41L,page.getList().get(0).getId());verify(messages,never()).listAdmin(any());
 }
 @Test void invalidPaginationOrMessageProductNeverReachesQueries() {
  var bargains=mock(SecondHandBargainMapper.class);var messages=mock(SecondHandMessageMapper.class);var service=new SecondHandServiceImpl();ReflectionTestUtils.setField(service,"bargainMapper",bargains);ReflectionTestUtils.setField(service,"messageMapper",messages);var query=new ListPageDTO();query.setPage(0);assertThrows(ParamException.class,()->service.adminListBargains(query));query.setPage(1);query.setPageSize(51);assertThrows(ParamException.class,()->service.adminListBargains(query));
  var messageQuery=new AdminSecondHandMessageQueryDTO();messageQuery.setProductId(0L);assertThrows(ParamException.class,()->service.adminListMessages(messageQuery));messageQuery.setProductId(null);messageQuery.setPage(-1);assertThrows(ParamException.class,()->service.adminListMessages(messageQuery));verifyNoInteractions(bargains,messages);
 }
 private org.apache.ibatis.session.Configuration mapper(String name) throws Exception {var config=new org.apache.ibatis.session.Configuration();try(var xml=getClass().getResourceAsStream("/mapper/"+name+".xml")){new org.apache.ibatis.builder.xml.XMLMapperBuilder(xml,config,name,config.getSqlFragments()).parse();}return config;}
 private String sql(org.apache.ibatis.session.Configuration config,String name,String method,Object params){return config.getMappedStatement("com.mikasa.campusrunner.mapper."+name+"."+method).getBoundSql(params).getSql().replaceAll("\\s+"," ");}
 @Test void generatedSqlPreservesJoinedBargainRowsAndFiltersMessagesBeforeStablePagination() throws Exception {
  var bargains=mapper("SecondHandBargainMapper");var query=new ListPageDTO();query.setPage(2);var page=sql(bargains,"SecondHandBargainMapper","listAdminPage",Map.of("query",query));var summary=sql(bargains,"SecondHandBargainMapper","summarizeAdmin",Map.of());
  for(var text:List.of(page,summary)){assertTrue(text.contains("o.bargain_id = b.id and o.deleted = 0"));assertTrue(text.contains("where b.deleted = 0"));}assertTrue(page.contains("order by b.create_time desc, b.id desc, o.id desc limit ? offset ?"));assertTrue(summary.contains("case when b.status = 0"));assertFalse(summary.contains("limit"));assertTrue(sql(bargains,"SecondHandBargainMapper","listAdmin",Map.of()).strip().endsWith("limit 20"));
  var messages=mapper("SecondHandMessageMapper");var mq=new AdminSecondHandMessageQueryDTO();mq.setPage(2);mq.setProductId(7L);var messagePage=sql(messages,"SecondHandMessageMapper","listAdminPage",Map.of("query",mq));var count=sql(messages,"SecondHandMessageMapper","countAdmin",Map.of("query",mq));
  for(var text:List.of(messagePage,count)){assertTrue(text.contains("m.deleted = 0"));assertTrue(text.contains("m.product_id = ?"));}assertTrue(messagePage.contains("order by m.create_time desc, m.id desc limit ? offset ?"));assertFalse(count.contains("limit"));assertTrue(sql(messages,"SecondHandMessageMapper","listAdmin",Map.of("productId",7L)).strip().endsWith("limit 20"));
 }
 @Test void controllersBindPaginationAndReturnFullCounts() throws Exception {
  var service=mock(SecondHandService.class);when(service.adminListBargains(any())).thenReturn(new AdminSecondHandBargainPageVO(stats(),2,5,List.of()));when(service.adminListMessages(any())).thenReturn(new PageResult<>(137,3,5,List.of()));var controller=new AdminSecondHandController();ReflectionTestUtils.setField(controller,"secondHandService",service);var mvc=MockMvcBuilders.standaloneSetup(controller).build();
  mvc.perform(get("/admin/api/second-hand/bargains").param("page","2").param("pageSize","5")).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(125)).andExpect(jsonPath("$.data.pendingBargains").value(75));
  mvc.perform(get("/admin/api/second-hand/messages").param("page","3").param("pageSize","5").param("productId","7")).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(137)).andExpect(jsonPath("$.data.list").isArray());verify(service).adminListBargains(argThat(q->q.getPage()==2&&q.getPageSize()==5));verify(service).adminListMessages(argThat(q->q.getPage()==3&&q.getPageSize()==5&&q.getProductId()==7L));
 }
}
