package com.mzbloc.springboot.redis.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 消息监听器（注解类）
 * Created by tanxw on 2019/2/22.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface RedisMessageListener {

    /**
     * 主题，以逗号隔开","
     * @return
     */
    String [] topics();
}
