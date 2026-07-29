package com.mikasa.campusrunner.migration.media;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class HistoricalMediaMigrationSummary {
    private final MigrationMode mode;
    private final Map<String, Long> counters = new TreeMap<>();

    public HistoricalMediaMigrationSummary(MigrationMode mode) {
        this.mode = mode;
    }

    public void increment(String name) {
        counters.merge(name, 1L, Long::sum);
    }

    public void add(String name, long amount) {
        counters.merge(name, amount, Long::sum);
    }

    public long get(String name) {
        return counters.getOrDefault(name, 0L);
    }

    public MigrationMode getMode() {
        return mode;
    }

    public Map<String, Long> getCounters() {
        return Collections.unmodifiableMap(counters);
    }
}
