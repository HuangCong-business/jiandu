package com.app.platform.api.dto.admin;

/** 管理端角色列表/创建返回的单行摘要。 */
public record AdminRoleRowDto(
		Long id,
		String code,
		String name,
		String status,
		String isViewAll,
		int sequ
) {
}
