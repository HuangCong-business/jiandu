package com.app.platform.api.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 管理端新建角色：编码需符合字母数字与少量符号规则。 */
public record AdminCreateRoleRequest(
		@NotBlank @Size(max = 64) @Pattern(regexp = "[a-zA-Z0-9._-]+") String code,
		@NotBlank @Size(max = 128) String name,
		@Size(max = 512) String roleDesc
) {
}
