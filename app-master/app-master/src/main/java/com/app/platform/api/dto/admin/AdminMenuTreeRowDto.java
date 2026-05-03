package com.app.platform.api.dto.admin;

/**
 * 管理端构建菜单树用的扁平行（含父节点 id）。
 */
public record AdminMenuTreeRowDto(long id, Long parentId, String name, int sequ) {
}
