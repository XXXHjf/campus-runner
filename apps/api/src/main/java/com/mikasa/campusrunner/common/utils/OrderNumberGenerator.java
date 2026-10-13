package com.mikasa.campusrunner.common.utils;

import org.springframework.dao.DuplicateKeyException;

import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** New numbers fit WeChat's 32-character merchant order number limit. */
public final class OrderNumberGenerator {
    private OrderNumberGenerator() {}

    public static String next(String prefix) {
        if (!"R".equals(prefix) && !"SH".equals(prefix)) throw new IllegalArgumentException("Unknown order prefix");
        String random = UUID.randomUUID().toString().replace("-", "");
        return prefix + random.substring(0, 32 - prefix.length());
    }

    public static <T> T persist(String prefix, Function<String, T> insert, Predicate<String> numberExists) {
        return persist(() -> next(prefix), insert, numberExists);
    }

    static <T> T persist(Supplier<String> numbers, Function<String, T> insert, Predicate<String> numberExists) {
        for (int attempt = 0; ; attempt++) {
            String number = numbers.get();
            try {
                return insert.apply(number);
            } catch (DuplicateKeyException conflict) {
                // Do not disguise collisions on another unique business constraint.
                if (attempt >= 4 || !numberExists.test(number)) throw conflict;
            }
        }
    }
}
