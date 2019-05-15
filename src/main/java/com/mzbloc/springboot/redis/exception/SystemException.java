package com.mzbloc.springboot.redis.exception;

/**
 * Created by tanxw on 2019/5/15.
 */
public class SystemException extends RuntimeException{

    public SystemException(String message) {
        super(message);
    }
}
