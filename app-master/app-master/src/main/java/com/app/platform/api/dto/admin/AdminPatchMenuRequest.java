package com.app.platform.api.dto.admin;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 管理端局部更新菜单：{@code clearParent} 为 true 时可显式清空父节点（与业务层约定配合）。
 */
public record AdminPatchMenuRequest(
		@Size(max = 128) String name,
		@Size(max = 256) String path,
		@Size(max = 128) String icon,
		@Pattern(regexp = "[0123]") String menuType,
		Integer sequ,
		@Pattern(regexp = "[01]") String status,
		@Size(max = 16) String controlType,
		Long parentId,
		Boolean clearParent
) {
}
