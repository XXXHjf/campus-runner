package com.mikasa.campusrunner.common.utils;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class OrderNumberGeneratorTest {
    @Test
    void parallelGenerationDoesNotDependOnMillisecondClockAndFitsMerchantLimit() {
        Set<String> generated = ConcurrentHashMap.newKeySet();
        IntStream.range(0, 20_000).parallel().forEach(index -> {
            String prefix = index % 2 == 0 ? "R" : "SH";
            String number = OrderNumberGenerator.next(prefix);
            assertEquals(32, number.length());
            assertTrue(number.startsWith(prefix));
            assertTrue(number.matches("[A-Za-z0-9]{32}"));
            assertTrue(generated.add(number));
        });
        assertEquals(20_000, generated.size());
    }

    @Test
    void confirmedNumberCollisionRegeneratesAndReturnsSuccessfulInsertResult() {
        AtomicInteger attempts = new AtomicInteger();
        AtomicInteger numbers = new AtomicInteger();
        int result = OrderNumberGenerator.persist(() -> "R" + numbers.incrementAndGet(), number -> {
            if (attempts.incrementAndGet() < 3) throw new DuplicateKeyException("collision");
            return 42;
        }, number -> true);
        assertEquals(42, result);
        assertEquals(3, attempts.get());
        assertEquals(3, numbers.get());
    }

    @Test
    void unrelatedUniqueConstraintIsNotRetried() {
        AtomicInteger attempts = new AtomicInteger();
        DuplicateKeyException failure = new DuplicateKeyException("other constraint");
        assertSame(failure, assertThrows(DuplicateKeyException.class, () -> OrderNumberGenerator.persist(
                () -> "candidate", number -> { attempts.incrementAndGet(); throw failure; }, number -> false)));
        assertEquals(1, attempts.get());
    }

    @Test
    void retryBudgetIsFiniteAndOtherFailuresPropagate() {
        AtomicInteger attempts = new AtomicInteger();
        assertThrows(DuplicateKeyException.class, () -> OrderNumberGenerator.persist(() -> "candidate", number -> {
            attempts.incrementAndGet(); throw new DuplicateKeyException("collision");
        }, number -> true));
        assertEquals(5, attempts.get());
        IllegalStateException failure = new IllegalStateException("not duplicate");
        assertSame(failure, assertThrows(IllegalStateException.class, () -> OrderNumberGenerator.persist(
                () -> "candidate", number -> { throw failure; }, number -> fail("must not check collision"))));
    }
}
