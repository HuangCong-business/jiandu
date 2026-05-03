package com.app.platform.auth;

import com.app.platform.core.authentication.intf.IUser;

import java.util.List;

/**
 * 管理后台访问判定：配置白名单用户 ID 与 IUser 管理标记的并集。
 */
public final class AdminAuthSupport {

	private AdminAuthSupport() {
	}

	/**
	 * 平台管理员：{@code user_type=9}、{@code IS_ADMIN_EMP}（含 {@code super_admin} 应用角色）、或配置 {@code adminUserIds}。
	 */
	public static boolean isPlatformAdmin(IUser user, List<Long> adminUserIds) {
		if (user == null || user.getLoginUserId() == null) {
			return false;
		}
		// 显式配置的超级用户列表优先命中
		if (adminUserIds != null && adminUserIds.contains(user.getLoginUserId())) {
			return true;
		}
		return user.isAdmin();
	}
}
