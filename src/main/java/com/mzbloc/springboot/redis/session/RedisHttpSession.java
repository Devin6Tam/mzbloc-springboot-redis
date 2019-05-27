package com.mzbloc.springboot.redis.session;

import org.springframework.data.redis.core.RedisTemplate;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 实现会话接口
 *
 * Created by tanxw on 2019/5/15.
 */
public class RedisHttpSession implements HttpSession {

	private RedisTemplate<String, Object> redisTemplate;
	private final String sessionId;

	private final Map<String, Object> sessionValueMap = new HashMap<>();

	private int maxInactiveInterval;

	private ServletContext servletContext;

	private boolean updated = false;

	private boolean invalidated = false;
	
	public RedisHttpSession(String sessionId, RedisTemplate<String, Object> redisTemplate) {
		this.sessionId = sessionId;
		this.redisTemplate = redisTemplate;
		
	}
	
	protected void update() {
		if (invalidated) {
			return;
		}
		if (sessionValueMap.size() == 0) {
			// 空会话不需要序列号，提高性能
			redisTemplate.opsForValue().set(sessionId, 0, getMaxInactiveInterval(), TimeUnit.SECONDS);
		} else {
			redisTemplate.opsForValue().set(sessionId, sessionValueMap, getMaxInactiveInterval(), TimeUnit.SECONDS);
		}
		updated = true;
	}

	protected Map<String, Object> getValueMap() {
		return this.sessionValueMap;
	}

	public Object getAttribute(String key) {
		return this.sessionValueMap.get(key);
	}

	public Enumeration<String> getAttributeNames() {
		return (new Enumerator<>(this.sessionValueMap.keySet(), true));
	}

	public void invalidate() {
		redisTemplate.delete(sessionId);
		updated = true;
		invalidated = true;
	}

	public void removeAttribute(String key) {
		sessionValueMap.remove(key);
		update();
	}

	public void setAttribute(String key, Object value) {
		sessionValueMap.put(key, value);
		update();
	}

	public long getCreationTime() {
		return 0;
	}

	public String getId() {
		return sessionId;
	}

	public long getLastAccessedTime() {
		return 0;
	}

	public int getMaxInactiveInterval() {
		return maxInactiveInterval;
	}

	public ServletContext getServletContext() {
		return servletContext;
	}

	@Deprecated
	public HttpSessionContext getSessionContext() {
		return null;
	}

	public Object getValue(String key) {
		return getAttribute(key);
	}

	@Deprecated
	public String[] getValueNames() {
		Set<String> set = sessionValueMap.keySet();
		return set.toArray(new String[set.size()]);
	}

	public boolean isNew() {
		return false;
	}

	@Deprecated
	public void putValue(String key, Object value) {
		setAttribute(key, value);
	}

	@Deprecated
	public void removeValue(String key) {
		removeAttribute(key);
	}

	public void setMaxInactiveInterval(int maxInactiveInterval) {
		this.maxInactiveInterval = maxInactiveInterval;
	}

	public boolean isUpdated() {
		return updated;
	}

	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}
}