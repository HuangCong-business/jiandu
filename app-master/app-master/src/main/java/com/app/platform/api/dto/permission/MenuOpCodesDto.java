package com.app.platform.api.dto.permission;

import java.util.List;

/**
 * 某菜单下用户被授予的操作编码列表；{@code menuId} 以字符串形式与 JSON 序列化约定一致。
 */
public record MenuOpCodesDto(String menuId, List<String> opCodes) {
}
