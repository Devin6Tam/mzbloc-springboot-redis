package com.mzbloc.springboot.redis.exception;

import org.apache.commons.lang3.StringUtils;

/**
 * Created by tanxw on 2019/5/15.
 */
public class ExceptionUtil {

    public static RuntimeException geneException(CommonExceptionEnum exceptionEnum,String exMessage){
        if(exceptionEnum!=null){
            if(exceptionEnum.equals(CommonExceptionEnum.SYSTEM_ERROR)){
                return new SystemException(StringUtils.isNotBlank(exMessage)?exMessage:exceptionEnum.getDesc());
            }
        }
        return new RuntimeException(StringUtils.isNotBlank(exMessage)?exMessage:"未定义的异常！");
    }

    public static RuntimeException geneException(CommonExceptionEnum exceptionEnum){
        return geneException(exceptionEnum,null);
    }
}
