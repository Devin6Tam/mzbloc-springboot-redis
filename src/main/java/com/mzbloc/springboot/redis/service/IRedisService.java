package com.mzbloc.springboot.redis.service;

import java.util.concurrent.TimeUnit;

/**
 * Created by tanxw on 2019/2/22.
 */
public interface IRedisService {

    /**
     * 缓存
     * @param key 键
     * @param value 值
     * @param unit 单位
     * @param longTime 多长时间
     */
    void setValue(String key, String value, TimeUnit unit, int longTime);

    /**
     * 缓存
     * @param key 键
     * @param value 值
     */
    void setValue(String key, String value);

    /**
     * 获取缓存
     * @param key 键
     */
    String getValue(String key);

    /**
     * 发布订阅（监听）
     * @param channel
     * @param message
     */
    void publish(String channel, Object message);

    /**
     * 消息队列：生产者
     * @param channel
     * @param message
     */
    void leftPush(String channel, String message);

    /**
     * 消息队列：消费者
     * @param channel
     */
    String rightPop(String channel);
}
