package com.app.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/** 绑定 {@code app.auth.*}：会话、锁定策略、匿名路径与管理员 ID。 */
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

	/**
	 * 历史配置项；当前实现固定使用 {@link com.app.platform.core.authentication.Constants#SESSION_USER}（{@code user}）存放 {@link com.app.platform.core.authentication.intf.IUser}。
	 */
	private String sessionAttributeName = "user";

	// BCrypt 强度，越高越慢越难暴力破解
	private int bcryptStrength = 10;

	private int maxFailedLogins = 5;

	private int lockDurationMinutes = 15;

	/**
	 * Paths that skip authentication. Each entry: {@code METHOD:antPattern}, e.g. {@code POST:/api/auth/login}.
	 */
	private List<String> anonymousPaths = defaultAnonymousPaths();

	/**
	 * 显式平台管理员用户 ID（与 {@code user_type=9}、{@code IS_ADMIN_EMP} 并列，满足其一即可访问 /api/admin）。
	 */
	private List<Long> adminUserIds = new ArrayList<>();

	/** 内置默认白名单：注册登录与健康检查等。 */
	private static List<String> defaultAnonymousPaths() {
		List<String> list = new ArrayList<>();
		list.add("POST:/api/auth/register");
		list.add("POST:/api/auth/login");
		list.add("POST:/api/auth/logout");
		list.add("GET:/actuator/health");
		list.add("GET:/actuator/health/**");
		list.add("GET:/static/**");
		return list;
	}

	public String getSessionAttributeName() {
		return sessionAttributeName;
	}

	public void setSessionAttributeName(String sessionAttributeName) {
		this.sessionAttributeName = sessionAttributeName;
	}

	public int getBcryptStrength() {
		return bcryptStrength;
	}

	public void setBcryptStrength(int bcryptStrength) {
		this.bcryptStrength = bcryptStrength;
	}

	public int getMaxFailedLogins() {
		return maxFailedLogins;
	}

	public void setMaxFailedLogins(int maxFailedLogins) {
		this.maxFailedLogins = maxFailedLogins;
	}

	public int getLockDurationMinutes() {
		return lockDurationMinutes;
	}

	public void setLockDurationMinutes(int lockDurationMinutes) {
		this.lockDurationMinutes = lockDurationMinutes;
	}

	public List<String> getAnonymousPaths() {
		return anonymousPaths;
	}

	public void setAnonymousPaths(List<String> anonymousPaths) {
		this.anonymousPaths = anonymousPaths;
	}

	public List<Long> getAdminUserIds() {
		return adminUserIds;
	}

	public void setAdminUserIds(List<Long> adminUserIds) {
		this.adminUserIds = adminUserIds;
	}
}
