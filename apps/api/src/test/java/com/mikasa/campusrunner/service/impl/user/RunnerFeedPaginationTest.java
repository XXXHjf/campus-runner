package com.mikasa.campusrunner.service.impl.user;
import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.*;
import com.mikasa.campusrunner.mapper.*;
import com.mikasa.campusrunner.pojo.dto.RunnerFeedDTO;
import com.mikasa.campusrunner.pojo.entity.AddressBook;
import com.mikasa.campusrunner.pojo.vo.*;
import com.mikasa.campusrunner.service.MediaAssetService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class RunnerFeedPaginationTest {
 @Test void sqlCombinesSchoolAddressesSearchPrivacyAndSortBeforePaging() throws Exception {
  var config=new org.apache.ibatis.session.Configuration();try(var xml=getClass().getResourceAsStream("/mapper/OrderMapper.xml")) {
   new org.apache.ibatis.builder.xml.XMLMapperBuilder(xml,config,"mapper/OrderMapper.xml",config.getSqlFragments()).parse();
  }
  var query=new RunnerFeedDTO();query.setCategoryId(2L);query.setKeyword(" 100% ");query.setSort("price_desc");query.setPage(2);query.getOffset();
  var address=AddressBook.builder().compusId(11L).buildCategoryId(12L).buildingId(13L).build();
  var bound=config.getMappedStatement("com.mikasa.campusrunner.mapper.OrderMapper.listRunnerFeed").getBoundSql(Map.of("query",query,"callerId",10L,"schoolId",7L,"pickup",address,"receive",address));
  String sql=bound.getSql().replaceAll("\\s+"," ");
  for(String part:List.of("p.school_id=?","r.school_id=?","o.deleted=0 AND o.status=0","access_candidate.order_id=o.id AND access_candidate.user_id=?",
    "access_candidate.deleted=0 AND access_candidate.status IN (0,1,2)","p.compus_id = ?","r.building_id = ?","o.category_id=?",
    "IF((COALESCE(o.user_id = ?,0) OR access_take.id IS NOT NULL), o.phone, NULL)","COALESCE(o.note,'')",
    "p.public_address AS pick_up_address","r.public_address AS recive_address","BINARY LOWER","ESCAPE '!'","COALESCE(o.price,0) desc","o.create_time desc, o.id desc LIMIT ? OFFSET ?"))assertTrue(sql.replace(" ", "").contains(part.replace(" ", "")),part);
  assertTrue(sql.indexOf("p.compus_id")<sql.indexOf("LIMIT"));assertFalse(sql.contains("select *"));
 }
 private OrderServiceImpl service(OrderMapper orders,UserMapper users,MediaAssetService media) {
  var service=new OrderServiceImpl();ReflectionTestUtils.setField(service,"orderMapper",orders);ReflectionTestUtils.setField(service,"userMapper",users);ReflectionTestUtils.setField(service,"mediaAssetService",media);return service;
 }
 private UserVO user(){var user=new UserVO();user.setAuthentication(1);user.setSchoolId(7L);return user;}
 private OrderShowVO row(long id,boolean authorized){var row=new OrderShowVO();row.setId(id);row.setAccessUserId(10L);row.setPrivateAccess(authorized);row.setUserId(10L);row.setNote("private-note");row.setPhone("private-phone");row.setOrderNumber("private-number");row.setPickUpAddress("public-place");return row;}
 @Test void allLobbyRowsUseBlurredPreviewsAndAnExplicitSafeProjection() throws Exception {
  BaseContext.setCurrentId(10L);try {
   var users=mock(UserMapper.class);var orders=mock(OrderMapper.class);var media=mock(MediaAssetService.class);var service=service(orders,users,media);when(users.getById(10L)).thenReturn(user());
   var privateRow=row(1L,true);var publicRow=row(2L,false);when(orders.listRunnerFeed(any(),eq(10L),eq(7L),any(),any())).thenReturn(List.of(privateRow,publicRow));
   when(media.resolveOrderPreviewBindings(anyList())).thenReturn(Map.of(1L,List.of(new BoundMediaVO(null,"blurred-1",0)),2L,List.of(new BoundMediaVO(null,"blurred-2",0))));
   when(media.resolvePublicBindings(anyString(),anyList(),anyString(),anyInt())).thenReturn(Map.of());
   var result=service.listRunnerFeed(new RunnerFeedDTO());assertEquals("blurred-1",result.get(0).getImage());assertEquals("private-note",result.get(0).getNote());
   assertEquals("private-note",result.get(1).getNote());assertNull(result.get(1).getPhone());assertNull(result.get(1).getUserId());assertNull(result.get(1).getOrderNumber());assertEquals("blurred-2",result.get(1).getImage());
   assertNull(result.get(0).getPhone());assertNull(result.get(0).getOrderNumber());assertTrue(result.get(0).getImageAssetIds().isEmpty());
   verify(media).resolveOrderPreviewBindings(List.of(1L,2L));verify(media,never()).resolveAuthorizedBindings(anyString(),anyList(),anyString(),anyInt());verify(orders,never()).getPublicOrderById(any());verify(users,never()).getSchoolId(any());
   String json=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(privateRow);assertFalse(json.contains("privateAccess"));assertFalse(json.contains("accessUserId"));
  }finally{BaseContext.removeCurrentId();}
 }
 @Test void incorrectAccountProofAndUnapprovedUsersFailBeforeMediaOrFeedRead() {
  BaseContext.setCurrentId(10L);try {
   var users=mock(UserMapper.class);var orders=mock(OrderMapper.class);var media=mock(MediaAssetService.class);var service=service(orders,users,media);
   var user=user();user.setAuthentication(0);when(users.getById(10L)).thenReturn(user);assertThrows(UserException.class,()->service.listRunnerFeed(new RunnerFeedDTO()));verifyNoInteractions(orders,media);
   user.setAuthentication(1);var row=row(1,true);row.setAccessUserId(11L);when(orders.listRunnerFeed(any(),any(),any(),any(),any())).thenReturn(List.of(row));
   assertThrows(OrderException.class,()->service.listRunnerFeed(new RunnerFeedDTO()));verifyNoInteractions(media);
  }finally{BaseContext.removeCurrentId();}
 }
 @Test void selectedNumberIdsResolveToSnapshotIdsWithinTheAuthenticatedSchool() {
  BaseContext.setCurrentId(10L);try {
   var users=mock(UserMapper.class);var orders=mock(OrderMapper.class);var media=mock(MediaAssetService.class);var service=service(orders,users,media);
   var campuses=mock(CompusMapper.class);var types=mock(BuildCategoryMapper.class);var buildings=mock(BuildingMapper.class);
   ReflectionTestUtils.setField(service,"compusMapper",campuses);ReflectionTestUtils.setField(service,"buildCategoryMapper",types);ReflectionTestUtils.setField(service,"buildingMapper",buildings);
   when(users.getById(10L)).thenReturn(user());when(users.getSchoolId(10L)).thenReturn(7L);when(campuses.getIdByNumberId(101L)).thenReturn(11L);when(types.getIdByNumberId(102L)).thenReturn(12L);when(buildings.getIdByNumberId(103L)).thenReturn(13L);
   var q=new RunnerFeedDTO();q.setPickCampusNumberId(101L);q.setPickBuildCategoryNumberId(102L);q.setPickBuildingNumberId(103L);
   when(orders.listRunnerFeed(any(),any(),any(),any(),any())).thenReturn(List.of());when(media.resolveAuthorizedBindings(anyString(),anyList(),anyString(),anyInt())).thenReturn(Map.of());when(media.resolvePublicBindings(anyString(),anyList(),anyString(),anyInt())).thenReturn(Map.of());
   service.listRunnerFeed(q);verify(orders).listRunnerFeed(eq(q),eq(10L),eq(7L),argThat(a->a.getCompusId()==11L&&a.getBuildCategoryId()==12L&&a.getBuildingId()==13L),argThat(a->a.getSchoolId()==7L&&a.getCompusId()==null));
  }finally{BaseContext.removeCurrentId();}
 }
 @Test void invalidBoundsAndAnonymousCallsNeverReachDatabase() {
  var users=mock(UserMapper.class);var orders=mock(OrderMapper.class);var media=mock(MediaAssetService.class);var service=service(orders,users,media);var q=new RunnerFeedDTO();q.setPickCampusNumberId(0L);assertThrows(ParamException.class,()->service.listRunnerFeed(q));
  q.setPickCampusNumberId(null);assertThrows(OrderException.class,()->service.listRunnerFeed(q));verifyNoInteractions(users,orders,media);
 }
}
