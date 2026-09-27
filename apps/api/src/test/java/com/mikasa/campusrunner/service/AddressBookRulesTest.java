package com.mikasa.campusrunner.service;

import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.mapper.*;
import com.mikasa.campusrunner.pojo.dto.*;
import com.mikasa.campusrunner.pojo.entity.AddressBook;
import com.mikasa.campusrunner.pojo.vo.UserVO;
import com.mikasa.campusrunner.service.impl.user.AddressBookServiceImpl;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.support.*;
import org.springframework.aop.framework.ProxyFactory;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class AddressBookRulesTest {
    AddressBookMapper mapper;
    AddressBookServiceImpl service;
    Map<Long, AddressBook> rows;
    boolean fail;
    int failAt, writes;
    AddressBook copy(AddressBook a) {
        AddressBook b = new AddressBook();
        org.springframework.beans.BeanUtils.copyProperties(a, b);
        return b;
    }
    @BeforeEach void setup() {
        BaseContext.setCurrentId(7L);
        rows = new HashMap<>();
        mapper = mock(AddressBookMapper.class);
        AddressBookServiceImpl target = new AddressBookServiceImpl();
        ReflectionTestUtils.setField(target, "addressBookMapper", mapper);
        ReflectionTestUtils.setField(target, "schoolMapper", mock(SchoolMapper.class));
        ReflectionTestUtils.setField(target, "compusMapper", mock(CompusMapper.class));
        ReflectionTestUtils.setField(target, "buildCategoryMapper", mock(BuildCategoryMapper.class));
        ReflectionTestUtils.setField(target, "buildingMapper", mock(BuildingMapper.class));
        UserMapper users = mock(UserMapper.class);
        when(users.getById(7L)).thenReturn(UserVO.builder().id(7L).schoolId(1L).build());
        ReflectionTestUtils.setField(target, "userMapper", users);
        when(mapper.getById(anyLong())).thenAnswer(i -> {
            AddressBook a = rows.get(i.getArgument(0));
            return a == null || a.getDeleted() == 1 ? null : copy(a);
        });
        when(mapper.getIds(any())).thenAnswer(i -> {
            AddressBook f = i.getArgument(0);
            return rows.values().stream().filter(a -> a.getDeleted() == 0 && a.getUserId().equals(f.getUserId())
                && (f.getIsDefault() == null || f.getIsDefault().equals(a.getIsDefault())))
                .map(AddressBook::getId).toList();
        });
        doAnswer(i -> { rows.values().forEach(a -> { if(a.getDeleted() == 0) a.setIsDefault(0); }); return null; })
            .when(mapper).clearDefault(any());
        when(mapper.insert(any())).thenAnswer(i -> {
            AddressBook a = i.getArgument(0); a.setId(10L); rows.put(10L, copy(a)); return 1;
        });
        when(mapper.update(any())).thenAnswer(i -> {
            if (fail || ++writes == failAt) throw new RuntimeException("write failed");
            AddressBook a = i.getArgument(0), b = rows.get(a.getId());
            if (a.getIsDefault() != null) b.setIsDefault(a.getIsDefault());
            if (a.getDetails() != null) b.setDetails(a.getDetails());
            if (a.getLabel() != null) b.setLabel(a.getLabel());
            if (a.getDeleted() != null) b.setDeleted(a.getDeleted());
            return 1;
        });
        // Exercise actual Spring transaction interception with an in-memory storage snapshot.
        AbstractPlatformTransactionManager tx = new AbstractPlatformTransactionManager() {
            Map<Long, AddressBook> snapshot;
            protected Object doGetTransaction() { return new Object(); }
            protected void doBegin(Object t, org.springframework.transaction.TransactionDefinition d) {
                snapshot = new HashMap<>(); rows.forEach((id,a) -> snapshot.put(id,copy(a)));
            }
            protected void doCommit(DefaultTransactionStatus s) {}
            protected void doRollback(DefaultTransactionStatus s) { rows.clear(); rows.putAll(snapshot); }
        };
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(tx, new AnnotationTransactionAttributeSource()));
        service = (AddressBookServiceImpl) proxy.getProxy();
    }
    @AfterEach void cleanup() { BaseContext.removeCurrentId(); }
    void add(long id, int state) { rows.put(id, AddressBook.builder().id(id).userId(7L).isDefault(state).deleted(0).details("old").label("old").build()); }
    AddressBookUpdateDTO update(int state) {
        AddressBookUpdateDTO dto = new AddressBookUpdateDTO(); dto.setId(2L); dto.setIsDefault(state); dto.setDetails(""); dto.setLabel("new"); return dto;
    }
    @Test void firstAddressBecomesDefault() { service.save(new AddressBookDTO()); assertEquals(1,rows.get(10L).getIsDefault()); }
    @Test void replacementAndContentSaveTogether() {
        add(1,1); add(2,0); service.update(update(1));
        assertEquals(0,rows.get(1L).getIsDefault()); assertEquals(1,rows.get(2L).getIsDefault());
        assertEquals("",rows.get(2L).getDetails()); assertEquals("new",rows.get(2L).getLabel());
    }
    @Test void cannotUnsetDefault() { add(2,1); assertThrows(RuntimeException.class,()->service.update(update(0))); assertEquals("old",rows.get(2L).getDetails()); }
    @Test void deletionPicksNewest() { add(1,1); add(2,0); add(5,0); service.deleteById(1L); assertEquals(1,rows.get(5L).getIsDefault()); assertEquals(0,rows.get(2L).getIsDefault()); }
    @Test void deletingLastAddressIsAllowed() { add(1,1); service.deleteById(1L); assertEquals(1,rows.get(1L).getDeleted()); }
    @Test void failureRollsBackDefaultClearing() { add(1,1); add(2,0); fail=true; assertThrows(RuntimeException.class,()->service.update(update(1))); assertEquals(1,rows.get(1L).getIsDefault()); assertEquals(0,rows.get(2L).getIsDefault()); }
    @Test void deletionFailureRollsBack() { add(1,1); add(2,0); failAt=2; assertThrows(RuntimeException.class,()->service.deleteById(1L)); assertEquals(0,rows.get(1L).getDeleted()); assertEquals(1,rows.get(1L).getIsDefault()); }
}
