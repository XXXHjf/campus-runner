package com.mikasa.campusrunner.common.exception;

public class RequestGuardUnavailableException extends BaseException {
    public RequestGuardUnavailableException() { super("服务繁忙，请稍后重试"); }
}
