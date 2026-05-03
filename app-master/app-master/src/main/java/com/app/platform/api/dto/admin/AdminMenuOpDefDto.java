package com.app.platform.api.dto.admin;

/** 管理端维护某菜单下「默认」操作定义（groupId 为空），含已停用行便于展示 */
public record AdminMenuOpDefDto(long id, String code, String name, int sequ, String status) {
}
