package com.mikasa.campusrunner.common.exception;

/** A write result cannot safely be classified as rejected. */
public class SubmissionPendingException extends BaseException {
    public SubmissionPendingException() { super("提交结果暂未确认，请稍后重试"); }
    public SubmissionPendingException(String message) { super(message); }
}
