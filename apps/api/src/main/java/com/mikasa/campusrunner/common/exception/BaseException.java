package com.mikasa.campusrunner.common.exception;

/**
 * author  Edith
 * created  2024/4/20 13:42
 */
public class BaseException extends RuntimeException{
    public BaseException(){

    }

    public BaseException(String msg){
        super(msg);
    }
}
