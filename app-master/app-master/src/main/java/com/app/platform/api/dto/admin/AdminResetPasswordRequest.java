package com.app.platform.api.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 管理员重置用户密码：新密码必填；是否下次登录强制改密默认为 false。 */
public record AdminResetPasswordRequest(
		@NotBlank @Size(min = 8, max = 128) String newPassword,
		Boolean forceChangeOnNextLogin) {

	public AdminResetPasswordRequest {
		newPassword = newPassword == null ? "" : newPassword.trim();
		if (forceChangeOnNextLogin == null) {
			forceChangeOnNextLogin = false;
		}
	}
}
