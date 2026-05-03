package com.app.platform.api.dto.admin;

import jakarta.validation.Valid;

import java.util.List;

/**
 * 全量替换某菜单下 groupId 为空的操作定义：请求中出现的 id 更新/保留为启用，未出现且已存在的行逻辑停用。
 */
public record AdminReplaceMenuOpDefsRequest(@Valid List<AdminMenuOpUpsertItem> items) {
	public AdminReplaceMenuOpDefsRequest {
		// 防御性拷贝，避免外部修改传入列表
		items = items == null ? List.of() : List.copyOf(items);
	}
}
