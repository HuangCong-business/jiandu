package com.app.platform.exception;

/** 用户 id 不存在或已逻辑删除（HTTP 404）。 */
public class UserNotFoundException extends RuntimeException {

	public UserNotFoundException() {
		super("用户不存在");
	}
}
