package com.mikasa.campusrunner.handler;

import com.mikasa.campusrunner.common.exception.BaseException;
import com.mikasa.campusrunner.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * author  Edith
 * created  2024/4/20 13:44
 */

/**
 * 全局异常处理
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(com.mikasa.campusrunner.common.exception.RequestGuardUnavailableException.class)
    public org.springframework.http.ResponseEntity<Result> guardUnavailableHandler(com.mikasa.campusrunner.common.exception.RequestGuardUnavailableException e) {
        return org.springframework.http.ResponseEntity.status(503).header("Retry-After", "5").body(Result.error(e.getMessage()));
    }
    @ExceptionHandler(com.mikasa.campusrunner.common.exception.RequestLimitException.class)
    public org.springframework.http.ResponseEntity<Result> requestLimitHandler(com.mikasa.campusrunner.common.exception.RequestLimitException e) {
        return org.springframework.http.ResponseEntity.status(429).header("Retry-After",String.valueOf(e.getRetryAfter())).body(Result.error(e.getMessage()));
    }

    @ExceptionHandler(com.mikasa.campusrunner.common.exception.SubmissionPendingException.class)
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.CONFLICT)
    public Result submissionPendingHandler(com.mikasa.campusrunner.common.exception.SubmissionPendingException e) {
        return Result.error(e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result uploadSizeExceptionHandler(MaxUploadSizeExceededException e) {
        log.warn("Upload rejected because the multipart size limit was exceeded");
        return Result.error("图片文件过大，请选择不超过 10MB 的图片");
    }

    @ExceptionHandler
    public Result exceptionHandler(BaseException e){
        log.warn("Business request rejected, failureType={}", e.getClass().getSimpleName());
        return Result.error(e.getMessage());
    }

    @ExceptionHandler
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
    public Result exceptionHandler(Exception e){
        log.error("Unknown exception; failureType={}", e.getClass().getSimpleName());
        if (e instanceof DataAccessException) {
            return Result.error("系统数据异常，请稍后重试或联系管理员");
        }
        return Result.error("服务异常，请稍后重试");
    }

}
