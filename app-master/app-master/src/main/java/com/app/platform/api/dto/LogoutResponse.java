package com.app.platform.api.dto;

/** 登出接口统一响应体。 */
public record LogoutResponse(boolean success) {

	/** 构造表示登出成功的固定响应。 */
	public static LogoutResponse ok() {
		return new LogoutResponse(true);
	}
}
