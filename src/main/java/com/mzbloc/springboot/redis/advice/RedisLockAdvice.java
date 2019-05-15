package com.mzbloc.springboot.redis.advice;

import com.mzbloc.springboot.redis.annotation.RedisLockAnnoation;
import com.mzbloc.springboot.redis.exception.CommonExceptionEnum;
import com.mzbloc.springboot.redis.exception.ExceptionUtil;
import com.mzbloc.springboot.redis.util.RedisDistributionLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 锁应用-切面工程
 * Created by tanxw on 2019/5/15.
 */
@Component
@Aspect
@Slf4j
public class RedisLockAdvice {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Around("@annotation(RedisLockAnnoation)")
    public Object processAround(ProceedingJoinPoint pjp) throws Throwable {
        //获取方法上的注解对象
        String methodName = pjp.getSignature().getName();
        Class<?> classTarget = pjp.getTarget().getClass();
        Class<?>[] par = ((MethodSignature) pjp.getSignature()).getParameterTypes();
        Method objMethod = classTarget.getMethod(methodName, par);
        RedisLockAnnoation rlAnnoation = objMethod.getDeclaredAnnotation(RedisLockAnnoation.class);

        //拼装分布式锁的key
        String[] keys = rlAnnoation.keys();
        Object[] args = pjp.getArgs();
        Object arg = args[0];
        StringBuilder temp = new StringBuilder();
        temp.append(rlAnnoation.keyPrefix());
        for (String key : keys) {
            String getMethod = "get" + StringUtils.capitalize(key);
            temp.append(MethodUtils.invokeExactMethod(arg, getMethod)).append("_");
        }
        String redisKey = StringUtils.removeEnd(temp.toString(), "_");

        RedisDistributionLock redisDistributionLock = new RedisDistributionLock(redisKey,stringRedisTemplate,rlAnnoation.expireTime());
        //执行分布式锁的逻辑
        if (rlAnnoation.isSpin()) {
            //阻塞锁
            int lockRetryTime = 0;
            try {
                while (!redisDistributionLock.tryLock(rlAnnoation.waitTime(),TimeUnit.MILLISECONDS,rlAnnoation.retryTimes())) {
                    // 获取不到锁的等待时间秒
                    if (lockRetryTime++ > rlAnnoation.retryWaitTimes()) {
                        log.error("lock exception. key:{}, lockRetryTime:{}", redisKey, lockRetryTime);
                        throw ExceptionUtil.geneException(CommonExceptionEnum.SYSTEM_ERROR);
                    }
                    TimeUnit.MILLISECONDS.sleep(rlAnnoation.waitTime());
                }
                return pjp.proceed();
            } finally {
                redisDistributionLock.unlock();
            }
        } else {
            //非阻塞锁
            try {
                if (!redisDistributionLock.tryLock(rlAnnoation.retryTimes())) {
                    log.error("lock exception. key:{}", redisKey);
                    throw ExceptionUtil.geneException(CommonExceptionEnum.SYSTEM_ERROR);
                }
                return pjp.proceed();
            } finally {
                redisDistributionLock.unlock();
            }
        }
    }

}
