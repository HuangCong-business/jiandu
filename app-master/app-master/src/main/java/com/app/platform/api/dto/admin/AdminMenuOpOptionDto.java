package com.app.platform.api.dto.admin;

/**
 * 管理端为角色勾选「菜单操作」时的选项（按客户端类型筛选）。
 */
public record AdminMenuOpOptionDto(long id, String code, String name, long menuId, String menuName) {
}
