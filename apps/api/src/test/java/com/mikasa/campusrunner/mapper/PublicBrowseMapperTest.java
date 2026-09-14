package com.mikasa.campusrunner.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PublicBrowseMapperTest {
    private String sql(String mapper, String statement, Map<String, Object> args) throws Exception {
        Configuration config = new Configuration();
        try (var input = Resources.getResourceAsStream("mapper/" + mapper + ".xml")) {
            new XMLMapperBuilder(input, config, mapper, config.getSqlFragments()).parse();
        }
        return config.getMappedStatement("com.mikasa.campusrunner.mapper." + mapper + "." + statement)
                .getBoundSql(args).getSql().replaceAll("\\s+", " ").toLowerCase();
    }

    @Test
    void publicOrdersNeverSelectPrivateFields() throws Exception {
        String query = sql("OrderMapper", "listPublicOrders", Map.of());
        assertTrue(query.contains("o.status = 0"));
        assertTrue(query.contains("limit 100"));
        for (String field : new String[]{"o.phone", "o.username", "o.note", "o.image", "o.order_number", "p.details", "r.details", "select *"}) {
            assertFalse(query.contains(field), field);
        }
    }

    @Test
    void campusDictionariesAllowGuestsAndKeepSelectedSchoolFilter() throws Exception {
        for (String mapper : new String[]{"SchoolMapper", "CompusMapper", "BuildCategoryMapper", "BuildingMapper"}) {
            Map<String, Object> guest = new HashMap<>();
            guest.put("schoolId", null);
            assertFalse(sql(mapper, "getThree", guest).contains("= ?"));
            assertTrue(sql(mapper, "getThree", Map.of("schoolId", 1L)).contains("= ?"));
        }
    }
}
