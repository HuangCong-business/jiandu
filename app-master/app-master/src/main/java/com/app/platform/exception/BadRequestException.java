package com.app.platform.exception;

/** 客户端请求参数或业务前置条件不满足（HTTP 400）。 */
public class BadRequestException extends RuntimeException {

	/** @param message 可直接展示给调用方的说明 */
	public BadRequestException(String message) {
		super(message);
	}
}
