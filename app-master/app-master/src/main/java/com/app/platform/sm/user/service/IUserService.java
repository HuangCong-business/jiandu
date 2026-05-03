package com.app.platform.sm.user.service;

import com.app.platform.core.authentication.intf.IUser;
import jakarta.servlet.http.HttpServletRequest;

/** 用户领域门面：按 id 构建 IUser，以及登录态落 Session/ThreadLocal。 */
public interface IUserService {

	/** 加载并 enrich 后的可序列化用户视图。 */
	IUser getById(Long id);

	/**
	 * 写入登录类型、IP、Session 与 ThreadLocal；可选记录成功登录。
	 */
	void setLoginUser(IUser user, String loginType, HttpServletRequest request, boolean recordLog);
}
