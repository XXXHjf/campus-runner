package com.mikasa.campusrunner.service.impl.user;
import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.OrderException;
import com.mikasa.campusrunner.mapper.*;
import com.mikasa.campusrunner.pojo.vo.*;
import com.mikasa.campusrunner.service.user.UserTaskService;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class UserTaskSummaryTest {
 @Test void sqlCountsFullScopeWithoutListLimitAndUsesExistingSelectionRules() throws Exception {
  var config=new org.apache.ibatis.session.Configuration();
  try(var xml=getClass().getResourceAsStream("/mapper/UserTaskMapper.xml")) {
   new org.apache.ibatis.builder.xml.XMLMapperBuilder(xml,config,"mapper/UserTaskMapper.xml",config.getSqlFragments()).parse();
  }
  String sql=config.getMappedStatement("com.mikasa.campusrunner.mapper.UserTaskMapper.summarize")
   .getBoundSql(Map.of("userId",10L,"schoolId",2L,"unpaidAfter",LocalDateTime.now())).getSql().replaceAll("\\s+"," ");
  for(String part:List.of("COUNT(*)","tk.user_id=?","p.school_id=?","r.school_id=?","tk.deleted=0 AND tk.status=2 AND o.status=5",
   "o.price IS NOT NULL AND o.price != 0","o.user_id=? AND o.deleted=0 AND o.status=-1 AND o.create_time > ?",
   "buyer_id=? AND deleted=0 AND status IN (0,2)","buyer_id=? AND deleted=0 AND status=2",
   "status=8 AND transfer_state='WAIT_USER_CONFIRM'","seller_id=? AND deleted=0 AND status=0")) assertTrue(sql.contains(part),part);
  assertFalse(sql.toLowerCase().contains("limit"));assertFalse(sql.contains("media_asset"));
 }
 @Test void aggregateCountsAreNotLimitedToTwentyAndSingleIdsAreConditional() {
  BaseContext.setCurrentId(10L);try {
   var users=mock(UserMapper.class);var tasks=mock(UserTaskMapper.class);var user=new UserVO();user.setAuthentication(1);user.setSchoolId(2L);
   when(users.getById(10L)).thenReturn(user);
   when(tasks.summarize(eq(10L),eq(2L),any())).thenReturn(new UserTaskSummaryVO(75L,123L,1L,456L,60L,45L,50L,80L));
   var result=new UserTaskService(users,tasks).summary();assertEquals(75,result.runnerReceivableCount());assertNull(result.runnerReceivableOrderId());assertEquals(456,result.runnerUnpaidOrderId());assertEquals(60,result.secondHandBuyerCount());
   verify(tasks).summarize(eq(10L),eq(2L),argThat(t->t.isAfter(LocalDateTime.now().minusMinutes(31))&&t.isBefore(LocalDateTime.now().minusMinutes(29))));
  }finally{BaseContext.removeCurrentId();}
 }
 @Test void unapprovedUserAndMissingIdentityCannotRunCountsForAnotherAccount() {
  var users=mock(UserMapper.class);var tasks=mock(UserTaskMapper.class);var service=new UserTaskService(users,tasks);
  assertThrows(OrderException.class,service::summary);verifyNoInteractions(users,tasks);
  BaseContext.setCurrentId(10L);try {
   var user=new UserVO();user.setAuthentication(0);when(users.getById(10L)).thenReturn(user);
   assertEquals(UserTaskSummaryVO.empty(),service.summary());verifyNoInteractions(tasks);
  }finally{BaseContext.removeCurrentId();}
 }
}
