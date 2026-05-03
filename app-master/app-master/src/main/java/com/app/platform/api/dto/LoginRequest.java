package com.app.platform.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 用户名密码登录请求；紧凑构造器中做 trim 规范化。 */
public record LoginRequest(
		@NotBlank @Size(min = 1, max = 64) @Pattern(regexp = "[a-zA-Z0-9._@-]+", message = "loginName format invalid") String loginName,
		@NotBlank @Size(min = 1, max = 128) String password) {

	public LoginRequest {
		loginName = loginName == null ? "" : loginName.trim();
		password = password == null ? "" : password.trim();
	}
}
