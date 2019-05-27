package com.mzbloc.springboot.redis.session;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpSession;

/**
 * 会话响应包装器
 * session response wrapper
 *
 * Created by tanxw on 2019/5/15.
 */
public class HttpSessionResponseWrapper extends HttpServletResponseWrapper {

	private HttpSession session;

	public HttpSessionResponseWrapper(HttpServletResponse response, HttpSession session) {
		super(response);
		this.session = session;
	}
}
