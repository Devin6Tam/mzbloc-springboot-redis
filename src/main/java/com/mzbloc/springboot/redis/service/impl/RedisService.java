package com.mzbloc.springboot.redis.service.impl;


import com.mzbloc.springboot.redis.service.IRedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Created by tanxw on 2019/2/22.
 */
@Service
public class RedisService implements IRedisService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void setValue(String key, String value, TimeUnit unit, int longTime){
        stringRedisTemplate.opsForValue().set(key,value,longTime,unit);
    }

    @Override
    public void setValue(String key,String value){
        stringRedisTemplate.opsForValue().set(key,value);
    }

    @Override
    public String getValue(String key){
        return  stringRedisTemplate.opsForValue().get(key);
    }

    @Override
    public void publish(String channel, Object message){
        stringRedisTemplate.convertAndSend(channel,message);
    }

    @Override
    public void leftPush(String channel, String message){
        stringRedisTemplate.opsForList().leftPush(channel,message);
    }

    @Override
    public String rightPop(String channel){
        return stringRedisTemplate.opsForList().rightPop(channel);
    }

}
