package com.app.platform.api.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 管理端创建菜单请求：名称必填，其余为可选展示与路由信息。
 * {@code menuType} 取值 0–3，{@code clientType} 标识 Web/App 等多端。
 */
public record AdminCreateMenuRequest(
		@NotBlank @Size(max = 128) String name,
		Long parentId,
		@Size(max = 256) String path,
		@Size(max = 128) String icon,
		@Pattern(regexp = "[0123]") String menuType,
		@Size(max = 16) String clientType,
		Integer sequ,
		@Size(max = 16) String controlType
) {
}
