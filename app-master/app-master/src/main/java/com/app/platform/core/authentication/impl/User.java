package com.app.platform.core.authentication.impl;

import java.io.Serial;
import java.util.LinkedHashMap;

import com.app.platform.core.authentication.Constants;
import com.app.platform.core.authentication.intf.IUser;

/**
 * 可序列化的属性包，语义上等同历史基座的 {@code User}。
 */
public class User extends LinkedHashMap<String, Object> implements IUser {

	@Serial
	private static final long serialVersionUID = 1L;

	/** 构造匿名占位用户，仅含 ANONYMOUS_USER_ID。 */
	public static User anonymous() {
		User u = new User();
		u.put(USERID, Constants.ANONYMOUS_USER_ID);
		return u;
	}

	@Override
	public Long getLoginUserId() {
		Object v = get(USERID);
		// 兼容 Integer/Long 等数值类型反序列化
		if (v instanceof Number n) {
			return n.longValue();
		}
		return null;
	}

	@Override
	public String getUserCode() {
		Object v = get(CODE);
		return v != null ? v.toString() : null;
	}

	@Override
	public String getDisplayName() {
		Object v = get(NAME);
		return v != null ? v.toString() : null;
	}

	@Override
	public boolean isAdmin() {
		// 委派管理员或系统用户类型均视为管理会话
		if (Boolean.TRUE.equals(get(IS_ADMIN_EMP))) {
			return true;
		}
		Object ut = get(USERTYPE);
		return ut instanceof Number n && n.shortValue() == Constants.USER_TYPE_SYS_ADMIN;
	}

	@Override
	public Object getProperty(String key) {
		return get(key);
	}

	@Override
	public void setProperty(String key, Object value) {
		put(key, value);
	}
}
