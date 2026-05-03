package com.app.platform.api.dto.admin;

/** 下拉框用的角色选项（id + 编码 + 名称）。 */
public record AdminRoleOptionDto(Long id, String code, String name) {
}
