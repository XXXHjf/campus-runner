package com.mikasa.campusrunner.service.impl.user;
import com.mikasa.campusrunner.mapper.SecondHandProductMapper;
import com.mikasa.campusrunner.pojo.dto.SecondHandProductQueryDTO;
import com.mikasa.campusrunner.pojo.vo.SecondHandProductVO;
import com.mikasa.campusrunner.pojo.vo.BoundMediaVO;
import com.mikasa.campusrunner.service.MediaAssetService;
import com.mikasa.campusrunner.common.exception.ParamException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class AdminSecondHandProductPaginationTest {
 @Test void pageUsesFilteredFullCountsAndOnlyResolvesItsOwnMedia() {
  var products=mock(SecondHandProductMapper.class);var media=mock(MediaAssetService.class);var service=new SecondHandServiceImpl();ReflectionTestUtils.setField(service,"productMapper",products);ReflectionTestUtils.setField(service,"mediaAssetService",media);
  var query=new SecondHandProductQueryDTO();query.setPage(2);query.setCategoryId(3L);query.setKeyword("book");var first=new SecondHandProductVO();first.setId(21L);first.setImages("legacy");var second=new SecondHandProductVO();second.setId(22L);second.setImages("legacy");
  when(products.listPage(query,null)).thenReturn(List.of(first,second));when(products.countProducts(query,false,null)).thenReturn(125L);when(products.countProducts(query,true,null)).thenReturn(80L);
  when(media.resolvePublicBindings("SECOND_HAND_PRODUCT",List.of(21L,22L),"SECOND_HAND_PRODUCT_IMAGE",0)).thenReturn(Map.of(21L,List.of(new BoundMediaVO(1L,"fresh",0))));
  var result=service.adminListProducts(query);assertEquals(125,result.getTotal());assertEquals(80,result.getActiveProducts());assertEquals(2,result.getPage());assertEquals(20,result.getPageSize());assertEquals("fresh",result.getList().get(0).getImages());assertEquals("",result.getList().get(1).getImages());
  verify(products,never()).list(any(),any());verify(media).resolvePublicBindings("SECOND_HAND_PRODUCT",List.of(21L,22L),"SECOND_HAND_PRODUCT_IMAGE",0);verifyNoMoreInteractions(media);
 }
 @Test void invalidPageAndStatusNeverReadProductsOrCounts() {
  var products=mock(SecondHandProductMapper.class);var service=new SecondHandServiceImpl();ReflectionTestUtils.setField(service,"productMapper",products);var query=new SecondHandProductQueryDTO();query.setPage(0);assertThrows(ParamException.class,()->service.adminListProducts(query));query.setPage(1);query.setPageSize(51);assertThrows(ParamException.class,()->service.adminListProducts(query));query.setPageSize(20);query.setStatus(5);assertThrows(ParamException.class,()->service.adminListProducts(query));verifyNoInteractions(products);
 }
 @Test void generatedCountsShareAllFiltersWithoutOrderOrPaginationAndInternalLegacyReadIsBounded() throws Exception {
  var config=new org.apache.ibatis.session.Configuration();try(var xml=getClass().getResourceAsStream("/mapper/SecondHandProductMapper.xml")){new org.apache.ibatis.builder.xml.XMLMapperBuilder(xml,config,"mapper/SecondHandProductMapper.xml",config.getSqlFragments()).parse();}
  var query=new SecondHandProductQueryDTO();query.setPage(2);query.setKeyword("book");query.setCategoryId(3L);query.setStatus(4);var params=new HashMap<String,Object>();params.put("query",query);params.put("schoolId",null);params.put("onSaleOnly",true);
  var count=config.getMappedStatement("com.mikasa.campusrunner.mapper.SecondHandProductMapper.countProducts").getBoundSql(params).getSql().replaceAll("\\s+"," ");var page=config.getMappedStatement("com.mikasa.campusrunner.mapper.SecondHandProductMapper.listPage").getBoundSql(params).getSql().replaceAll("\\s+"," ");
  for(var part:List.of("p.deleted = 0","p.status = ?","p.category_id = ?","p.title like","p.description like")){assertTrue(count.contains(part));assertTrue(page.contains(part));}
  assertTrue(count.contains("and p.status = 0"));assertFalse(count.contains("order by"));assertFalse(count.contains("limit"));assertFalse(count.contains("p.school_id = ?"));assertTrue(page.contains("order by p.create_time desc, p.id desc limit ? offset ?"));assertTrue(config.getMappedStatement("com.mikasa.campusrunner.mapper.SecondHandProductMapper.list").getBoundSql(params).getSql().strip().endsWith("limit 20"));
 }
}
