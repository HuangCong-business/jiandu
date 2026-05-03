package com.app.platform.api.dto.admin;

import java.util.List;

/**
 * 管理端用户分页结果：内容与 Spring Data 风格分页元数据字段对齐。
 */
public record AdminUserPageDto(
		List<AdminUserRowDto> content,
		long totalElements,
		int totalPages,
		int number,
		int size) {
}
