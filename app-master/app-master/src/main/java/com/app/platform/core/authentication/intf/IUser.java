package com.app.platform.core.authentication.intf;

import java.io.Serializable;
import java.util.List;

import com.app.platform.core.authentication.Constants;
import com.app.platform.core.authentication.RoleSnapshot;

/**
 * 运行时登录用户抽象；属性键与历史基座 IUser 对齐。
 */
public interface IUser extends Serializable {

	// —— 基础身份（Map 键与历史 IUser 对齐）——
	String USERID = "id";
	String CODE = "code";
	String NAME = "name";
	String USERTYPE = "user_type";
	/** 平台管理会话标记：由 {@code user_type=9} 或 {@link Constants#APP_ROLE_CODE_SUPER_ADMIN} 角色注入。 */
	String IS_ADMIN_EMP = "is_admin_emp";
	String ROLE_LIST = "ROLE_LIST";
	// 组织与汇报关系
	String DEPT_ID = "dept_id";
	String DEPT_NAME = "dept_name";
	String IS_LEADER = "is_leader";
	String SUPERIOR_ID = "superior_id";
	String MOBILE = "mobile";
	String FACE_TIME = "face_time";
	// 会话与终端信息
	String LOGIN_TYPE = "login_type";
	String IP = "ip";
	String ORIGIN_TYPE = "origin_type";
	String STATUS = "status";
	// 经销商维度（多租户扩展位）
	String DEALER_ID = "dealer_id";
	String DEALER_NAME = "dealer_name";
	String DEALER_CODE = "dealer_code";

	Long getLoginUserId();

	String getUserCode();

	String getDisplayName();

	boolean isAdmin();

	/** 严格超管：仅 {@code user_type == 9}，不含仅 {@code IS_ADMIN_EMP} 的委派管理员。 */
	default boolean isSuperAdmin() {
		Object ut = getProperty(USERTYPE);
		return ut instanceof Number n && n.shortValue() == Constants.USER_TYPE_SYS_ADMIN;
	}

	@SuppressWarnings("unchecked")
	default List<RoleSnapshot> getRoleList() {
		Object v = getProperty(ROLE_LIST);
		// 非 List 或类型不符时返回空列表，避免调用方 NPE
		if (v instanceof List<?> list) {
			return (List<RoleSnapshot>) list;
		}
		return List.of();
	}

	Object getProperty(String key);

	void setProperty(String key, Object value);
}
