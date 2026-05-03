package com.app.platform.exception;

/** 已登录但无权访问该资源或操作（HTTP 403）。 */
public class ForbiddenException extends RuntimeException {

	public ForbiddenException(String message) {
		super(message);
	}
}
