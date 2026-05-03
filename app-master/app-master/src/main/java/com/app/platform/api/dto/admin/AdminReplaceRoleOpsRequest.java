package com.app.platform.api.dto.admin;

import java.util.List;

/**
 * 全量替换某角色在菜单操作上的授权：请求体为操作主键 id 列表。
 */
public record AdminReplaceRoleOpsRequest(List<Long> opIds) {
}
