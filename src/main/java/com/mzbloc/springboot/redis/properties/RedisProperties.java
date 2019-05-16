package com.mzbloc.springboot.redis.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created by tanxw on 2019/5/16.
 */
@ConfigurationProperties(
        prefix = "mzbloc.redis"
)
public class RedisProperties {

    private String scanPackageName;

    public String getScanPackageName() {
        return scanPackageName;
    }

    public void setScanPackageName(String scanPackageName) {
        this.scanPackageName = scanPackageName;
    }
}
