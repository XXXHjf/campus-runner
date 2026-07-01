package com.mikasa.campusrunner.common.exception;

import com.mikasa.campusrunner.common.context.BaseContext;

/**
 * author  Edith
 * created  2024/6/13 11:40
 */
public class UserException extends BaseException {
    public UserException(){}

    public UserException(String msg){
        super(msg);
    }
}
