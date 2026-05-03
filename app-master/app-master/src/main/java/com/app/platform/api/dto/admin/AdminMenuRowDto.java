package com.app.platform.api.dto.admin;

/** 管理端单条菜单的完整展示字段。 */
public record AdminMenuRowDto(
		long id,
		Long parentId,
		String name,
		String path,
		String icon,
		String menuType,
		String clientType,
		int sequ,
		String status,
		String controlType
) {
}
