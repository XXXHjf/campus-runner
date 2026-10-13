package com.mikasa.campusrunner.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mikasa.campusrunner.common.context.BaseContext;
import com.mikasa.campusrunner.common.exception.ParamException;
import com.mikasa.campusrunner.common.exception.SubmissionPendingException;
import com.mikasa.campusrunner.common.exception.UserException;
import com.mikasa.campusrunner.mapper.OrderMapper;
import com.mikasa.campusrunner.mapper.SubmissionRecordMapper;
import com.mikasa.campusrunner.mapper.UserMapper;
import com.mikasa.campusrunner.pojo.entity.Order;
import com.mikasa.campusrunner.pojo.entity.SubmissionRecord;
import com.mikasa.campusrunner.pojo.vo.UserVO;
import org.junit.jupiter.api.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.mikasa.campusrunner.service.SubmissionService.Operation.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SubmissionServiceTest {
    private final SubmissionRecordMapper mapper = mock(SubmissionRecordMapper.class);
    private final UserMapper users = mock(UserMapper.class);
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private final Map<String, SubmissionRecord> records = new HashMap<>();
    private SubmissionService service;
    private final String key = "client-request-0001";
    private String identity(SubmissionRecord record) {
        return record.getOwnerId() + ":" + record.getOperation() + ":" + record.getRequestKey();
    }

    @BeforeEach void setup() {
        BaseContext.setCurrentId(1L);
        var user = new UserVO(); user.setAuthentication(1); user.setDeleted(0);
        when(users.getById(anyLong())).thenReturn(user);
        doAnswer(call -> {
            SubmissionRecord record = call.getArgument(0);
            records.putIfAbsent(identity(record), json.convertValue(record, SubmissionRecord.class));
            return null;
        }).when(mapper).claim(any());
        when(mapper.lock(any())).thenAnswer(call -> records.get(identity(call.getArgument(0))));
        when(mapper.complete(any())).thenAnswer(call -> {
            SubmissionRecord claim = call.getArgument(0), record = records.get(identity(claim));
            if (!claim.getClaimToken().equals(record.getClaimToken()) || record.getResultJson() != null) return 0;
            record.setResultJson(claim.getResultJson()); return 1;
        });
        service = new SubmissionService(mapper, users, json);
    }
    @AfterEach void cleanup() { BaseContext.removeCurrentId(); }

    @Test void replaySurvivesServiceRestartAndDoesNotRepeatBusinessWrite() {
        var calls = new AtomicInteger();
        var payload = Map.of("title", "book", "imageAssetIds", List.of(10L));
        assertEquals(Map.of("id", 42), service.execute(SECOND_HAND_PRODUCT, key, payload, Map.class,
                () -> { calls.incrementAndGet(); return Map.of("id", 42); }));
        var restarted = new SubmissionService(mapper, users, json);
        assertEquals(Map.of("id", 42), restarted.execute(SECOND_HAND_PRODUCT, key, payload, Map.class,
                () -> { fail("must replay saved result"); return null; }));
        assertEquals(1, calls.get());
    }
    @Test void objectKeyOrderAndClientKeyFieldDoNotChangePayloadHash() {
        var first = new LinkedHashMap<String, Object>(); first.put("title", "book"); first.put("price", 2);
        service.execute(SECOND_HAND_PRODUCT, key, first, Long.class, () -> 42L);
        var second = new LinkedHashMap<String, Object>(); second.put("clientRequestId", key);
        second.put("price", 2); second.put("title", "book");
        assertEquals(42L, service.execute(SECOND_HAND_PRODUCT, key, second, Long.class, () -> fail("no new write")));
    }
    @Test void differentPayloadIncludingArrayOrderCannotReuseKey() {
        service.execute(RUNNER_ORDER, key, Map.of("images", List.of(1, 2)), Long.class, () -> 1L);
        assertThrows(SubmissionPendingException.class, () -> service.execute(RUNNER_ORDER, key,
                Map.of("images", List.of(2, 1)), Long.class, () -> fail("no new write")));
    }
    @Test void accountAndOperationIsolateKeys() {
        assertEquals(1L, service.execute(RUNNER_ORDER, key, Map.of(), Long.class, () -> 1L));
        assertEquals(2L, service.execute(SECOND_HAND_ORDER, key, Map.of(), Long.class, () -> 2L));
        BaseContext.setCurrentId(2L);
        assertEquals(3L, service.execute(RUNNER_ORDER, key, Map.of(), Long.class, () -> 3L));
        assertEquals(3, records.size());
    }
    @Test void authenticationIsCheckedEvenForSavedResult() {
        service.execute(RUNNER_ORDER, key, Map.of(), Long.class, () -> 1L);
        var user = new UserVO(); user.setAuthentication(0);
        when(users.getById(1L)).thenReturn(user);
        assertThrows(UserException.class, () -> service.execute(RUNNER_ORDER, key, Map.of(), Long.class,
                () -> fail("no new write")));
    }
    @Test void malformedKeyIsRejectedBeforeClaim() {
        assertThrows(ParamException.class, () -> service.execute(RUNNER_ORDER, "bad", Map.of(), Long.class, () -> 1L));
        verifyNoInteractions(mapper);
    }
    @Test void orphanClaimAndCorruptSavedResultNeverCreateAgain() {
        service.execute(RUNNER_ORDER, key, Map.of(), Long.class, () -> 1L);
        var record = records.values().iterator().next(); record.setResultJson(null);
        assertThrows(SubmissionPendingException.class, () -> service.execute(RUNNER_ORDER, key,
                Map.of(), Long.class, () -> fail("orphan must not repeat")));
        record.setResultJson("broken-json");
        assertThrows(SubmissionPendingException.class, () -> service.execute(RUNNER_ORDER, key,
                Map.of(), Long.class, () -> fail("corrupt result must not repeat")));
    }
    @Test void failedBusinessAndFailedResultSavePropagateForRollback() {
        var failure = new IllegalStateException("test failure");
        assertSame(failure, assertThrows(IllegalStateException.class, () -> service.execute(RUNNER_ORDER,
                key, Map.of(), Long.class, () -> { throw failure; })));
        verify(mapper, never()).complete(any());
        doReturn(0).when(mapper).complete(any());
        assertThrows(SubmissionPendingException.class, () -> service.execute(RUNNER_ORDER,
                "client-request-0002", Map.of(), Long.class, () -> 1L));
    }
    @Test void transactionBoundaryIncludesClaimBusinessAndResultWithoutInnerInsertBoundary() throws Exception {
        var boundary = SubmissionService.class.getMethod("execute", SubmissionService.Operation.class,
                String.class, Object.class, Class.class, java.util.function.Supplier.class).getAnnotation(Transactional.class);
        assertNotNull(boundary);
        assertTrue(Arrays.asList(boundary.rollbackFor()).contains(Exception.class));
        assertNull(OrderMapper.class.getMethod("insert", Order.class).getAnnotation(Transactional.class));
    }
}
