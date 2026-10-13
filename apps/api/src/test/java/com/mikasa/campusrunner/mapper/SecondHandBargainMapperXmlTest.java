package com.mikasa.campusrunner.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SecondHandBargainMapperXmlTest {
    @Test
    void replacementScopesToBuyerAndProductAndPreservesTerminalAndLinkedQuotes() throws IOException {
        var params = Map.<String, Object>of("buyerId", 200L, "productId", 10L);
        String update = sql("SecondHandBargainMapper", "expirePendingByBuyerAndProduct", params);
        assertTrue(update.contains("buyer_id = ? and product_id = ?"));
        assertTrue(update.contains("deleted = 0 and status = 0"));
        assertTrue(update.contains("not exists (select 1 from tb_second_hand_order linked"));
        String read = sql("SecondHandBargainMapper", "listActiveByBuyerAndProductForUpdate", params);
        assertTrue(read.contains("b.buyer_id = ? and b.product_id = ?"));
        assertTrue(read.contains("b.deleted = 0 and b.status in (0, 1)"));
        assertTrue(read.contains("subsequent.create_time >= b.create_time"));
        assertTrue(read.endsWith("order by b.id desc for update"));
    }

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

    @Test
    void closingAcceptedQuotesPreservesEveryQuoteThatAlreadyHasAnOrder() throws IOException {
        String query = sql("SecondHandBargainMapper", "expireAcceptedWithoutOrder", Map.of("productId", 10L));
        assertTrue(query.contains("left join tb_second_hand_order o on o.bargain_id = b.id"));
        assertTrue(query.contains("b.status = 1 and o.id is null"));
        assertTrue(query.contains("b.deleted = 0"));
    }

    @Test
    void withdrawalAndAdminReadCurrentBargainUnderRowLock() throws IOException {
        String query = sql("SecondHandBargainMapper", "getByIdForUpdate", Map.of("id", 7L));
        assertTrue(query.contains("where id = ? and deleted = 0 for update"));
        String withdrawal = sql("SecondHandBargainMapper", "updatePendingStatus", Map.of("id", 7L, "status", 4));
        assertTrue(withdrawal.contains("where id = ? and deleted = 0 and status = 0"));
    }

    @Test
    void recipientsExcludeLinkedOrdersAndLockQuotesBeforeTransition() throws IOException {
        String query = sql("SecondHandBargainMapper", "listExpirableForUpdate", Map.of("productId", 10L));
        assertTrue(query.contains("b.status in (0, 1)"));
        assertTrue(query.contains("not exists (select 1 from tb_second_hand_order linked where linked.bargain_id = b.id)"));
        assertTrue(query.endsWith("order by b.id for update"));
    }

    @Test
    void rejectionSavesFeedbackOnlyWhileQuoteIsPending() throws IOException {
        String query = sql("SecondHandBargainMapper", "rejectPending", Map.of("id", 7L, "reasonCode", "CUSTOM", "note", "谢谢"));
        assertTrue(query.contains("set status = 2, reject_reason_code = ?, reject_note = ?"));
        assertTrue(query.contains("where id = ? and deleted = 0 and status = 0"));
    }

    @Test
    void listAndFilterUseEffectiveStatusForLegacyUnavailableProducts() throws IOException {
        var dto = new com.mikasa.campusrunner.pojo.dto.BargainPageDTO();
        dto.setStatus(3);
        dto.getOffset();
        String query = sql("SecondHandBargainMapper", "listMinePage", Map.of("userId", 10L, "query", dto));
        assertTrue(query.contains("p.id is null or p.deleted != 0 or p.status != 0"));
        assertTrue(query.contains("subsequent.create_time >= b.create_time"));
        assertTrue(query.contains("then 3 else b.status end as status"));
        assertTrue(query.contains("then 3 else b.status end ) = ?"));
    }
}
