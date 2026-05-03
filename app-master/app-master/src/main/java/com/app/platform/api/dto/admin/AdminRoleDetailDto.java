package com.app.platform.api.dto.admin;

import java.util.List;

/**
 * 管理端角色详情：基础字段 + 当前已勾选的操作 id 列表（某端上下文由接口参数决定）。
 */
public record AdminRoleDetailDto(
		Long id,
		String code,
		String name,
		String roleDesc,
		String status,
		String isInner,
		String isViewAll,
		int sequ,
		List<Long> assignedOpIds
) {
}
