package com.app.platform.api.dto.admin;

import java.util.List;

/** 全量替换用户角色绑定；null 入参在紧凑构造中归一为不可变空列表。 */
public record AdminReplaceRolesRequest(List<Long> roleIds) {

	public AdminReplaceRolesRequest {
		if (roleIds == null) {
			roleIds = List.of();
		}
	}
}
