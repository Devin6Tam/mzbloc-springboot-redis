package com.mzbloc.springboot.redis.exception;

/**
 * Created by tanxw on 2019/5/15.
 */
public enum CommonExceptionEnum {

    SYSTEM_ERROR("系统异常");

    private String desc;

    CommonExceptionEnum(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
