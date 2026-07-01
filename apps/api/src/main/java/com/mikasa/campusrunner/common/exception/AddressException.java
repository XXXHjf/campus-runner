package com.mikasa.campusrunner.common.exception;

import com.mikasa.campusrunner.pojo.entity.AddressBook;

/**
 * author  Edith
 * created  2024/4/26 10:21
 */
public class AddressException extends BaseException{
    public AddressException(){}

    public AddressException(String msg){
        super(msg);
    }
}
