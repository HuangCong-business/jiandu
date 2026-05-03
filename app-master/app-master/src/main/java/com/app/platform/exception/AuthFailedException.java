package com.app.platform.exception;

/**
 * 认证失败：统一映射为 HTTP 401，对外固定提示语，避免泄露账号是否存在。
 */
public class AuthFailedException extends RuntimeException {

	/** 使用固定文案，不区分用户名/密码错误细节。 */
	public AuthFailedException() {
		super("账号或密码错误");
	}
}
