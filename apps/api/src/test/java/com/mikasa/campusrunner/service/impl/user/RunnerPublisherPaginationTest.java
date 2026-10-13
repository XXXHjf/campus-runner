package com.mikasa.campusrunner.service.impl.user;
import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.*;
import com.mikasa.campusrunner.mapper.OrderMapper;
import com.mikasa.campusrunner.pojo.dto.RunnerOrderQueryDTO;
import com.mikasa.campusrunner.pojo.vo.OrderShowVO;
import com.mikasa.campusrunner.service.MediaAssetService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class RunnerPublisherPaginationTest {
    @Test void actualSqlFiltersBeforePagingAndPreservesSearchFields() throws Exception {
        var config = new org.apache.ibatis.session.Configuration();
        try(var xml=getClass().getResourceAsStream("/mapper/OrderMapper.xml")) {
            new org.apache.ibatis.builder.xml.XMLMapperBuilder(xml,config,"mapper/OrderMapper.xml",config.getSqlFragments()).parse();
        }
        var query=new RunnerOrderQueryDTO(); query.setGroup("refund");query.setPage(3);query.setKeyword(" AB %_");
        assertEquals(40,query.getOffset());assertEquals("ab!%!_",query.getSearchKeyword());
        var bound=config.getMappedStatement("com.mikasa.campusrunner.mapper.OrderMapper.getMyPage").getBoundSql(Map.of("id",10L,"query",query));
        String sql=bound.getSql().replaceAll("\\s+"," ");
        for(String text:List.of("orders.user_id = ?","publisher_hidden","orders.status IN (-2,-3,-4)","category.category_name","orders.note","address_recive.address","orders.order_number","DATE_FORMAT","DECIMAL(20,2)","BINARY LOWER","ESCAPE '!'","orders.create_time desc, orders.id desc","LIMIT ? OFFSET ?")) assertTrue(sql.contains(text),text);
        assertEquals(25,bound.getParameterMappings().stream().filter(m->m.getProperty().startsWith("__frch_space_")).count());
        query.setGroup("bogus");assertThrows(ParamException.class,query::getOffset);
        query.setGroup("all");query.setPageSize(51);assertThrows(ParamException.class,query::getOffset);
    }
    @Test void refusesForeignOrderBeforeSigningAnyMedia() {
        BaseContext.setCurrentId(10L);
        try {
            var mapper=mock(OrderMapper.class);var media=mock(MediaAssetService.class);
            var service=new OrderServiceImpl();ReflectionTestUtils.setField(service,"orderMapper",mapper);ReflectionTestUtils.setField(service,"mediaAssetService",media);
            var row=new OrderShowVO();row.setId(1L);row.setUserId(11L);
            when(mapper.getMyPage(eq(10L),any())).thenReturn(List.of(row));
            assertThrows(OrderException.class,()->service.showMy(new RunnerOrderQueryDTO()));verifyNoInteractions(media);
        } finally { BaseContext.removeCurrentId(); }
    }
    @Test void ownPageUsesOnePrivateBatchAndOnePublicCategoryBatch() {
        BaseContext.setCurrentId(10L);
        try {
            var mapper=mock(OrderMapper.class);var media=mock(MediaAssetService.class);
            var service=new OrderServiceImpl();ReflectionTestUtils.setField(service,"orderMapper",mapper);ReflectionTestUtils.setField(service,"mediaAssetService",media);
            var first=new OrderShowVO();first.setId(1L);first.setUserId(10L);first.setCategoryId(2L);first.setImage("stale");
            var second=new OrderShowVO();second.setId(3L);second.setUserId(10L);
            when(mapper.getMyPage(eq(10L),any())).thenReturn(List.of(first,second));
            when(media.resolveAuthorizedBindings(anyString(),anyList(),anyString(),anyInt())).thenReturn(Map.of());
            when(media.resolvePublicBindings(anyString(),anyList(),anyString(),anyInt())).thenReturn(Map.of());
            assertEquals(2,service.showMy().size());assertNull(first.getImage());assertTrue(first.getImages().isEmpty());
            verify(media).resolveAuthorizedBindings(anyString(),eq(List.of(1L,3L)),eq("ORDER_IMAGE"),eq(320));
            verify(media).resolvePublicBindings(anyString(),eq(List.of(2L)),eq("ORDER_CATEGORY_ICON"),eq(0));
            verifyNoMoreInteractions(media);
        } finally {BaseContext.removeCurrentId();}
    }
}
