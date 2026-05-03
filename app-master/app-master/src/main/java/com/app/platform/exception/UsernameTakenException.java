package com.app.platform.exception;

/** 注册或改名时登录名冲突（HTTP 409）。 */
public class UsernameTakenException extends RuntimeException {

	public UsernameTakenException() {
		super("用户名已被使用");
	}
}
