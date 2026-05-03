package com.app.platform.api.dto.admin;

import java.time.Instant;
import java.util.List;

/**
 * 管理端用户单行：含组织、角色、最近登录与锁定截止时间等审计信息。
 */
public record AdminUserRowDto(
		long id,
		String code,
		String name,
		String status,
		String userType,
		Long deptId,
		String deptName,
		List<Long> roleIds,
		List<String> roleNames,
		Instant lastLoginTime,
		Instant lockedUntil) {
}
