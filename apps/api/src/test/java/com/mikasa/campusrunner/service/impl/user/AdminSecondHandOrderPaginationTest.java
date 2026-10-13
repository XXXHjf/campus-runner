package com.mikasa.campusrunner.service.impl.user;
import com.mikasa.campusrunner.mapper.*;
import com.mikasa.campusrunner.pojo.dto.AdminSecondHandOrderQueryDTO;
import com.mikasa.campusrunner.pojo.vo.*;
import com.mikasa.campusrunner.service.MediaAssetService;
import com.mikasa.campusrunner.service.user.SecondHandService;
import com.mikasa.campusrunner.controller.admin.AdminSecondHandController;
import com.mikasa.campusrunner.common.exception.ParamException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class AdminSecondHandOrderPaginationTest {
 private AdminSecondHandOrderStatsVO stats(){var stats=new AdminSecondHandOrderStatsVO();stats.setTotal(125);stats.setAbnormalOrders(30);stats.setTurnover(new BigDecimal("1234.56"));return stats;}
 private SecondHandOrderVO row(long id,long product,String mode){var row=new SecondHandOrderVO();row.setId(id);row.setProductId(product);row.setTradeMode(mode);row.setStatus(0);row.setCreateTime(LocalDateTime.now());row.setProductImages("legacy");return row;}
 @Test void pagedOrdersKeepFullFilteredStatsBatchMediaAndOneTimeoutLookup() {
  var orders=mock(SecondHandOrderMapper.class);var media=mock(MediaAssetService.class);var configs=mock(AdminSystemConfigMapper.class);var service=new SecondHandServiceImpl();ReflectionTestUtils.setField(service,"orderMapper",orders);ReflectionTestUtils.setField(service,"mediaAssetService",media);ReflectionTestUtils.setField(service,"configMapper",configs);
  var query=new AdminSecondHandOrderQueryDTO();query.setPage(2);var a=row(21,30,"ONLINE");var b=row(22,30,"ONLINE");var c=row(23,40,"OFFLINE");
  when(orders.listAdminPage(query)).thenReturn(List.of(a,b,c));when(orders.summarizeAdmin(query)).thenReturn(stats());when(media.resolvePublicBindings("SECOND_HAND_PRODUCT",List.of(30L,40L),"SECOND_HAND_PRODUCT_IMAGE",0)).thenReturn(Map.of(30L,List.of(new BoundMediaVO(1L,"fresh",0))));
  var result=service.adminListOrders(query);assertEquals(2,result.getPage());assertEquals(20,result.getPageSize());assertEquals(125,result.getTotal());assertEquals(30,result.getAbnormalOrders());assertEquals(new BigDecimal("1234.56"),result.getTurnover());assertEquals("fresh",result.getList().get(0).getProductImages());assertEquals("",result.getList().get(2).getProductImages());assertNotNull(a.getPayDeadline());assertNull(c.getPayDeadline());
  verify(configs).getByConfigKey("second_hand_payment_timeout_minutes");verifyNoMoreInteractions(configs);verify(orders,never()).listAdmin(any());verify(media).resolvePublicBindings("SECOND_HAND_PRODUCT",List.of(30L,40L),"SECOND_HAND_PRODUCT_IMAGE",0);verifyNoMoreInteractions(media);
 }
 @Test void invalidStatusAndPaginationFailBeforeQueries() {
  var orders=mock(SecondHandOrderMapper.class);var service=new SecondHandServiceImpl();ReflectionTestUtils.setField(service,"orderMapper",orders);var query=new AdminSecondHandOrderQueryDTO();query.setStatus(-1);assertThrows(ParamException.class,()->service.adminListOrders(query));query.setStatus(12);assertThrows(ParamException.class,()->service.adminListOrders(query));query.setStatus(11);query.setPage(0);assertThrows(ParamException.class,()->service.adminListOrders(query));query.setPage(1);query.setPageSize(51);assertThrows(ParamException.class,()->service.adminListOrders(query));verifyNoInteractions(orders);
 }
 @Test void generatedSqlSharesStatusAndDeletionFiltersAndPreservesExistingAggregateSemantics() throws Exception {
  var config=new org.apache.ibatis.session.Configuration();try(var xml=getClass().getResourceAsStream("/mapper/SecondHandOrderMapper.xml")){new org.apache.ibatis.builder.xml.XMLMapperBuilder(xml,config,"mapper/SecondHandOrderMapper.xml",config.getSqlFragments()).parse();}
  var query=new AdminSecondHandOrderQueryDTO();query.setStatus(10);query.setPage(3);var params=Map.of("query",query);var page=config.getMappedStatement("com.mikasa.campusrunner.mapper.SecondHandOrderMapper.listAdminPage").getBoundSql(params).getSql().replaceAll("\\s+"," ");var summary=config.getMappedStatement("com.mikasa.campusrunner.mapper.SecondHandOrderMapper.summarizeAdmin").getBoundSql(params).getSql().replaceAll("\\s+"," ");
  for(var sql:List.of(page,summary)){assertTrue(sql.contains("o.deleted = 0"));assertTrue(sql.contains("o.status = ?"));}assertTrue(page.contains("order by o.create_time desc, o.id desc limit ? offset ?"));assertTrue(summary.contains("o.status in (7,10,11)"));assertTrue(summary.contains("o.status in (3,8,9,10)"));assertTrue(summary.contains("coalesce(o.pay_amount,0)"));assertFalse(summary.contains("limit"));assertFalse(summary.contains("order by"));assertFalse(summary.contains("trade_mode ="));
  assertTrue(config.getMappedStatement("com.mikasa.campusrunner.mapper.SecondHandOrderMapper.listAdmin").getBoundSql(Map.of("status",10)).getSql().strip().endsWith("limit 20"));
 }
 @Test void controllerBindsPageStatusAndReturnsPaginatedStats() throws Exception {
  var service=mock(SecondHandService.class);when(service.adminListOrders(any())).thenReturn(new AdminSecondHandOrderPageVO(stats(),2,5,List.of()));var controller=new AdminSecondHandController();ReflectionTestUtils.setField(controller,"secondHandService",service);var mvc=MockMvcBuilders.standaloneSetup(controller).build();
  mvc.perform(get("/admin/api/second-hand/orders").param("page","2").param("pageSize","5").param("status","10")).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(125)).andExpect(jsonPath("$.data.abnormalOrders").value(30)).andExpect(jsonPath("$.data.turnover").value(1234.56)).andExpect(jsonPath("$.data.list").isArray());
  verify(service).adminListOrders(argThat(q->q.getPage()==2&&q.getPageSize()==5&&q.getStatus()==10));
 }
}
