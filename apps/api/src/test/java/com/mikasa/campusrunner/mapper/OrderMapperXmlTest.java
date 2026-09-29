package com.mikasa.campusrunner.mapper;

import com.mikasa.campusrunner.pojo.entity.Order;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OrderMapperXmlTest {
    @Test
    void amountAndRateBindToTheirOwnProperties() throws Exception {
        BoundSql sql = statement("update", Order.builder().id(1L)
                .serviceFeeRate(new BigDecimal("0.03"))
                .serviceFee(new BigDecimal("1.50")).build());
        assertEquals("update tb_orders SET service_fee_rate = ?, service_fee = ? where id = ?", normalized(sql));
        assertEquals(List.of("serviceFeeRate", "serviceFee", "id"), properties(sql));
    }

    @Test
    void rateAloneDoesNotOverwriteFee() throws Exception {
        BoundSql sql = statement("update", Order.builder().id(1L)
                .serviceFeeRate(new BigDecimal("0.03")).build());
        assertEquals("update tb_orders SET service_fee_rate = ? where id = ?", normalized(sql));
        assertEquals(List.of("serviceFeeRate", "id"), properties(sql));
    }

    @Test
    void feeAloneCanBeUpdatedIncludingZero() throws Exception {
        for (BigDecimal fee : List.of(new BigDecimal("1.50"), BigDecimal.ZERO)) {
            BoundSql sql = statement("update", Order.builder().id(1L).serviceFee(fee).build());
            assertEquals("update tb_orders SET service_fee = ? where id = ?", normalized(sql));
            assertEquals(List.of("serviceFee", "id"), properties(sql));
        }
    }

    @Test
    void stateChangesLeaveAllAmountColumnsUntouched() throws Exception {
        for (int status : new int[]{-4, -3, -2, 1, 2, 3, 4, 5}) {
            BoundSql sql = statement("update", Order.builder().id(1L).status(status)
                    .cancelReson("取消").cancelTime(LocalDateTime.now()).build());
            assertEquals("update tb_orders SET cancel_time = ?, cancel_reson = ?, status = ? where id = ?", normalized(sql));
            assertEquals(List.of("cancelTime", "cancelReson", "status", "id"), properties(sql));
        }
        BoundSql refund = statement("updateStatusByOrderNumber", Map.of("orderNumber", "ORDER1", "status", -3));
        assertEquals("update tb_orders set status = ? where order_number = ?", normalized(refund));
        assertEquals(List.of("status", "orderNumber"), properties(refund));
    }

    @Test
    void adminListsExposeOnlyLatestRefundAndConvertFenToYuan() throws Exception {
        for (String id : List.of("listAllOrders", "listOrdersByStatus")) {
            Object parameters = id.equals("listAllOrders") ? Map.of("offset", 0, "limit", 20)
                    : Map.of("offset", 0, "limit", 20, "statuses", List.of(-2, -4));
            String sql = normalized(statement(id, parameters));
            assertTrue(sql.contains("SELECT MAX(r.id) FROM tb_refund_info r"));
            assertTrue(sql.contains("ri.refund / 100.0 AS refund_amount, ri.refund_status"));
        }
    }

    private BoundSql statement(String id, Object parameter) throws Exception {
        Configuration configuration = new Configuration();
        String resource = "mapper/OrderMapper.xml";
        try (InputStream input = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        return configuration.getMappedStatement("com.mikasa.campusrunner.mapper.OrderMapper." + id)
                .getBoundSql(parameter);
    }

    private String normalized(BoundSql sql) {
        return sql.getSql().replaceAll("\\s+", " ").trim();
    }

    private List<String> properties(BoundSql sql) {
        return sql.getParameterMappings().stream().map(mapping -> mapping.getProperty()).toList();
    }
}
