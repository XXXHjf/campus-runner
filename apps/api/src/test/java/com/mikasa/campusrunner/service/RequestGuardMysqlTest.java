package com.mikasa.campusrunner.service;

import com.mikasa.campusrunner.common.exception.RequestLimitException;
import com.mikasa.campusrunner.common.properties.RequestGuardProperties;
import com.mikasa.campusrunner.mapper.RequestGuardMapper;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.jdbc.datasource.*;
import org.yaml.snakeyaml.Yaml;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Writes only an isolated disposable schema on a verified loopback MySQL. */
@EnabledIfSystemProperty(named="guard.mysql",matches="true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RequestGuardMysqlTest {
    DriverManagerDataSource control, db;
    String schema;
    RequestGuardMapper mapper;
    RequestGuardService one, two;
    RequestGuardProperties p;
    com.mikasa.campusrunner.mapper.AddressBookMapper addresses;
    com.mikasa.campusrunner.mapper.BuildingMapper buildings;
    @BeforeAll @SuppressWarnings("unchecked") void setup() throws Exception {
        Map<String,Object> yaml=new Yaml().load(Files.readString(Path.of("src/main/resources/application.yaml")));
        var config=(Map<String,Object>)((Map<String,Object>)yaml.get("spring")).get("datasource");
        String url=String.valueOf(config.get("url"));
        assertTrue(url.matches("jdbc:mysql://(localhost|127\\.0\\.0\\.1):[0-9]+/.*"),"Local MySQL only");
        String password=String.valueOf(config.get("password"));
        if(password.startsWith("${") && password.endsWith("}")) {
            var parts=password.substring(2,password.length()-1).split(":",2);
            password=System.getenv().getOrDefault(parts[0],parts.length==2?parts[1]:"");
        }
        String base=url.substring(0,url.indexOf('/',"jdbc:mysql://".length())+1);
        String opts="?useSSL=false&allowPublicKeyRetrieval=true&sessionVariables=innodb_lock_wait_timeout=5";
        control=new DriverManagerDataSource(base+opts,String.valueOf(config.get("username")),password);
        schema="cr_guard_test_"+UUID.randomUUID().toString().replace("-","");
        try(var c=control.getConnection();var s=c.createStatement()){s.execute("CREATE DATABASE `"+schema+"`");}
        db=new DriverManagerDataSource(base+schema+opts,String.valueOf(config.get("username")),password);
        String ddl=Files.readString(Path.of("../../docs/database/request-guard.sql")).replaceAll("(?m)^--.*$","");
        try(var c=db.getConnection();var s=c.createStatement()){
            for(String sql:ddl.split(";"))if(!sql.isBlank())s.execute(sql);
            s.execute("create table tb_media_asset(id bigint primary key auto_increment, owner_type varchar(20),owner_id bigint,purpose varchar(50),status varchar(30),file_size bigint) engine=InnoDB");
            s.execute("create table tb_school(id bigint primary key,school_name varchar(50),number_id bigint,deleted int)");
            s.execute("create table tb_compus(id bigint primary key,school_id bigint,compus_name varchar(50),number_id bigint,deleted int)");
            s.execute("create table tb_build_category(id bigint primary key,school_id bigint,compus_id bigint,name varchar(50),number_id bigint,deleted int)");
            s.execute("create table tb_building(id bigint primary key,school_id bigint,compus_id bigint,build_category_id bigint,building_name varchar(50),number_id bigint,deleted int)");
            s.execute("create table tb_address_book(id bigint primary key,school_id bigint,compus_id bigint,build_category_id bigint,building_id bigint,details varchar(50),label varchar(50),user_id bigint,is_default int,deleted int)");
            s.execute("insert into tb_school values(1,'学校',1010000000000,0)");
            s.execute("insert into tb_compus values(1,1,'校区',1010010000000,0),(2,1,'其他校区',1010020000000,0)");
            s.execute("insert into tb_build_category values(1,1,1,'楼栋',1010010010000,0)");
            for(int i=1;i<=65;i++) {
                s.execute("insert into tb_building values("+i+",1,1,1,'楼',"+(1010010010000L+i)+",0)");
                s.execute("insert into tb_address_book values("+i+",1,1,1,1,'详情','标签',7,"+(i==1?1:0)+",0)");
            }
            s.execute("insert into tb_building values(99,1,2,1,'别校区楼',1010020010001,0)");
            s.execute("insert into tb_address_book values(99,1,1,1,1,'他人详情','标签',8,1,0)");
        }
        var configuration=new Configuration(new Environment("guard",new SpringManagedTransactionFactory(),db));
        configuration.addMapper(RequestGuardMapper.class);
        configuration.setMapUnderscoreToCamelCase(true);
        for(String name:List.of("AddressBookMapper","BuildingMapper")) {
            String resource="mapper/"+name+".xml";
            try(var input=getClass().getClassLoader().getResourceAsStream(resource)){
                new org.apache.ibatis.builder.xml.XMLMapperBuilder(input,configuration,resource,configuration.getSqlFragments()).parse();
            }
        }
        var sessions=new SqlSessionTemplate(new SqlSessionFactoryBuilder().build(configuration));
        mapper=sessions.getMapper(RequestGuardMapper.class);
        addresses=sessions.getMapper(com.mikasa.campusrunner.mapper.AddressBookMapper.class);
        buildings=sessions.getMapper(com.mikasa.campusrunner.mapper.BuildingMapper.class);
        p=new RequestGuardProperties();p.setEnabled(true);
        one=new RequestGuardService(p,mapper,new DataSourceTransactionManager(db));
        two=new RequestGuardService(p,mapper,new DataSourceTransactionManager(db));
    }
    @AfterAll void cleanup() throws Exception {
        if(control!=null && schema!=null)try(var c=control.getConnection();var s=c.createStatement()){s.execute("DROP DATABASE IF EXISTS `"+schema+"`");}
    }
    int parallel(java.util.function.IntConsumer action) throws Exception {
        var pool=Executors.newFixedThreadPool(12);var start=new CountDownLatch(1);
        var results=new ArrayList<Future<Boolean>>();
        try {
            for(int i=0;i<30;i++){final int index=i;results.add(pool.submit(()->{start.await();try{action.accept(index);return true;}catch(RequestLimitException e){return false;}}));}
            start.countDown();int success=0;for(var future:results)if(future.get(20,TimeUnit.SECONDS))success++;return success;
        } finally {pool.shutdownNow();}
    }
    @Test void atomicLimitIsSharedAcrossInstancesAndServiceRecreation() throws Exception {
        assertEquals(10,parallel(i->(i%2==0?one:two).check("READ:1",3600,10)));
        var restarted=new RequestGuardService(p,mapper,new DataSourceTransactionManager(db));
        assertThrows(RequestLimitException.class,()->restarted.check("READ:1",3600,10));
        two.check("READ:2",3600,10);
    }
    @Test void uploadLeasesHaveSharedConcurrencyAndRelease() throws Exception {
        var leases=new ConcurrentLinkedQueue<String>();
        assertEquals(3,parallel(i->leases.add((i%2==0?one:two).acquireUpload("USER:9"))));
        one.releaseUpload(leases.remove());assertNotNull(two.acquireUpload("USER:9"));
        assertNotNull(one.acquireUpload("ADMIN:9"));
    }
    @Test void dailyByteBudgetChargesFailedAttemptsAndKeepsEvidenceSeparate() {
        one.atomic(()->{one.spend("USER:1:DAILY:CONTENT",86400,200,80,100);return null;});
        assertThrows(RequestLimitException.class,()->two.atomic(()->{two.spend("USER:1:DAILY:CONTENT",86400,200,30,100);return null;}));
        two.atomic(()->{two.spend("USER:1:DAILY:EVIDENCE",86400,200,30,100);return null;});
    }
    @Test void ipBucketHasAtomicBurstLimit() throws Exception {
        p.setIpBurst(100);p.setIpPerSecond(1);
        // One serialized short transaction spends all tokens at the same database clock second.
        one.atomic(()->{long now=mapper.nowSeconds();mapper.ensure("IP:burst",-2,3,now);
            for(int i=0;i<3;i++)assertEquals(1,mapper.takeToken("IP:burst",3,1,now));
            assertEquals(0,mapper.takeToken("IP:burst",3,1,now));
            assertEquals(1,mapper.takeToken("IP:burst",3,1,now+1));return null;});
    }
    @Test void ownerLockMakesStorageAdmissionAtomicAndPendingDeletionStillCounts() throws Exception {
        assertEquals(4,parallel(i->one.atomic(()->{
            one.lockOwner("USER:21");var used=one.unsettled("USER",21,List.of("ORDER_IMAGE"));
            if(((Number)used.get("resource_count")).intValue()>=4)throw new RequestLimitException(5);
            try(var c=org.springframework.jdbc.datasource.DataSourceUtils.getConnection(db).createStatement()){
                c.execute("insert into tb_media_asset(owner_type,owner_id,purpose,status,file_size) values('USER',21,'ORDER_IMAGE','PENDING_DELETE',1024)");
            } catch(java.sql.SQLException error){throw new IllegalStateException(error);}
            return null;
        })));
        assertEquals(4096L,((Number)one.unsettled("USER",21,List.of("ORDER_IMAGE")).get("resource_bytes")).longValue());
    }
    @Test void realAddressSqlPaginatesWithAuthorizationAndIndependentDefaultAndTotal() {
        var page=new com.mikasa.campusrunner.pojo.dto.ListPageDTO();page.setPageSize(20);
        var seen=new java.util.HashSet<Long>();
        for(int i=1;i<=4;i++){page.setPage(i);var rows=addresses.showPage(7L,page);assertTrue(rows.size()<=20);for(var row:rows)assertTrue(seen.add(row.getId()));}
        assertEquals(65,seen.size());assertFalse(seen.contains(99L));
        var summary=addresses.summary(7L);assertEquals(65,((Number)summary.get("total")).intValue());assertEquals(1,((Number)summary.get("defaultCount")).intValue());
        var filter=new com.mikasa.campusrunner.pojo.entity.AddressBook();filter.setUserId(7L);filter.setIsDefault(1);page.setPage(1);page.setPageSize(1);
        assertEquals(1L,addresses.queryPage(filter,page).get(0).getId());filter.setId(99L);assertTrue(addresses.queryPage(filter,page).isEmpty());
    }
    @Test void realBuildingSqlPagesOnlyTheChosenCampus() {
        var query=new com.mikasa.campusrunner.pojo.dto.AddressTreeQueryDTO();query.setCompusNumberId(1010010000000L);query.setPageSize(50);
        assertEquals(50,buildings.getThreePage(1L,query).size());query.setPage(2);assertEquals(15,buildings.getThreePage(1L,query).size());
        assertTrue(buildings.getThreePage(2L,query).isEmpty());
    }
    @Test void expiryRecoversCrashedLeasesAndHeartbeatNeverResurrectsAnExpiredLease() throws Exception {
        String lease=one.acquireUpload("USER:expired");
        try(var c=db.getConnection();var s=c.prepareStatement("update tb_upload_lease set expires_at=unix_timestamp()-1 where lease_id=?")) {
            s.setString(1,lease);s.executeUpdate();
        }
        one.renewLiveUploads();assertEquals(0,mapper.activeLease(lease));
        assertThrows(RequestLimitException.class,one::checkUploadLease);
        assertNotNull(two.acquireUpload("USER:expired"));one.releaseUpload(lease);
    }
    @Test void parserInputReservationSettlesOnceAndFailedUnknownLengthRetainsBudget() {
        var purpose=com.mikasa.campusrunner.common.constant.MediaPurpose.ORDER_IMAGE;
        one.reserveInput("USER:31",purpose,-1);
        assertTrue(one.settleInput("USER:31",purpose.name(),1024));
        assertFalse(one.settleInput("USER:31",purpose.name(),1024));
        one.reserveInput("USER:31",purpose,-1);one.clearInputReservation();
        one.atomic(()->{long now=mapper.nowSeconds(),window=(now+28800)/86400*86400-28800;
            try(var c=org.springframework.jdbc.datasource.DataSourceUtils.getConnection(db).prepareStatement("select used_count,used_bytes from tb_request_guard where guard_key=? and window_start=?")) {
                c.setString(1,"USER:31:DAILY:CONTENT");c.setLong(2,window);
                try(var rows=c.executeQuery()){assertTrue(rows.next());assertEquals(2,rows.getLong(1));assertEquals(1024+purpose.getMaxInputBytes(),rows.getLong(2));}
            }catch(java.sql.SQLException error){throw new IllegalStateException(error);}return null;
        });
    }
}
