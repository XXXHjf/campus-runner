package com.mikasa.campusrunner.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SecondHandBargainMapperXmlTest {
    private String sql(String mapper, String statement, Map<String, Object> params) throws IOException {
        Configuration configuration = new Configuration();
        String resource = "mapper/" + mapper + ".xml";
        try (var input = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        return configuration.getMappedStatement("com.mikasa.campusrunner.mapper." + mapper + "." + statement)
                .getBoundSql(params).getSql().replaceAll("\\s+", " ").trim();
    }

    @Test
    void stateTransitionOnlyUpdatesPendingUndeletedBargain() throws IOException {
        String query = sql("SecondHandBargainMapper", "updatePendingStatus", Map.of("id", 7L, "status", 2));
        assertTrue(query.contains("set status = ?"));
        assertTrue(query.contains("where id = ? and deleted = 0 and status = 0"));
    }

    @Test
    void closingBargainsPreservesAcceptedAndRejectedRecords() throws IOException {
        String query = sql("SecondHandBargainMapper", "expirePendingByProduct", Map.of("productId", 10L));
        assertTrue(query.contains("set status = 3"));
        assertTrue(query.contains("where product_id = ? and deleted = 0 and status = 0"));
    }

    @Test
    void productReadLocksCurrentRowBeforeBargainMutation() throws IOException {
        String query = sql("SecondHandProductMapper", "getByIdForUpdate", Map.of("id", 10L));
        assertTrue(query.contains("where id = ? and deleted = 0 for update"));
    }
}
