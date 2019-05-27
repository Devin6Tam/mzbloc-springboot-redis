package com.mzbloc.springboot.redis.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created by tanxw on 2019/5/16.
 */
@ConfigurationProperties(
        prefix = "mzbloc.redis"
)
public class RedisProperties {

    /**
     * 消息监听扫描包名
     */
    private String scanPackageName;

    private String sessionKey = "UsesSession";

    private String cookieDomain = "";

    private String cookiePath = "/";

    private Integer sessionTimeout = 40;

    public String getScanPackageName() {
        return scanPackageName;
    }

    public void setScanPackageName(String scanPackageName) {
        this.scanPackageName = scanPackageName;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public String getCookieDomain() {
        return cookieDomain;
    }

    public void setCookieDomain(String cookieDomain) {
        this.cookieDomain = cookieDomain;
    }

    public String getCookiePath() {
        return cookiePath;
    }

    public void setCookiePath(String cookiePath) {
        this.cookiePath = cookiePath;
    }

    public Integer getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(Integer sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }
}
