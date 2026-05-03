package com.app.platform.api.dto.admin;

import jakarta.validation.constraints.Size;

/** 管理端局部更新角色：各字段均可选，非空表示覆盖。 */
public record AdminPatchRoleRequest(
		@Size(max = 128) String name,
		@Size(max = 512) String roleDesc,
		String status,
		String isViewAll,
		Integer sequ
) {
}
