package com.mzbloc.springboot.redis.session;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

/**
 * 会话请求包装器
 * session request wrapper
 *
 * Created by tanxw on 2019/5/15.
 */
public class HttpSessionRequestWrapper extends HttpServletRequestWrapper {

	private HttpSession session;

	public HttpSessionRequestWrapper(HttpServletRequest request, HttpSession session) {
		super(request);
		this.session = session;
	}

	public HttpSession getSession(boolean create) {
		return session;
	}

	public HttpSession getSession() {
		return getSession(true);
	}
	
}
