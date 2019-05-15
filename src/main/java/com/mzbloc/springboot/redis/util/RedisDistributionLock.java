package com.mzbloc.springboot.redis.util;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * redis 分布式事务锁
 * Created by tanxw on 2019/5/15.
 */
public class RedisDistributionLock implements Lock{

    private static final Logger log = LoggerFactory.getLogger(RedisDistributionLock.class);
    private static final int defaultExpTime = 10000;
    private static final int defaultRetryTimes = 3;
    private StringRedisTemplate redisTemplate;
    private String lockKey;
    private String value;
    private boolean isLock;
    private int expire;
    private long lockTimestamp;
    private long lockedTimestamp;
    private static final int defaultThreadSleepTime = 5;
    private long threadSleepTime;
    private static final DefaultRedisScript<Boolean> redisScript = new DefaultRedisScript("if redis.call(\"get\",KEYS[1]) == ARGV[1] then \n    redis.call(\"del\",KEYS[1]) \n\t return true \nelse \n\treturn false \nend", Boolean.class);

    public RedisDistributionLock(String lockKey, StringRedisTemplate redisTemplate, int expire, int thradSleepTime) {
        this.redisTemplate = redisTemplate;
        this.lockKey = lockKey;
        this.expire = expire;
        this.value = RandomStringUtils.randomNumeric(4);
        this.threadSleepTime = (long)thradSleepTime;
    }

    public RedisDistributionLock(String lockKey, StringRedisTemplate redisTemplate, int expire) {
        this(lockKey, redisTemplate, expire==0?defaultExpTime:expire, defaultThreadSleepTime);
    }

    public void lock() {
        if(!this.tryLock()) {
            this.waitForLock(2147483647L, TimeUnit.MILLISECONDS);
        }
    }

    public void lockInterruptibly() throws InterruptedException {
        throw new InterruptedException();
    }

    public boolean tryLock(int retryTimes){
        try {
            log.info("tryLock:[{}]", this.lockKey);
            this.lockTimestamp = getCurrentTimeFromRedis();
            long lockExpireTime = this.lockTimestamp + (long)this.expire;
            this.value = this.value + ":" + lockExpireTime;
            boolean success = this.redisTemplate.opsForValue().setIfAbsent(this.lockKey, this.value).booleanValue();
            if(success) {
                this.redisTemplate.expire(this.lockKey, (long)this.expire, TimeUnit.MILLISECONDS);
                this.lockedTimestamp = this.lockTimestamp;
                if(log.isDebugEnabled()) {
                    log.info("locked:{}", this.lockKey);
                }

                this.isLock = true;
            } else {
                Object valueFromRedis = this.getKeyWithRetry(this.lockKey, retryTimes);
                // 避免获取锁失败,同时对方释放锁后,造成NPE
                if (valueFromRedis != null) {
                    //已存在的锁超时时间
                    long oldExpireTime = Long.parseLong((String)valueFromRedis);
                    log.debug("redis lock debug, key already seted. key:[{}], oldExpireTime:[{}]",this.lockKey,oldExpireTime);
                    //锁过期时间小于当前时间,锁已经超时,重新取锁
                    if (oldExpireTime <= lockTimestamp) {
                        log.debug("redis lock debug, lock time expired. key:[{}], oldExpireTime:[{}], now:[{}]", this.lockKey, oldExpireTime, lockTimestamp);
                        String valueFromRedis2 = this.redisTemplate.opsForValue().getAndSet(this.lockKey, String.valueOf(lockExpireTime));
                        long currentExpireTime = Long.parseLong(valueFromRedis2);
                        //判断currentExpireTime与oldExpireTime是否相等
                        if(currentExpireTime == oldExpireTime){
                            //相等,则取锁成功
                            log.debug("redis lock debug, getSet. key:[{}], currentExpireTime:[{}], oldExpireTime:[{}], lockExpireTime:[{}]", this.lockKey, currentExpireTime, oldExpireTime, lockExpireTime);
                            this.redisTemplate.expire(this.lockKey, this.expire, TimeUnit.MILLISECONDS);
                            this.isLock = true;
                            return true;
                        }else{
                            //不相等,取锁失败
                            return false;
                        }
                    }
                } else {
                    log.warn("redis lock,lock have been release. key:[{}]", this.lockKey);
                    return false;
                }
            }

            return success;
        } catch (Exception var4) {
            log.error("获取锁异常：{}", this.lockKey);
            return false;
        }
    }

    public boolean tryLock() {
        return this.tryLock(this.defaultRetryTimes);
    }

    public boolean tryLock(long time, TimeUnit unit,int retryTimes) throws InterruptedException {
        return this.tryLock(retryTimes)?true:this.waitForLock(time, unit,retryTimes);
    }

    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return this.tryLock()?true:this.waitForLock(time, unit);
    }

    private boolean waitForLock(long waitTime, TimeUnit unit) {
        return this.waitForLock(waitTime,unit,this.defaultRetryTimes);
    }

    private boolean waitForLock(long waitTime, TimeUnit unit,int retryTimes) {
        try {
            long e = unit.toMillis(waitTime);
            int times = 0;

            while(true) {
                while(true) {
                    while(this.lockTimestamp + e > getCurrentTimeFromRedis()) {
                        long timestamp = getCurrentTimeFromRedis();
                        long end = timestamp + (long)this.expire;
                        String redValue = this.value + ":" + end;
                        boolean success = this.redisTemplate.opsForValue().setIfAbsent(this.lockKey, redValue).booleanValue();
                        //累计取锁次数
                        ++times;
                        //取锁成功,为key设置expire
                        if(success) {
                            this.value = redValue;
                            this.redisTemplate.expire(this.lockKey, (long)this.expire, TimeUnit.MILLISECONDS);
                            this.lockedTimestamp = timestamp;
                            this.isLock = true;
                            if(log.isDebugEnabled()) {
                                log.info("locked:{}, try_lock_times:{}", this.lockKey, Integer.valueOf(times));
                            }
                            return true;
                        }

                        if(times > retryTimes) {
                            String redisVal = this.redisTemplate.opsForValue().get(this.lockKey);
                            long current = getCurrentTimeFromRedis();
                            long endTime = redisVal.indexOf(":") > 0? NumberUtils.toLong(redisVal.split(":")[1]):NumberUtils.toLong(redisVal);
                            if(StringUtils.isNotEmpty(redisVal) && current > endTime) {
                                this.redisTemplate.execute(redisScript, Arrays.asList(new String[]{this.lockKey}), new Object[]{redisVal});
                                log.info("Lock [{}] 被强制删除锁 redisVal:{}, concurent:{}", new Object[]{this.lockKey, redisVal, Long.valueOf(current)});
                            } else {
                                TimeUnit.MICROSECONDS.sleep(this.threadSleepTime);
                            }
                        } else {
                            TimeUnit.MICROSECONDS.sleep(this.threadSleepTime);
                        }
                    }

                    return false;
                }
            }
        } catch (InterruptedException var18) {
            log.error("Thread.sleep 异常", var18);
            var18.printStackTrace();
            throw new RuntimeException(var18);
        }
    }

    private Object getKeyWithRetry(String key, int retryTimes) {
        int failTime = 0;
        while (failTime < retryTimes) {
            try {
                return redisTemplate.opsForValue().get(key);
            } catch (Exception e) {
                failTime++;
                if (failTime >= retryTimes) {
                    throw e;
                }
            }
        }
        return null;
    }

    public void unlock() {
        if(this.isLock) {
            this.redisTemplate.execute(redisScript, Arrays.asList(new String[]{this.lockKey}), new Object[]{this.value});
            if(log.isDebugEnabled()) {
                log.info("unlock[{}], lockedTimes:{}ms", this.lockKey, Long.valueOf(getCurrentTimeFromRedis() - this.lockedTimestamp));
            }
        }

    }

    /**
     * 服务器集群时，使用如下方法获取当前时间
     *
     * 替代 System.currentTimeMillis()
     * @return
     */
    public long getCurrentTimeFromRedis(){
        return this.redisTemplate.execute(new RedisCallback<Long>(){
            @Override
            public Long doInRedis(RedisConnection connection) throws DataAccessException{
                return connection.time();
            }
        });
    }


    public Condition newCondition() {
        return null;
    }
}
