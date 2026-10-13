package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.exception.ParamException;
import com.mikasa.campusrunner.controller.user.SecondHandController;
import com.mikasa.campusrunner.pojo.dto.ListPageDTO;
import com.mikasa.campusrunner.pojo.dto.SecondHandProductQueryDTO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SecondHandProductPaginationTest {
    @Test void defaultsBoundsAndLargePageArithmeticAreExplicit() {
        var page = new ListPageDTO();
        assertEquals(20, page.getLimit()); assertEquals(0, page.getOffset());
        page.setPage(2); page.setPageSize(50);
        assertEquals(50, page.getOffset());
        page.setPage(Integer.MAX_VALUE);
        assertEquals((Integer.MAX_VALUE - 1L) * 50, page.getOffset());
        page.setPage(0); assertThrows(ParamException.class, page::getOffset);
        page.setPage(1); page.setPageSize(51); assertThrows(ParamException.class, page::getOffset);
        page.setPageSize(-1); assertThrows(ParamException.class, page::getOffset);
    }
    @Test void controllerPreservesDefaultsAndRejectsMalformedPagination() {
        var controller = new SecondHandController();
        SecondHandProductQueryDTO defaults = ReflectionTestUtils.invokeMethod(controller, "buildProductQuery", Map.of());
        assertEquals(20, defaults.getLimit()); assertEquals(0, defaults.getOffset());
        SecondHandProductQueryDTO next = ReflectionTestUtils.invokeMethod(controller, "buildProductQuery", Map.of("page", "3", "pageSize", "20"));
        assertEquals(40, next.getOffset());
        assertThrows(ParamException.class, () -> ReflectionTestUtils.invokeMethod(controller, "buildProductQuery", Map.of("page", "text")));
        assertThrows(ParamException.class, () -> ReflectionTestUtils.invokeMethod(controller, "buildProductQuery", Map.of("pageSize", "500")));
    }
    @Test void generatedSqlKeepsFiltersAndDeterministicOrderBeforeBoundLimit() throws Exception {
        var configuration = new org.apache.ibatis.session.Configuration();
        try (var xml = getClass().getResourceAsStream("/mapper/SecondHandProductMapper.xml")) {
            new org.apache.ibatis.builder.xml.XMLMapperBuilder(xml, configuration,
                    "mapper/SecondHandProductMapper.xml", configuration.getSqlFragments()).parse();
        }
        var query = new SecondHandProductQueryDTO(); query.setPage(2); query.setPageSize(20);
        query.setKeyword("book"); query.setCategoryId(3L); query.setStatus(0);
        var params = new HashMap<String, Object>(); params.put("query", query); params.put("schoolId", 10L);
        var sql = configuration.getMappedStatement("com.mikasa.campusrunner.mapper.SecondHandProductMapper.listPage").getBoundSql(params);
        String text = sql.getSql().replaceAll("\\s+", " ");
        assertTrue(text.contains("p.school_id = ?")); assertTrue(text.contains("p.category_id = ?"));
        assertTrue(text.contains("p.status = ?")); assertTrue(text.contains("p.title like"));
        assertTrue(text.contains("order by p.create_time desc, p.id desc limit ? offset ?"));
        var mappings = sql.getParameterMappings();
        assertEquals("query.limit", mappings.get(mappings.size() - 2).getProperty());
        assertEquals("query.offset", mappings.get(mappings.size() - 1).getProperty());
    }
    @Test void personalSqlScopesOwnerBeforeFilteringAndPagination() throws Exception {
        var configuration = new org.apache.ibatis.session.Configuration();
        try (var xml = getClass().getResourceAsStream("/mapper/SecondHandProductMapper.xml")) {
            new org.apache.ibatis.builder.xml.XMLMapperBuilder(xml, configuration,
                    "mapper/SecondHandProductMapper.xml", configuration.getSqlFragments()).parse();
        }
        var query = new SecondHandProductQueryDTO(); query.setPage(2); query.setStatus(4); query.setKeyword("100%_!");
        assertEquals("100!%!_!!", query.getLiteralKeyword());
        var params = Map.<String, Object>of("query", query, "sellerId", 10L, "userId", 10L);
        for (String method : java.util.List.of("listBySeller", "listFavorites")) {
            var sql = configuration.getMappedStatement("com.mikasa.campusrunner.mapper.SecondHandProductMapper." + method).getBoundSql(params);
            String text = sql.getSql().replaceAll("\\s+", " ");
            assertTrue(text.contains("p.deleted = 0"));
            assertTrue(text.contains(method.equals("listBySeller") ? "p.seller_id = ?" : "f.user_id = ?"));
            assertTrue(text.contains("limit ? offset ?"));
            if (method.equals("listBySeller")) {
                assertTrue(text.contains("p.status = ?")); assertTrue(text.contains("p.id desc"));
            } else {
                assertTrue(text.contains("lower(c.name) like")); assertTrue(text.contains("escape '!'"));
                assertTrue(text.contains("order by f.create_time desc, f.id desc"));
            }
            var mappings = sql.getParameterMappings();
            assertEquals("query.limit", mappings.get(mappings.size() - 2).getProperty());
            assertEquals("query.offset", mappings.get(mappings.size() - 1).getProperty());
        }
    }
}
