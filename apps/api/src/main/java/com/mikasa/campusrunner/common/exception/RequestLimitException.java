package com.mikasa.campusrunner.common.exception;

public class RequestLimitException extends BaseException {
    private final int retryAfter;
    public RequestLimitException(int retryAfter) {
        super("操作较频繁，请稍后再试");
        this.retryAfter = Math.max(1, retryAfter);
    }
    public int getRetryAfter() { return retryAfter; }
}
