package com.mikasa.campusrunner.mapper;

import com.mikasa.campusrunner.common.constant.SecondHandConstant;
import com.mikasa.campusrunner.pojo.entity.SecondHandOrder;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SecondHandOrderMapperXmlTest {

    @Test
    void cancellationAndCallbacksShareLockedOrderReads() throws IOException {
        String byId = statement("getByIdForUpdate").getBoundSql(Map.of("id", 1L)).getSql().toLowerCase();
        String byNumber = statement("getByOrderNumberForUpdate").getBoundSql(Map.of("orderNumber", "SH1")).getSql().toLowerCase();
        assertTrue(byId.contains("for update"));
        assertTrue(byNumber.contains("for update"));
    }

    @Test
    void compensationQueriesBindCutoffAndOnlyScanHistoricalOnlineOrders() throws IOException {
        for (String id : java.util.List.of("listRefundRecovery", "listCanceledPaymentRecovery")) {
            var sql = statement(id).getBoundSql(Map.of("cutoff", LocalDateTime.now()));
            assertTrue(sql.getSql().contains("trade_mode = 'ONLINE'"));
            org.junit.jupiter.api.Assertions.assertEquals("cutoff", sql.getParameterMappings().get(0).getProperty());
        }
    }

    @Test
    void insertsTradeModeAndDefendsNonNullTransferAttempt() throws IOException {
        SecondHandOrder order = SecondHandOrder.builder()
                .tradeMode(SecondHandConstant.TRADE_MODE_OFFLINE)
                .build();

        String sql = statement("insert").getBoundSql(order).getSql()
                .replaceAll("\\s+", " ")
                .trim();

        assertTrue(sql.contains("trade_mode"));
        assertTrue(sql.contains("COALESCE(?, 0)"));
    }

    @Test
    void scheduledOnlinePaymentJobsExcludeOfflineOrders() throws IOException {
        String unpaidSql = statement("listUnpaidTimeout")
                .getBoundSql(Map.of("time", LocalDateTime.now()))
                .getSql()
                .replaceAll("\\s+", " ")
                .trim();
        String autoConfirmSql = statement("listAutoConfirm")
                .getBoundSql(Map.of("now", LocalDateTime.now()))
                .getSql()
                .replaceAll("\\s+", " ")
                .trim();

        assertTrue(unpaidSql.contains("trade_mode = 'ONLINE'"));
        assertTrue(autoConfirmSql.contains("trade_mode = 'ONLINE'"));
    }

    private MappedStatement statement(String id) throws IOException {
        Configuration configuration = new Configuration();
        String resource = "mapper/SecondHandOrderMapper.xml";
        try (InputStream input = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        return configuration.getMappedStatement("com.mikasa.campusrunner.mapper.SecondHandOrderMapper." + id);
    }
}
