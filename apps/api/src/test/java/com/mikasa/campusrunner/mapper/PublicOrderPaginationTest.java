package com.mikasa.campusrunner.mapper;
import com.mikasa.campusrunner.pojo.dto.PublicOrderQueryDTO;
import com.mikasa.campusrunner.common.exception.ParamException;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class PublicOrderPaginationTest {
 private org.apache.ibatis.session.Configuration configuration() throws Exception {
  var config=new org.apache.ibatis.session.Configuration();try(var xml=getClass().getResourceAsStream("/mapper/OrderMapper.xml")) {
   new org.apache.ibatis.builder.xml.XMLMapperBuilder(xml,config,"mapper/OrderMapper.xml",config.getSqlFragments()).parse();
  }return config;
 }
 @Test void filterAndSortApplyToSafePublicProjectionBeforeLimit() throws Exception {
  var q=new PublicOrderQueryDTO();q.setCategoryId(2L);q.setPickupLabels("[\"North\",\"Library\"]");q.setReceiveLabels("[\"South\"]");q.setKeyword(" Test %_");q.setSort("price_asc");q.setPage(7);
  assertEquals(120,q.getOffset());assertEquals("test !%!_",q.getLiteralKeyword());
  var bound=configuration().getMappedStatement("com.mikasa.campusrunner.mapper.OrderMapper.listPublicOrdersPage").getBoundSql(Map.of("query",q));
  String sql=bound.getSql().replaceAll("\\s+"," ");
  for(String part:List.of("o.deleted = 0 and o.status = 0","o.category_id = ?","LOCATE(BINARY ?, BINARY p.public_address)","LOCATE(BINARY ?, BINARY r.public_address)","BINARY LOWER","ESCAPE '!'","COALESCE(o.price,0) asc","o.create_time desc, o.id desc","LIMIT ? OFFSET ?")) assertTrue(sql.contains(part),part);
  assertTrue(sql.indexOf("LOCATE")<sql.indexOf("LIMIT"));
  for(String field:List.of("o.phone","o.order_number","o.image","p.address ","r.address ","select *")) assertFalse(sql.contains(field),field);
  for(String field:List.of("o.note","sender.username as username","o.door_access")) assertTrue(sql.contains(field),field);
 }
 @Test void keywordKeepsInternalSpacesAndEscapesLiteralSearch() {
  var q=new PublicOrderQueryDTO();q.setKeyword("\u00a0 A B %_!\u3000");assertEquals("a b !%!_!!",q.getLiteralKeyword());
  q.setKeyword("x".repeat(201));assertThrows(ParamException.class,q::getOffset);
 }
 @Test void invalidPagesSortAndLabelsAreRejected() {
  var q=new PublicOrderQueryDTO();q.setPageSize(51);assertThrows(ParamException.class,q::getOffset);q.setPageSize(20);
  q.setSort("price;drop");assertThrows(ParamException.class,q::getOffset);q.setSort("time");
  for(String text:List.of("{}","[1]","[\"a\",\"b\",\"c\",\"d\",\"e\"]","[\""+"a".repeat(101)+"\"]")) {q.setPickupLabels(text);assertThrows(ParamException.class,q::getOffset);}
  q.setPickupLabels("[]");q.setCategoryId(0L);assertThrows(ParamException.class,q::getOffset);
 }
}
