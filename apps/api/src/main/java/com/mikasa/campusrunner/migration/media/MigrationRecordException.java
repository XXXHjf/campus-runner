package com.mikasa.campusrunner.migration.media;

public class MigrationRecordException extends RuntimeException {
    private final String code;
    private final boolean manualReview;

    public MigrationRecordException(String code, String message) {
        this(code, message, false);
    }

    public MigrationRecordException(String code, String message, boolean manualReview) {
        super(message);
        this.code = code;
        this.manualReview = manualReview;
    }

    public String getCode() {
        return code;
    }

    public boolean isManualReview() {
        return manualReview;
    }
}
