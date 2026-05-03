package com.app.platform.core.authentication;

import com.app.platform.core.authentication.impl.User;
import com.app.platform.core.authentication.intf.IUser;
import com.app.platform.permission.OperationPermissionEvaluator;
import com.app.platform.sm.user.service.IUserService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Optional;

/**
 * 门面：ThreadLocal / Session 取当前用户；写会话委托 {@code userServiceImpl}；操作码委托 {@link OperationPermissionEvaluator}。
 */
@Component
public class UserManager {

	// 单例引用，供 static 方法在非 Spring 调用点访问 Bean
	private static UserManager instance;

	private final IUserService userService;
	private final OperationPermissionEvaluator operationPermissionEvaluator;

	/** 注入用户服务与操作权限评估器。 */
	public UserManager(IUserService userService, OperationPermissionEvaluator operationPermissionEvaluator) {
		this.userService = userService;
		this.operationPermissionEvaluator = operationPermissionEvaluator;
	}

	/** 容器就绪后注册静态门面，避免 static 方法拿不到 Bean。 */
	@PostConstruct
	void registerStatic() {
		instance = this;
	}

	/** 将登录用户写入 Session/ThreadLocal，可选记审计日志。 */
	public static void setLoginUser(IUser user, String loginType, HttpServletRequest request, boolean recordLog) {
		instance.userService.setLoginUser(user, loginType, request, recordLog);
	}

	public static IUser getLoginUser() {
		// 优先 ThreadLocal（过滤器等已设置），否则回退 Session
		IUser local = ThreadLocalManager.getUserLocal();
		if (local != null) {
			return local;
		}
		return sessionUser().orElseGet(User::anonymous);
	}

	public static Long getLoginUserId() {
		Long id = getLoginUser().getLoginUserId();
		// null 视为匿名用户 ID，避免 NPE 传播
		return id != null ? id : Constants.ANONYMOUS_USER_ID;
	}

	public static boolean isAnonymous() {
		return Constants.ANONYMOUS_USER_ID == getLoginUserId();
	}

	/** 当前用户是否拥有某菜单下指定操作码。 */
	public static boolean isOpAllow(Long menuId, String opCode) {
		return instance.operationPermissionEvaluator.isOpAllowForCurrentUser(menuId, opCode);
	}

	/** 当前用户在指定菜单下可见的操作码列表。 */
	public static List<String> getOpCodes(Long menuId) {
		return instance.operationPermissionEvaluator.getOpCodesForCurrentUser(menuId);
	}

	private static Optional<IUser> sessionUser() {
		ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		// 非 Web 请求上下文下无 Session
		if (attrs == null) {
			return Optional.empty();
		}
		HttpSession session = attrs.getRequest().getSession(false);
		if (session == null) {
			return Optional.empty();
		}
		Object raw = session.getAttribute(Constants.SESSION_USER);
		// 仅接受实现了 IUser 的会话属性，防止反序列化污染
		if (raw instanceof IUser iu) {
			return Optional.of(iu);
		}
		return Optional.empty();
	}
}
