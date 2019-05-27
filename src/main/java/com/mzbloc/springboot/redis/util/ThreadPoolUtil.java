package com.mzbloc.springboot.redis.util;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by tanxw on 2019/5/17.
 */
public class ThreadPoolUtil {

    private final static ThreadFactory REDIS_FRAMEWORK_THREAD_FACTORY = new ThreadFactory() {
        private String namePrefix = "mzbloc.framework.redis";
        private final AtomicInteger poolNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(Thread.currentThread().getThreadGroup(), r,
                    namePrefix + poolNumber.getAndIncrement(),
                    0);
        }
    };
    private final static ThreadPoolExecutor SCHEDULE_POOL =
            new ThreadPoolExecutor(100, 300, 0L, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>(), REDIS_FRAMEWORK_THREAD_FACTORY);


    public static ThreadPoolExecutor getScheduleLoanPool() {
        return SCHEDULE_POOL;
    }
}
