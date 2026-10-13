package com.mikasa.campusrunner.service.impl.user;
import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.ParamException;
import com.mikasa.campusrunner.mapper.*;
import com.mikasa.campusrunner.pojo.dto.BargainPageDTO;
import com.mikasa.campusrunner.pojo.entity.SecondHandProduct;
import com.mikasa.campusrunner.pojo.vo.UserVO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class BargainPaginationTest {
 private org.apache.ibatis.session.Configuration configuration() throws Exception {
  var config=new org.apache.ibatis.session.Configuration();try(var xml=getClass().getResourceAsStream("/mapper/SecondHandBargainMapper.xml")) {
   new org.apache.ibatis.builder.xml.XMLMapperBuilder(xml,config,"mapper/SecondHandBargainMapper.xml",config.getSqlFragments()).parse();
  }return config;
 }
 @Test void actualSqlFiltersIdentityRoleAndAcceptedQuoteBeforePaging() throws Exception {
  var config=configuration();var query=new BargainPageDTO();query.setRole("issued");query.setStatus(1);query.setWithoutOrder(true);query.setBargainId(123L);query.setPage(2);assertEquals(20,query.getOffset());
  for(String method:List.of("listMinePage","listByProductPage")) {
   var bound=config.getMappedStatement("com.mikasa.campusrunner.mapper.SecondHandBargainMapper."+method)
     .getBoundSql(Map.of("query",query,"userId",20L,"productId",1L,"buyerId",20L));
   String sql=bound.getSql().replaceAll("\\s+"," ");
   for(String part:List.of("b.deleted = 0","b.buyer_id = ?","then 3 else b.status end ) = ?","b.id = ?","o.id IS NULL","o.deleted = 0",
    "order by b.create_time desc, b.id desc, o.id desc LIMIT ? OFFSET ?")) assertTrue(sql.contains(part),part);
   assertTrue(sql.indexOf("b.id = ?")<sql.indexOf("LIMIT"));
   if(method.equals("listMinePage"))assertTrue(sql.contains("(b.buyer_id = ? or b.seller_id = ?)"));else assertTrue(sql.contains("b.product_id = ?"));
  }
 }
 @Test void sellerAndBuyerProductAccessIsDecidedFromAuthenticatedIdentity() {
  BaseContext.setCurrentId(20L);try {
   var users=mock(UserMapper.class);var products=mock(SecondHandProductMapper.class);var bargains=mock(SecondHandBargainMapper.class);var service=new SecondHandServiceImpl();
   ReflectionTestUtils.setField(service,"userMapper",users);ReflectionTestUtils.setField(service,"productMapper",products);ReflectionTestUtils.setField(service,"bargainMapper",bargains);
   var user=new UserVO();user.setAuthentication(1);when(users.getById(20L)).thenReturn(user);
   var product=new SecondHandProduct();product.setId(1L);product.setSellerId(10L);when(products.getById(1L)).thenReturn(product);
   var query=new BargainPageDTO();service.listProductBargains(1L,query);verify(bargains).listByProductPage(1L,20L,query);
   product.setSellerId(20L);service.listProductBargains(1L,query);verify(bargains).listByProductPage(1L,null,query);
   verify(bargains,never()).listByProduct(any());
  }finally{BaseContext.removeCurrentId();}
 }
 @Test void malformedBoundsAndFiltersFailBeforeListReads() {
  var query=new BargainPageDTO();assertEquals(20,query.getLimit());query.setPageSize(51);assertThrows(ParamException.class,query::getOffset);
  query.setPageSize(20);query.setRole("other");assertThrows(ParamException.class,query::getOffset);
  query.setRole("received");query.setStatus(5);assertThrows(ParamException.class,query::getOffset);
  query.setStatus(1);query.setBargainId(0L);assertThrows(ParamException.class,query::getOffset);
 }
}
