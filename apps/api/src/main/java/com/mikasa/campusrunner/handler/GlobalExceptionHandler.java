package com.mikasa.campusrunner.handler;

import com.mikasa.campusrunner.common.exception.BaseException;
import com.mikasa.campusrunner.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
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

    @ExceptionHandler
    public Result exceptionHandler(BaseException e){
        log.error("Exception: {}", e.getMessage());
        return Result.error(e.getMessage());
    }

    @ExceptionHandler
    public Result exceptionHandler(Exception e){
        log.error("Unknown exception", e);
        if (e instanceof DataAccessException) {
            return Result.error("系统数据异常，请稍后重试或联系管理员");
        }
        return Result.error("服务异常，请稍后重试");
    }

}
