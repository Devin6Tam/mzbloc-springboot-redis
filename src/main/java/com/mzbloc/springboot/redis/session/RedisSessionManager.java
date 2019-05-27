package com.mzbloc.springboot.redis.session;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * 会话管理器
 *
 * Created by tanxw on 2019/5/15.
 */
public class RedisSessionManager implements HttpSessionManager, ServletContextAware {

	private RedisTemplate<String, Object> redisTemplate;
	private ServletContext servletContext;
	/**
	 * 会话key
	 */
	public String sessionKey;
	/**
	 * cookie 所在的域 如 mzbloc.com
	 */
	public String cookieDomain;

	/**
	 * cookie 路径
	 */
	public String cookiePath = "/";
	/**
	 * session超时  单位为分钟
	 */
	private int sessionTimeout;

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	@Override
	public HttpSession getSession(String sessionId) {
		if (StringUtils.isEmpty(sessionId)) {
			return null;
		}
		Object obj = redisTemplate.opsForValue().get(sessionId);
		if (obj == null) {
			return null;
		}
		RedisHttpSession session = newSession(sessionId);
		session.setMaxInactiveInterval(getSessionTimeout() * 60);
		if (obj instanceof Map) {
			@SuppressWarnings("unchecked")
            Map<String, Object> sessionMap = (Map<String, Object>) obj;
			session.getValueMap().putAll(sessionMap);
		}
		return session;
	}

	@Override
	public boolean exists(String sessionId) {
		if (StringUtils.isEmpty(sessionId)) {
			return false;
		}
		return redisTemplate.opsForValue().get(sessionId) != null;
	}

	@Override
	public RedisHttpSession newSession(String sessionId) {
		RedisHttpSession session = new RedisHttpSession(sessionId, redisTemplate);
		session.setMaxInactiveInterval(getSessionTimeout() * 60);
		session.setServletContext(servletContext);
		return session;
	}

	@Override
	public void updateSession(HttpSession session) {
		RedisHttpSession redisSession = (RedisHttpSession)session;
		if (!redisSession.isUpdated()) {
			redisSession.update();
		}
	}

	@Override
	public String getSessionKey() {
		return sessionKey;
	}
	/**
	 * 设置系统的sessionKey 比如  sid
	 * @param sessionKey
	 */
	public void setSessionKey(String sessionKey) {
		this.sessionKey = sessionKey;
	}
	@Override
	public int getSessionTimeout() {
		return sessionTimeout;
	}
	public void setSessionTimeout(int sessionTimeout) {
		this.sessionTimeout = sessionTimeout;
	}
	@Override
	public String getCookieDomain() {
		return cookieDomain;
	}

	public void setCookieDomain(String cookieDomain) {
		this.cookieDomain = cookieDomain;
	}

	public void setCookiePath(String cookiePath) {
		this.cookiePath = cookiePath;
	}

	@Override
	public String getCookiePath() {
		return cookiePath;
	}
	public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}
}
