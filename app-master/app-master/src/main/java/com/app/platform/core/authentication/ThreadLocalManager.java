package com.app.platform.core.authentication;

import com.app.platform.core.authentication.intf.IUser;

/**
 * 请求线程内当前用户缓存；须在请求结束时 {@link #clearUserLocal()} 防止线程池复用泄漏。
 */
public final class ThreadLocalManager {

	private static final ThreadLocal<IUser> USER_LOCAL = new ThreadLocal<>();

	private ThreadLocalManager() {
	}

	/** 由过滤器在解析 Session 后写入当前 IUser。 */
	public static void setUserLocal(IUser user) {
		USER_LOCAL.set(user);
	}

	public static IUser getUserLocal() {
		return USER_LOCAL.get();
	}

	/** 请求结束务必清理，避免下一个请求读到错误用户。 */
	public static void clearUserLocal() {
		USER_LOCAL.remove();
	}
}
