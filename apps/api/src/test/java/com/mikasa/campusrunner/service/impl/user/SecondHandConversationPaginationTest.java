package com.mikasa.campusrunner.service.impl.user;
import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.*;
import com.mikasa.campusrunner.mapper.*;
import com.mikasa.campusrunner.pojo.dto.*;
import com.mikasa.campusrunner.pojo.entity.*;
import com.mikasa.campusrunner.pojo.vo.*;
import com.mikasa.campusrunner.service.MediaAssetService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class SecondHandConversationPaginationTest {
 private UserVO user(){var user=new UserVO();user.setAuthentication(1);return user;}
 private SecondHandServiceImpl service(UserMapper users,SecondHandMessageMapper messages,SecondHandProductMapper products,MediaAssetService media){var service=new SecondHandServiceImpl();ReflectionTestUtils.setField(service,"userMapper",users);ReflectionTestUtils.setField(service,"messageMapper",messages);ReflectionTestUtils.setField(service,"productMapper",products);ReflectionTestUtils.setField(service,"mediaAssetService",media);return service;}
 @Test void pageSignsMatchingVisibleProductsInOneBatchWithoutProductOrHistoryReads() throws Exception {
  BaseContext.setCurrentId(10L);try{var users=mock(UserMapper.class);var messages=mock(SecondHandMessageMapper.class);var products=mock(SecondHandProductMapper.class);var media=mock(MediaAssetService.class);var service=service(users,messages,products,media);when(users.getById(10L)).thenReturn(user());var query=new ListPageDTO();query.setPage(2);
   var a=SecondHandConversationVO.builder().productId(30L).mediaProductId(30L).accessUserId(10L).counterpartyId(20L).orderId(50L).unreadCount(99).productImages("legacy").build();var b=SecondHandConversationVO.builder().productId(40L).accessUserId(10L).counterpartyId(20L).unreadCount(0).productImages("deleted-legacy").build();
   when(messages.listConversationsPage(10L,query)).thenReturn(List.of(a,b));when(media.resolvePublicBindings("SECOND_HAND_PRODUCT",List.of(30L),"SECOND_HAND_PRODUCT_IMAGE",0)).thenReturn(Map.of(30L,List.of(new BoundMediaVO(1L,"fresh",0))));
   var result=service.listConversations(query);assertEquals("fresh",result.get(0).getProductImages());assertEquals(99,result.get(0).getUnreadCount());assertEquals(50L,result.get(0).getOrderId());assertEquals("",result.get(1).getProductImages());assertEquals("商品",result.get(1).getProductTitle());assertEquals("同学",result.get(1).getCounterpartyName());verifyNoInteractions(products);verify(messages,never()).listForUser(any());verify(media).resolvePublicBindings("SECOND_HAND_PRODUCT",List.of(30L),"SECOND_HAND_PRODUCT_IMAGE",0);verifyNoMoreInteractions(media);
   var json=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(a);assertFalse(json.contains("accessUserId"));assertFalse(json.contains("mediaProductId"));
  }finally{BaseContext.removeCurrentId();}
 }
 @Test void malformedPageOrIncorrectOwnerAndMediaProofFailBeforeSigning() {
  BaseContext.setCurrentId(10L);try{var users=mock(UserMapper.class);var messages=mock(SecondHandMessageMapper.class);var products=mock(SecondHandProductMapper.class);var media=mock(MediaAssetService.class);var service=service(users,messages,products,media);var query=new ListPageDTO();query.setPage(0);assertThrows(ParamException.class,()->service.listConversations(query));verifyNoInteractions(users,messages,products,media);query.setPage(1);when(users.getById(10L)).thenReturn(user());var row=SecondHandConversationVO.builder().productId(30L).mediaProductId(30L).accessUserId(11L).counterpartyId(20L).unreadCount(0).build();when(messages.listConversationsPage(10L,query)).thenReturn(List.of(row));assertThrows(SecondHandException.class,()->service.listConversations(query));row.setAccessUserId(10L);row.setMediaProductId(31L);assertThrows(SecondHandException.class,()->service.listConversations(query));verifyNoInteractions(media,products);
  }finally{BaseContext.removeCurrentId();}
 }
 @Test void sendingReadsOnlyTheCreatedMessageUnderCurrentUserIdentity() {
  BaseContext.setCurrentId(10L);try{var users=mock(UserMapper.class);var messages=mock(SecondHandMessageMapper.class);var products=mock(SecondHandProductMapper.class);var media=mock(MediaAssetService.class);var service=service(users,messages,products,media);when(users.getById(10L)).thenReturn(user());when(products.getById(30L)).thenReturn(SecondHandProduct.builder().id(30L).sellerId(20L).build());var subscriptions=mock(SecondHandSubscriptionService.class);ReflectionTestUtils.setField(service,"subscriptions",subscriptions);doAnswer(call->{((SecondHandMessage)call.getArgument(0)).setId(500L);return null;}).when(messages).insert(any());var created=new SecondHandMessageVO();created.setId(500L);when(messages.getForUser(500L,10L)).thenReturn(created);var dto=new SecondHandMessageDTO();dto.setProductId(30L);dto.setReceiverId(20L);dto.setContent("hello");assertSame(created,service.createMessage(dto));verify(messages).getForUser(500L,10L);verify(messages,never()).listByProductForUser(any(),any());verify(subscriptions).message(any());
  }finally{BaseContext.removeCurrentId();}
 }
 @Test void sqlPagesLatestOwnedConversationBeforeFullUnreadAndOrderFallbackAndProtectsSingleRead() throws Exception {
  var config=new org.apache.ibatis.session.Configuration();try(var xml=getClass().getResourceAsStream("/mapper/SecondHandMessageMapper.xml")){new org.apache.ibatis.builder.xml.XMLMapperBuilder(xml,config,"mapper/SecondHandMessageMapper.xml",config.getSqlFragments()).parse();}var query=new ListPageDTO();query.setPage(2);var bound=config.getMappedStatement("com.mikasa.campusrunner.mapper.SecondHandMessageMapper.listConversationsPage").getBoundSql(Map.of("userId",10L,"query",query));var sql=bound.getSql().replaceAll("\\s+"," ");
  for(var part:List.of("m.deleted = 0 and (m.sender_id = ? or m.receiver_id = ?)","and not exists", "newer.product_id = m.product_id","newer.sender_id = m.receiver_id","newer.create_time <=> m.create_time","newer.id > m.id","order by m.create_time desc, m.id desc limit ? offset ?","unread.sender_id = latest.counterparty_id and unread.receiver_id = ?","unread.read_time is null","history.order_id is not null","order by history.create_time desc, history.id desc limit 1","p.deleted = 0","order by latest.create_time desc, latest.id desc"))assertTrue(sql.contains(part),part);
  var single=config.getMappedStatement("com.mikasa.campusrunner.mapper.SecondHandMessageMapper.getForUser").getBoundSql(Map.of("id",500L,"userId",10L)).getSql().replaceAll("\\s+"," ");assertTrue(single.contains("m.id = ? and m.deleted = 0 and (m.sender_id = ? or m.receiver_id = ?)"));
 }
}
