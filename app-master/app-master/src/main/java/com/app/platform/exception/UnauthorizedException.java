package com.app.platform.exception;

/** 需要登录的接口在未建立会话时抛出（HTTP 401）。 */
public class UnauthorizedException extends RuntimeException {

	public UnauthorizedException() {
		super("未登录或会话已失效");
	}
}
