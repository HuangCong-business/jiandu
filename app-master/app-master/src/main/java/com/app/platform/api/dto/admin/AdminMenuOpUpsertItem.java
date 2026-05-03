package com.app.platform.api.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 替换菜单操作定义时的单行项：{@code id} 为空表示新建，非空表示更新已有行。
 */
public record AdminMenuOpUpsertItem(
		Long id,
		@NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9._-]+") String code,
		@Size(max = 128) String name,
		@NotNull Integer sequ
) {
}
