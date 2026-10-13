package com.mikasa.campusrunner.service.impl.user;
import com.mikasa.campusrunner.controller.user.OrderController;
import com.mikasa.campusrunner.service.user.OrderService;
import com.mikasa.campusrunner.pojo.dto.*;
import com.mikasa.campusrunner.pojo.vo.OrderShowVO;
import com.mikasa.campusrunner.pojo.entity.AddressBook;
import com.mikasa.campusrunner.common.exception.ParamException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
class LegacyRunnerFeedPaginationTest {
 static class CaptureService extends OrderServiceImpl {
  RunnerFeedDTO query;
  @Override public List<OrderShowVO> listRunnerFeed(RunnerFeedDTO value){value.getOffset();query=value;return List.of();}
 }
 private ListPageDTO page(){var page=new ListPageDTO();page.setPage(3);page.setPageSize(7);return page;}
 @Test void everyLegacyServiceUsesTheSharedFeedAndKeepsOriginalDirectionsAndAddresses() {
  var service=new CaptureService();service.showByPrice(0,page());assertEquals("price_desc",service.query.getSort());assertEquals(14,service.query.getOffset());
  service.showByPrice(1,page());assertEquals("price_asc",service.query.getSort());
  service.showByTime(0,page());assertEquals("time",service.query.getSort());service.showByTime(1,page());assertEquals("time_asc",service.query.getSort());
  service.showByCategory(4L,page());assertEquals(4L,service.query.getCategoryId());assertEquals(3,service.query.getPage());
  var one=OrderShowByAddressDTO.builder().schoolNumberId(999L).compusNumberId(11L).buildCategoryNumberId(12L).buildingNumberId(13L).build();one.setPage(3);one.setPageSize(7);
  service.showByPickUpAdd(one);assertEquals(13L,service.query.getPickBuildingNumberId());assertNull(service.query.getReceiveBuildingNumberId());assertEquals(14,service.query.getOffset());
  service.showByReciveAdd(one);assertEquals(13L,service.query.getReceiveBuildingNumberId());assertNull(service.query.getPickBuildingNumberId());
  var both=new OrderShowByDoubleAddDTO();both.setPickSchoolNumberId(999L);both.setReciveSchoolNumberId(888L);both.setPickCompusNumberId(11L);both.setPickBuildCategoryNumberId(12L);both.setPickBuildingNumberId(13L);both.setReciveCompusNumberId(21L);both.setReciveBuildCategoryNumberId(22L);both.setReciveBuildingNumberId(23L);both.setPage(3);both.setPageSize(7);
  service.showByDoubleAdd(both);assertEquals(11L,service.query.getPickCampusNumberId());assertEquals(12L,service.query.getPickBuildCategoryNumberId());assertEquals(13L,service.query.getPickBuildingNumberId());assertEquals(21L,service.query.getReceiveCampusNumberId());assertEquals(22L,service.query.getReceiveBuildCategoryNumberId());assertEquals(23L,service.query.getReceiveBuildingNumberId());assertEquals(14,service.query.getOffset());
  service.showByPrice(0);assertEquals(20,service.query.getLimit());service.showByTime(0);assertEquals(0,service.query.getOffset());service.showByCategory(4L);assertEquals(20,service.query.getLimit());
 }
 @Test void legacyInvalidParametersFailAndOnlyAuthenticatedFeedAcceptsAscendingTime() {
  var service=new CaptureService();assertThrows(ParamException.class,()->service.showByPrice(2));assertThrows(ParamException.class,()->service.showByTime(null));assertThrows(ParamException.class,()->service.showByCategory(0L));
  var page=page();page.setPageSize(51);assertThrows(ParamException.class,()->service.showByTime(0,page));
  var address=new OrderShowByAddressDTO();address.setBuildingNumberId(-1L);assertThrows(ParamException.class,()->service.showByReciveAdd(address));
  var publicQuery=new PublicOrderQueryDTO();publicQuery.setSort("time_asc");assertThrows(ParamException.class,publicQuery::getOffset);
  var query=new RunnerFeedDTO();query.setSort("time_asc");assertEquals(0,query.getOffset());query.setSort("invalid");assertThrows(ParamException.class,query::getOffset);
 }
 @Test void realControllerBindingPassesPaginationThroughAllSixLegacyRoutes() throws Exception {
  var service=mock(OrderService.class);var controller=new OrderController();ReflectionTestUtils.setField(controller,"orderService",service);var mvc=MockMvcBuilders.standaloneSetup(controller).build();
  for(var route:List.of("showByPrice/0","showByTime/1","showByCategory/4","showByPickUpAdd","showByReciveAdd","showByDoubleAdd"))mvc.perform(get("/api/order/"+route).param("page","3").param("pageSize","7").param("buildingNumberId","13").param("pickBuildingNumberId","13").param("reciveBuildingNumberId","23")).andExpect(status().isOk());
  verify(service).showByPrice(eq(0),argThat(p->p.getPage()==3&&p.getPageSize()==7));verify(service).showByTime(eq(1),argThat(p->p.getPage()==3&&p.getPageSize()==7));verify(service).showByCategory(eq(4L),argThat(p->p.getPage()==3&&p.getPageSize()==7));
  verify(service).showByPickUpAdd(argThat(p->p.getPage()==3&&p.getPageSize()==7&&p.getBuildingNumberId()==13L));verify(service).showByReciveAdd(argThat(p->p.getPage()==3&&p.getPageSize()==7&&p.getBuildingNumberId()==13L));verify(service).showByDoubleAdd(argThat(p->p.getPage()==3&&p.getPageSize()==7&&p.getPickBuildingNumberId()==13L&&p.getReciveBuildingNumberId()==23L));
 }
 @Test void generatedSqlKeepsLegacyReadsBoundedAndSharedAscendingTimeStable() throws Exception {
  var config=new org.apache.ibatis.session.Configuration();try(var xml=getClass().getResourceAsStream("/mapper/OrderMapper.xml")){new org.apache.ibatis.builder.xml.XMLMapperBuilder(xml,config,"mapper/OrderMapper.xml",config.getSqlFragments()).parse();}
  var params=Map.<String,Object>of("status",0,"schoolId",1L,"categoryId",4L,"list",List.of(1L),"pickIds",List.of(1L),"reciveIds",List.of(1L));
  for(var name:List.of("showByPrice","showByTime","showByCategory","showByPickUpAdd","showByReciveAdd","showByDoubleAdd"))assertTrue(config.getMappedStatement("com.mikasa.campusrunner.mapper.OrderMapper."+name).getBoundSql(params).getSql().strip().endsWith("LIMIT 20"),name);
  var query=new RunnerFeedDTO();query.setSort("time_asc");var address=AddressBook.builder().schoolId(1L).build();
  var sql=config.getMappedStatement("com.mikasa.campusrunner.mapper.OrderMapper.listRunnerFeed").getBoundSql(Map.of("query",query,"callerId",10L,"schoolId",1L,"pickup",address,"receive",address)).getSql().replaceAll("\\s+"," ");
  assertTrue(sql.contains("o.create_time asc, o.id asc LIMIT ? OFFSET ?"));
 }
}
