package com.mzbloc.springboot.redis.session;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 负责从request获取sessionid,并从缓存中取出session,
 * 如果没有session 则new 一个session，如果sessionid为空则使用随机算法成才sessionid并new 一个session
 * OncePerRequestFilter 一次请求只通过一次filter
 *
 * Created by tanxw on 2019/5/15.
 */
public class RedisSessionFilter extends OncePerRequestFilter {
	private static final Log log = LogFactory.getLog(RedisSessionFilter.class);
	private HttpSessionManager sessionManager;
	private static final String sessionSufix = "sd:";
    private List<String> excludeList = null;

	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
			throws ServletException, IOException {
        String requestURI = req.getRequestURI();
        if (excludeList != null) {
            for (String exclude : excludeList) {
                if (requestURI.contains(exclude)) {
                    req = new HttpSessionRequestWrapper(req, null);
                    chain.doFilter(req, res);
                    return;
                }
            }
        }
		String sessionKey = sessionManager.getSessionKey();
		//获取sessionid
		String sessionId = getCookie(req, sessionKey);
		boolean addCookie = false;
        HttpSession session = sessionManager.getSession(sessionId);
		if (StringUtils.isEmpty(sessionId) || session == null) {
			addCookie = true;
			sessionId = this.generateSessionId();
		}
		
		if (log.isDebugEnabled()) {
			log.info("sessionid="+sessionId);
		}
		
		if (session == null) {
			session = sessionManager.newSession(sessionId);
			sessionManager.updateSession(session);
		}
		if (addCookie) {
			Cookie sessionCookie = new Cookie(sessionKey, sessionId);
			sessionCookie.setPath(sessionManager.getCookiePath());
            if (StringUtils.isNotBlank(sessionManager.getCookieDomain())) {
                sessionCookie.setDomain(sessionManager.getCookieDomain());
            }
			res.addCookie(sessionCookie);
		}
		req = new HttpSessionRequestWrapper(req, session);
		chain.doFilter(req, res);
	}

	@Override
	protected void initFilterBean() throws ServletException {
		WebApplicationContext spring = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
		sessionManager = spring.getBean(HttpSessionManager.class);
	}

	@Override
	public void destroy() {
		sessionManager = null;
	}
	
	private String getCookie(HttpServletRequest req, String cookieKey) {
		if (StringUtils.isEmpty(cookieKey) || null == req) {
			return null;
		}
		Cookie[] cookies = req.getCookies();
		if (cookies == null || cookies.length == 0) {
			return null;
		}
		for (Cookie cookie : cookies) {
			if (cookieKey.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}

	protected String generateSessionId() {
	    return sessionSufix + RandomStringUtils.randomAlphanumeric(15) + System.currentTimeMillis();
    }

    private String getConvertRegStr(String str) {
        if (StringUtils.isNotBlank(str)) {
            String[] excludes = str.split(";");  //以分号进行分割
            int length = excludes.length;
            for(int i=0;i<length;i++){
                String tmpExclude = excludes[i];
                //对点、反斜杠和星号进行转义
                tmpExclude = tmpExclude.replace("\\", "\\\\").replace(".", "\\.").replace("*", ".*");
                tmpExclude = "^" + tmpExclude + "$";
                excludes[i] = tmpExclude;
            }
            return StringUtils.join(excludes, "|");
        }
        return str;
    }

    public void setExclude(String exclude) {
	    String[] excludeArray = exclude.split(";");
        this.excludeList = new ArrayList<>(Arrays.asList(excludeArray));
    }
}
