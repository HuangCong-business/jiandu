package com.app.platform.api.dto.admin;

import java.util.List;

/**
 * 菜单分配树节点：可挂操作列表与子菜单，形成递归树结构。
 */
public record AdminMenuAssignNodeDto(
		long id,
		Long parentId,
		String name,
		int sequ,
		List<AdminAssignTreeOpDto> operations,
		List<AdminMenuAssignNodeDto> children
) {
}
