package com.mikasa.campusrunner.mapper;

import com.mikasa.campusrunner.pojo.entity.AddressBook;
import com.mikasa.campusrunner.pojo.entity.TakeOrder;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Opt-in local MySQL verification; only a newly created isolated schema is mutated. */
@EnabledIfSystemProperty(named = "snapshot.mysql", matches = "true")
class OrderAddressSnapshotMysqlTest {
    @Test
    @SuppressWarnings("unchecked")
    void migrationAndAllOrderViewsSurviveAddressChanges() throws Exception {
        Map<String, Object> yaml = new Yaml().load(Files.readString(Path.of("src/main/resources/application.yaml")));
        Map<String, Object> datasource = (Map<String, Object>) ((Map<String, Object>) yaml.get("spring")).get("datasource");
        String configuredUrl = (String) datasource.get("url");
        assertTrue(configuredUrl.matches("jdbc:mysql://(localhost|127\\.0\\.0\\.1):[0-9]+/.*"),
                "This test is restricted to local MySQL");
        String password = String.valueOf(datasource.get("password"));
        if (password.startsWith("${") && password.endsWith("}")) {
            String[] setting = password.substring(2, password.length() - 1).split(":", 2);
            password = System.getenv().getOrDefault(setting[0], setting.length == 2 ? setting[1] : "");
        }
        String schema = "cr_snapshot_test_" + UUID.randomUUID().toString().replace("-", "");
        String baseUrl = configuredUrl.substring(0, configuredUrl.indexOf('/', "jdbc:mysql://".length()) + 1);
        var control = new DriverManagerDataSource(baseUrl + "?useSSL=false&allowPublicKeyRetrieval=true",
                String.valueOf(datasource.get("username")), password);
        try (Connection admin = control.getConnection(); Statement ddl = admin.createStatement()) {
            ddl.execute("CREATE DATABASE `" + schema + "` CHARACTER SET utf8mb4");
            try {
                var isolated = new DriverManagerDataSource(baseUrl + schema + "?useSSL=false&allowPublicKeyRetrieval=true",
                        String.valueOf(datasource.get("username")), password);
                try (Connection connection = isolated.getConnection(); Statement sql = connection.createStatement()) {
                    fixtures(sql);
                    migrate(sql);
                    assertEquals(6, scalar(sql, "SELECT COUNT(*) FROM tb_order_address_snapshot"));
                    assertEquals(1, scalar(sql, "SELECT COUNT(*) FROM tb_order_address_snapshot WHERE source='LEGACY_MISSING'"));
                    assertEquals(1, scalar(sql, "SELECT COUNT(*) FROM tb_order_address_snapshot WHERE source='LEGACY_PARTIAL'"));
                }
                Configuration configuration = new Configuration(new Environment("snapshot-test", new JdbcTransactionFactory(), isolated));
                configuration.setMapUnderscoreToCamelCase(true);
                for (String mapper : List.of("OrderMapper", "TakeOrderMapper", "OrderAddressSnapshotMapper")) {
                    String resource = "mapper/" + mapper + ".xml";
                    try (var input = getClass().getClassLoader().getResourceAsStream(resource)) {
                        new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
                    }
                }
                var factory = new SqlSessionFactoryBuilder().build(configuration);
                try (var session = factory.openSession()) {
                    var snapshots = session.getMapper(OrderAddressSnapshotMapper.class);
                    assertEquals(List.of(10L), snapshots.lockUsableAddresses(10L, 10L, 1L, 1L));
                    try (Connection competing = isolated.getConnection(); Statement update = competing.createStatement()) {
                        update.execute("SET innodb_lock_wait_timeout=1");
                        assertThrows(java.sql.SQLException.class,
                                () -> update.executeUpdate("UPDATE tb_address_book SET details='concurrent' WHERE id=10"));
                    }
                    session.getConnection().createStatement().executeUpdate("INSERT INTO tb_orders(id,pick_up_address,recive_address,user_id,category_id,status,deleted) VALUES(4,10,10,1,1,0,0)");
                    assertEquals(1, snapshots.capture(4L, 10L, "PICKUP"));
                    assertEquals(1, snapshots.capture(4L, 10L, "RECEIVE"));
                    session.commit();
                }
                try (Connection connection = isolated.getConnection(); Statement sql = connection.createStatement()) {
                    sql.executeUpdate("UPDATE tb_address_book SET details='changed', school_id=99, deleted=1 WHERE id=10");
                    sql.executeUpdate("DELETE FROM tb_address_book WHERE id=11");
                    sql.executeUpdate("UPDATE tb_school SET school_name='renamed' WHERE id=1");
                    sql.executeUpdate("DELETE FROM tb_building WHERE id=1");
                    migrate(sql); // Must preserve legacy and submission snapshots after sources change.
                    assertEquals(8, scalar(sql, "SELECT COUNT(*) FROM tb_order_address_snapshot"));
                    assertEquals(2, scalar(sql, "SELECT COUNT(*) FROM tb_order_address_snapshot WHERE source='SUBMIT'"));
                }
                try (var session = factory.openSession()) {
                    var orders = session.getMapper(OrderMapper.class);
                    var takes = session.getMapper(TakeOrderMapper.class);
                    var snapshots = session.getMapper(OrderAddressSnapshotMapper.class);
                    assertEquals("school campus type building original", orders.detail(1L).getPickUpAddress());
                    assertEquals(4, orders.getMy(1L).size());
                    assertNotNull(orders.detail(3L));
                    assertEquals("历史地址信息缺失", orders.detail(3L).getReciveAddress());
                    assertEquals(3, orders.showByTime(0, 1L).size());
                    assertEquals(3, orders.showByPrice(0, 1L).size());
                    assertEquals(3, orders.showByCategory(1L, 1L).size());
                    var ids = snapshots.findOrderIds(AddressBook.builder().schoolId(1L).buildingId(1L).build(), "PICKUP");
                    assertEquals(3, ids.size());
                    assertEquals(3, orders.showByPickUpAdd(ids).size());
                    assertEquals(3, orders.showByReciveAdd(List.of(1L, 2L, 4L)).size());
                    assertEquals(3, orders.showByDoubleAdd(ids, List.of(1L, 2L, 4L)).size());
                    assertEquals(0, orders.showByTime(0, 99L).size());
                    assertEquals("school campus type building", orders.getPublicOrderById(1L).pickUpAddress());
                    assertEquals(4, orders.listPublicOrders().size());
                    assertEquals(4, orders.listAllOrders(0, 10, null).size());
                    assertEquals(4, orders.listOrdersByStatus(List.of(0), 0, 10, null).size());
                    assertEquals("school campus type building original", orders.getAdminOrderDetail(1L).getPickUpAddress());
                    assertEquals(1, takes.getMy(2L).size());
                    assertEquals("school campus type building original", takes.getMy(2L).get(0).getPickUpAddress());
                    var takeQuery = new TakeOrder();
                    takeQuery.setUserId(2L);
                    takeQuery.setDeleted(0);
                    assertEquals(1, takes.query(takeQuery).size());
                    assertEquals(1, takes.listAllTakeOrders(0, 10, null).size());
                    assertEquals(0, takes.listUnpaidTakeOrders(0, 10, null).size());
                    session.getConnection().createStatement().executeUpdate("UPDATE tb_orders SET status=5,price=1,product_amount=0 WHERE id=1");
                    session.getConnection().createStatement().executeUpdate("UPDATE tb_take_orders SET status=2 WHERE id=1");
                    session.clearCache();
                    assertEquals(1, takes.getNoWithdrawn(2L, 1L, 5).size());
                    assertEquals(1, takes.listUnpaidTakeOrders(0, 10, null).size());
                    session.rollback();
                }
            } finally {
                ddl.execute("DROP DATABASE `" + schema + "`");
            }
        }
    }

    private static long scalar(Statement sql, String query) throws Exception {
        try (var result = sql.executeQuery(query)) { result.next(); return result.getLong(1); }
    }

    private static void migrate(Statement sql) throws Exception {
        String migration = Files.readString(Path.of("../../docs/database/order-address-snapshot-migration.sql"));
        for (String statement : migration.replaceAll("(?m)^--.*$", "").split(";")) {
            if (!statement.isBlank()) sql.execute(statement);
        }
    }

    private static void fixtures(Statement sql) throws Exception {
        for (String statement : """
            CREATE TABLE tb_school(id BIGINT PRIMARY KEY, school_name VARCHAR(50), deleted INT);
            CREATE TABLE tb_compus(id BIGINT PRIMARY KEY, school_id BIGINT, compus_name VARCHAR(50), deleted INT);
            CREATE TABLE tb_build_category(id BIGINT PRIMARY KEY, school_id BIGINT, compus_id BIGINT, name VARCHAR(50), deleted INT);
            CREATE TABLE tb_building(id BIGINT PRIMARY KEY, school_id BIGINT, compus_id BIGINT, build_category_id BIGINT, building_name VARCHAR(50), deleted INT);
            CREATE TABLE tb_address_book(id BIGINT PRIMARY KEY,user_id BIGINT,school_id BIGINT,compus_id BIGINT,build_category_id BIGINT,building_id BIGINT,details VARCHAR(100),deleted INT);
            CREATE TABLE tb_category(id BIGINT PRIMARY KEY,category_name VARCHAR(50));
            CREATE TABLE tb_user(id BIGINT PRIMARY KEY,realname VARCHAR(50),phone VARCHAR(20));
            CREATE TABLE tb_orders(id BIGINT PRIMARY KEY,pick_up_address BIGINT,recive_address BIGINT,order_number VARCHAR(50),price DECIMAL(10,2),product_amount DECIMAL(10,2),business_type VARCHAR(20),service_fee_rate DECIMAL(10,2),service_fee DECIMAL(10,2),pay_amount DECIMAL(10,2),expected_delivery_time DATETIME,delivery_time DATETIME,cancel_time DATETIME,cancel_reson VARCHAR(100),exceed_time DATETIME,gap INT,create_time DATETIME,door_access INT,user_id BIGINT,username VARCHAR(50),phone VARCHAR(20),status INT,note VARCHAR(100),category_id BIGINT,deleted INT,publisher_hidden INT DEFAULT 0);
            CREATE TABLE tb_take_orders(id BIGINT PRIMARY KEY,order_id BIGINT,user_id BIGINT,create_time DATETIME,delivery_time DATETIME,cancel_time DATETIME,cancel_reason VARCHAR(100),status INT,deleted INT);
            CREATE TABLE tb_refund_info(id BIGINT PRIMARY KEY,order_number VARCHAR(50),refund DECIMAL(10,2),total_fee DECIMAL(10,2),refund_status VARCHAR(50),refund_number VARCHAR(50),refund_id VARCHAR(50),reason VARCHAR(100),create_time DATETIME);
            CREATE TABLE tb_payment_log(id BIGINT PRIMARY KEY,order_number VARCHAR(50),transaction_id VARCHAR(50),trade_state VARCHAR(50),total INT,service_fee INT,payer_openid VARCHAR(50),success_time DATETIME);
            INSERT INTO tb_school VALUES(1,'school',0);
            INSERT INTO tb_compus VALUES(1,1,'campus',0);
            INSERT INTO tb_build_category VALUES(1,1,1,'type',0);
            INSERT INTO tb_building VALUES(1,1,1,1,'building',0);
            INSERT INTO tb_address_book VALUES(10,1,1,1,1,1,'original',0),(11,1,1,1,1,1,'soft deleted',1),(12,1,1,1,1,999,'partial',0);
            INSERT INTO tb_category VALUES(1,'errand');
            INSERT INTO tb_user VALUES(1,'publisher','13800138000'),(2,'runner','13900139000');
            INSERT INTO tb_orders(id,pick_up_address,recive_address,user_id,category_id,status,deleted) VALUES(1,10,10,1,1,0,0),(2,11,11,1,1,0,0),(3,12,999,1,1,0,0);
            INSERT INTO tb_take_orders(id,order_id,user_id,status,deleted) VALUES(1,1,2,0,0);
            """.split(";")) {
            if (!statement.isBlank()) sql.execute(statement);
        }
    }
}
