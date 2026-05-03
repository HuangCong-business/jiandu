package com.app.platform.api.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Locale;

/**
 * 管理端创建用户：登录名规范化（小写）、密码与展示名裁剪，角色列表默认空列表。
 */
public record AdminCreateUserRequest(
		@NotBlank @Size(min = 1, max = 64) @Pattern(regexp = "[a-zA-Z0-9._@-]+", message = "code format invalid") String code,
		@NotBlank @Size(min = 8, max = 128) String password,
		@Size(max = 128) String displayName,
		String userType,
		@NotBlank @Size(min = 1, max = 1) String status,
		Long deptId,
		List<Long> roleIds) {

	public AdminCreateUserRequest {
		// 登录名统一小写并去空白，便于唯一性比较
		code = code == null ? "" : code.trim().toLowerCase(Locale.ROOT);
		password = password == null ? "" : password.trim();
		displayName = displayName == null ? null : displayName.trim();
		if (displayName != null && displayName.isEmpty()) {
			displayName = null;
		}
		// 用户类型缺省为普通用户约定值
		if (userType == null || userType.isBlank()) {
			userType = "0";
		}
		if (roleIds == null) {
			roleIds = List.of();
		}
	}
}
