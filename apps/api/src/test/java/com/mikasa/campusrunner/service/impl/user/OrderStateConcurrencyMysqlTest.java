package com.mikasa.campusrunner.service.impl.user;

import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.OrderException;
import com.mikasa.campusrunner.common.exception.SecondHandException;
import com.mikasa.campusrunner.mapper.*;
import com.mikasa.campusrunner.pojo.dto.*;
import com.mikasa.campusrunner.pojo.entity.*;
import com.mikasa.campusrunner.pojo.entity.Order;
import com.mikasa.campusrunner.pojo.vo.UserVO;
import com.mikasa.campusrunner.service.MediaAssetService;
import com.mikasa.campusrunner.service.OrderCancellationService;
import com.mikasa.campusrunner.service.user.*;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.jdbc.datasource.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import org.yaml.snakeyaml.Yaml;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real InnoDB contention in a disposable local schema; never writes the configured application schema. */
@EnabledIfSystemProperty(named = "orders.mysql", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrderStateConcurrencyMysqlTest {
    DriverManagerDataSource control, db;
    String schema;
    TransactionTemplate tx;
    OrderMapper orders;
    TakeOrderMapper takes;
    SecondHandOrderMapper secondOrders;
    SecondHandProductMapper products;
    TakeOrderServiceImpl runner;
    OrderServiceImpl publisher;
    SecondHandServiceImpl second;
    OrderCancellationService cancellation;

    @BeforeAll @SuppressWarnings("unchecked") void setup() throws Exception {
        Map<String,Object> yaml = new Yaml().load(Files.readString(Path.of("src/main/resources/application.yaml")));
        Map<String,Object> config = (Map<String,Object>) ((Map<String,Object>) yaml.get("spring")).get("datasource");
        String url = (String) config.get("url");
        assertTrue(url.matches("jdbc:mysql://(localhost|127\\.0\\.0\\.1):[0-9]+/.*"), "Local MySQL only");
        String password = String.valueOf(config.get("password"));
        if (password.startsWith("${") && password.endsWith("}")) {
            String[] setting = password.substring(2, password.length()-1).split(":",2);
            password = System.getenv().getOrDefault(setting[0], setting.length == 2 ? setting[1] : "");
        }
        String base = url.substring(0, url.indexOf('/', "jdbc:mysql://".length())+1);
        String options = "?useSSL=false&allowPublicKeyRetrieval=true&sessionVariables=innodb_lock_wait_timeout=5";
        control = new DriverManagerDataSource(base+options, String.valueOf(config.get("username")), password);
        schema = "cr_order_race_"+UUID.randomUUID().toString().replace("-", "");
        try (var c=control.getConnection(); var sql=c.createStatement()) { sql.execute("CREATE DATABASE `"+schema+"`"); }
        db = new DriverManagerDataSource(base+schema+options, String.valueOf(config.get("username")), password);
        try (var c=db.getConnection(); var sql=c.createStatement()) {
            table(sql,"tb_orders",Order.class); table(sql,"tb_take_orders",TakeOrder.class);
            table(sql,"tb_second_hand_order",SecondHandOrder.class); table(sql,"tb_second_hand_product",SecondHandProduct.class);
        }
        var configuration = new Configuration(new Environment("race",new SpringManagedTransactionFactory(),db));
        configuration.setMapUnderscoreToCamelCase(true);
        for (String mapper : List.of("OrderAddressSnapshotMapper","OrderMapper","TakeOrderMapper","SecondHandProductMapper","SecondHandOrderMapper")) {
            String resource="mapper/"+mapper+".xml";
            try (var input=getClass().getClassLoader().getResourceAsStream(resource)) {
                new XMLMapperBuilder(input,configuration,resource,configuration.getSqlFragments()).parse();
            }
        }
        var session = new SqlSessionTemplate(new SqlSessionFactoryBuilder().build(configuration));
        orders=session.getMapper(OrderMapper.class); takes=session.getMapper(TakeOrderMapper.class);
        secondOrders=session.getMapper(SecondHandOrderMapper.class); products=session.getMapper(SecondHandProductMapper.class);
        var manager=new DataSourceTransactionManager(db); tx=new TransactionTemplate(manager);
        var users=mock(UserMapper.class); when(users.getById(anyLong())).thenReturn(UserVO.builder().authentication(1).build());
        runner=new TakeOrderServiceImpl(); publisher=new OrderServiceImpl(); second=new SecondHandServiceImpl();
        ReflectionTestUtils.setField(runner,"orderMapper",orders); ReflectionTestUtils.setField(runner,"takeOrderMapper",takes);
        ReflectionTestUtils.setField(runner,"userMapper",users); ReflectionTestUtils.setField(runner,"messages",mock(MessageSendService.class));
        ReflectionTestUtils.setField(runner,"mediaAssetService",mock(MediaAssetService.class));
        ReflectionTestUtils.setField(publisher,"orderMapper",orders);
        ReflectionTestUtils.setField(second,"orderMapper",secondOrders); ReflectionTestUtils.setField(second,"productMapper",products);
        ReflectionTestUtils.setField(second,"userMapper",users); ReflectionTestUtils.setField(second,"configMapper",mock(AdminSystemConfigMapper.class));
        ReflectionTestUtils.setField(second,"subscriptions",mock(SecondHandSubscriptionService.class));
        ReflectionTestUtils.setField(second,"transactionManager",manager); ReflectionTestUtils.setField(second,"mockPaymentEnabled",true);
        cancellation=new OrderCancellationService(orders,mock(RefundInfoService.class),mock(WeChatPayService.class),
                mock(com.mikasa.campusrunner.common.utils.WeChatPayUtil.class),manager);
    }

    @AfterAll void cleanup() throws Exception {
        if (control!=null && schema!=null) try (var c=control.getConnection();var sql=c.createStatement()) {
            sql.execute("DROP DATABASE IF EXISTS `"+schema+"`");
        }
    }

    @BeforeEach void fixtures() throws Exception {
        sql("DELETE FROM tb_take_orders", "DELETE FROM tb_orders", "DELETE FROM tb_second_hand_order", "DELETE FROM tb_second_hand_product",
                "INSERT INTO tb_orders(id,order_number,user_id,status,deleted,gap,pay_amount) VALUES(1,'O1',10,0,0,30,0)",
                "INSERT INTO tb_second_hand_product(id,seller_id,status,deleted) VALUES(1,20,2,0)",
                "INSERT INTO tb_second_hand_order(id,order_number,product_id,buyer_id,seller_id,trade_mode,status,deleted) VALUES(1,'SH1',1,10,20,'OFFLINE',1,0)");
    }

    @Test void cancellationWinningTakeRaceLeavesNoTakeRecord() throws Exception {
        race(10,()->orders.getByIdForUpdate(1L),()->cancellation.cancel(1L,"取消",10L),
                20,()->runner.take(1L),OrderException.class);
        assertEquals(4,orders.getById(1L).getStatus()); assertNull(takes.getByOrderId(1L));
    }

    @Test void takingWinningCancellationRaceKeepsBothRecords() throws Exception {
        race(20,()->orders.getByIdForUpdate(1L),()->runner.take(1L),
                10,()->cancellation.cancel(1L,"取消",10L),OrderException.class);
        assertEquals(1,orders.getById(1L).getStatus()); assertEquals(0,takes.getByOrderId(1L).getStatus());
    }

    @Test void confirmationCannotBeOverwrittenByRepeatedDeliveryOrPickup() throws Exception {
        sql("UPDATE tb_orders SET status=3 WHERE id=1",
                "INSERT INTO tb_take_orders(id,order_id,user_id,status,deleted) VALUES(1,1,20,2,0)");
        race(10,()->orders.getByIdForUpdate(1L),()->publisher.confirm(1L),20,()->runner.updateStatus(delivery(2)),null);
        assertEquals(5,orders.getById(1L).getStatus()); assertEquals(2,takes.getById(1L).getStatus());
        as(20,()->assertThrows(com.mikasa.campusrunner.common.exception.TakeOrderException.class,()->runner.updateStatus(delivery(1))));
    }

    @Test void pickupAndDeliverySerializeBothRunnerRecords() throws Exception {
        sql("UPDATE tb_orders SET status=1 WHERE id=1",
                "INSERT INTO tb_take_orders(id,order_id,user_id,status,deleted) VALUES(1,1,20,0,0)");
        race(20,()->orders.getByIdForUpdate(1L),()->runner.updateStatus(delivery(1)),
                20,()->runner.updateStatus(delivery(2)),null);
        assertEquals(3,orders.getById(1L).getStatus()); assertEquals(2,takes.getById(1L).getStatus());
        assertNotNull(orders.getById(1L).getDeliveryTime()); assertNotNull(takes.getById(1L).getDeliveryTime());
    }

    @Test void oldRefundCannotReleaseNewBuyersProduct() throws Exception {
        sql("UPDATE tb_second_hand_order SET status=6 WHERE id=1",
                "INSERT INTO tb_second_hand_order(id,product_id,status,deleted) VALUES(2,1,1,0)");
        Integer released = tx.execute(s->products.releaseAfterRefund(1L,1L));
        assertEquals(0,released.intValue());
        assertEquals(2,products.getById(1L).getStatus()); assertEquals(1,secondOrders.getById(2L).getStatus());
    }

    @Test void offlineCancellationWinningDeliveryRaceReleasesProduct() throws Exception {
        race(10,()->products.getByIdForUpdate(1L),()->second.cancelOrder(1L,"取消"),
                20,()->second.markDelivered(1L),SecondHandException.class);
        assertEquals(4,secondOrders.getById(1L).getStatus()); assertEquals(0,products.getById(1L).getStatus());
    }

    @Test void offlineDeliveryWinningCancellationRaceKeepsProductTrading() throws Exception {
        race(20,()->products.getByIdForUpdate(1L),()->second.markDelivered(1L),
                10,()->second.cancelOrder(1L,"取消"),SecondHandException.class);
        assertEquals(2,secondOrders.getById(1L).getStatus()); assertEquals(2,products.getById(1L).getStatus());
    }

    @Test void confirmedOfflineOrderCannotBeReopenedByAdmin() throws Exception {
        sql("UPDATE tb_second_hand_order SET status=2 WHERE id=1");
        var reopen=new SecondHandStatusDTO(); reopen.setStatus(1);
        race(10,()->products.getByIdForUpdate(1L),()->second.confirmOrder(1L),
                20,()->second.adminUpdateOrderStatus(1L,reopen),SecondHandException.class);
        assertEquals(3,secondOrders.getById(1L).getStatus()); assertEquals(3,products.getById(1L).getStatus());
    }

    @Test void productFailureRollsBackOrderCompletion() throws Exception {
        sql("UPDATE tb_second_hand_order SET status=2 WHERE id=1");
        var failing=spy(products); doThrow(new IllegalStateException("test write failure")).when(failing).markSold(1L);
        ReflectionTestUtils.setField(second,"productMapper",failing);
        try { as(10,()->assertThrows(IllegalStateException.class,()->tx.executeWithoutResult(s->second.confirmOrder(1L)))); }
        finally { ReflectionTestUtils.setField(second,"productMapper",products); }
        assertEquals(2,secondOrders.getById(1L).getStatus()); assertEquals(2,products.getById(1L).getStatus());
    }

    @Test void delayedPaymentAndTransferUpdatesCannotRegressFinalState() throws Exception {
        sql("UPDATE tb_orders SET status=6 WHERE id=1");
        orders.updateStatusByOrderNumber("O1",0); orders.updateStatusByOrderNumber("O1",7);
        orders.updateStatusByOrderNumber("O1",-2); assertEquals(6,orders.getById(1L).getStatus());
        sql("UPDATE tb_second_hand_product SET status=3 WHERE id=1");
        assertEquals(0,products.markTrading(1L)); assertEquals(3,products.getById(1L).getStatus());
    }

    // Force a real waiter at the lock, then commit the winner. No mock substitutes for row locks.
    void race(long firstUser,Runnable lock,Runnable first,long secondUser,Runnable contender,
              Class<? extends Throwable> expected) throws Exception {
        var locked=new CountDownLatch(1); var release=new CountDownLatch(1); var started=new CountDownLatch(1);
        try (var pool=Executors.newFixedThreadPool(2)) {
            var winner=pool.submit(()->as(firstUser,()->tx.executeWithoutResult(s->{ lock.run(); locked.countDown(); await(release); first.run(); })));
            assertTrue(locked.await(5,TimeUnit.SECONDS));
            var waiter=pool.submit(()->as(secondUser,()->{ started.countDown();
                if(expected==null) tx.executeWithoutResult(s->contender.run());
                else assertThrows(expected,()->tx.executeWithoutResult(s->contender.run()));
            }));
            try { assertTrue(started.await(5,TimeUnit.SECONDS)); assertThrows(TimeoutException.class,()->waiter.get(200,TimeUnit.MILLISECONDS)); }
            finally { release.countDown(); }
            winner.get(10,TimeUnit.SECONDS); waiter.get(10,TimeUnit.SECONDS);
        } finally { release.countDown(); }
    }
    static void await(CountDownLatch latch) { try { if(!latch.await(5,TimeUnit.SECONDS)) throw new AssertionError("barrier timeout"); }
        catch(InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); } }
    static void as(long user,Runnable operation) { BaseContext.setCurrentId(user); try { operation.run(); } finally { BaseContext.removeCurrentId(); } }
    static TakeOrderUpdateStatusDTO delivery(int status) { var dto=new TakeOrderUpdateStatusDTO(); dto.setId(1L); dto.setStatus(status); dto.setImageAssetId(1L); return dto; }
    void sql(String... statements) throws Exception { try(var c=db.getConnection();var sql=c.createStatement()) { for(String statement:statements) sql.execute(statement); } }
    static void table(Statement sql,String name,Class<?> entity) throws Exception {
        var columns=new ArrayList<String>();
        for(var field:entity.getDeclaredFields()) {
            if(Modifier.isStatic(field.getModifiers())) continue;
            String column=field.getName().replaceAll("([a-z])([A-Z])","$1_$2").toLowerCase(Locale.ROOT);
            String type=field.getType()==Long.class?"BIGINT":field.getType()==Integer.class?"INT":
                    field.getType()==BigDecimal.class?"DECIMAL(12,2)":field.getType()==LocalDateTime.class?"DATETIME":"VARCHAR(500)";
            columns.add("`"+column+"` "+type+(column.equals("id")?" PRIMARY KEY AUTO_INCREMENT":""));
        }
        sql.execute("CREATE TABLE "+name+"("+String.join(",",columns)+") ENGINE=InnoDB");
    }
}
