package com.mikasa.campusrunner.common.exception;

/**
 * author  Edith
 * created  2024/4/20 13:43
 */
public class LoginFailedException extends BaseException{
    public LoginFailedException(){}

    public LoginFailedException(String msg){
        super(msg);
    }
}
