package com.app.platform.permission;

import com.app.platform.core.authentication.Constants;
import com.app.platform.core.authentication.UserManager;
import com.app.platform.core.authentication.intf.IUser;
import com.app.platform.sm.menu.domain.AppMenu;
import com.app.platform.sm.menu.repository.AppMenuRepository;
import com.app.platform.sm.menu.repository.AppOpSecurityRepository;
import com.app.platform.sm.user.service.IUserService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 菜单操作码判定（MVP）：仅 app_op_security + app_op_assign + sm_role_user + app_role；
 * 仅 {@code group_id IS NULL}；不含 app_func_security*；无 isSuperAdmin 与 isAdmin 合并（超管菜单仅 user_type=9）。
 */
@Service
public class OperationPermissionEvaluator {

	private final AppMenuRepository appMenuRepository;
	private final AppOpSecurityRepository appOpSecurityRepository;
	private final OpAssignCache opAssignCache;
	private final IUserService userService;

	/** 注入菜单、操作定义、缓存与 IUser 装载服务。 */
	public OperationPermissionEvaluator(AppMenuRepository appMenuRepository,
			AppOpSecurityRepository appOpSecurityRepository,
			OpAssignCache opAssignCache,
			IUserService userService) {
		this.appMenuRepository = appMenuRepository;
		this.appOpSecurityRepository = appOpSecurityRepository;
		this.opAssignCache = opAssignCache;
		this.userService = userService;
	}

	/** 基于当前 Session 用户解析 originType 后计算某菜单下可见操作码。 */
	public List<String> getOpCodesForCurrentUser(Long menuId) {
		if (UserManager.isAnonymous()) {
			return List.of();
		}
		IUser sessionUser = UserManager.getLoginUser();
		Long userId = sessionUser.getLoginUserId();
		if (userId == null) {
			return List.of();
		}
		String originType = Optional.ofNullable(sessionUser.getProperty(IUser.ORIGIN_TYPE))
				.map(Object::toString)
				.orElse(Constants.ORIGIN_TYPE_EMPLOYEE);
		return getOpCodes(originType, userId, menuId);
	}

	public List<String> getOpCodes(String originType, Long userId, Long menuId) {
		if (menuId == null || menuId < 0) {
			return List.of();
		}
		Optional<AppMenu> menuOpt = appMenuRepository.findById(menuId);
		if (menuOpt.isEmpty()) {
			return List.of();
		}
		AppMenu menu = menuOpt.get();
		// 停用菜单不返回任何操作
		if (!"1".equals(menu.getStatus())) {
			return List.of();
		}

		IUser user;
		try {
			user = userService.getById(userId);
		}
		catch (RuntimeException ex) {
			return List.of();
		}
		if (user.getLoginUserId() == null) {
			return List.of();
		}

		// 当前仅实现员工端分支语义；其它 origin 走角色分配
		if (!Constants.ORIGIN_TYPE_EMPLOYEE.equals(originType)) {
			return loadAssigned(userId, menuId);
		}

		String menuType = menu.getMenuType() != null ? menu.getMenuType() : Constants.MENU_TYPE_EMP;

		if (Constants.MENU_TYPE_ADMIN.equals(menuType)) {
			if (user.isAdmin()) {
				return loadAllMenuOps(menuId);
			}
			return loadAssigned(userId, menuId);
		}
		if (Constants.MENU_TYPE_SUPER_ADMIN.equals(menuType)) {
			// 超管菜单仅 user_type=9 可见全量操作
			if (user.isSuperAdmin()) {
				return loadAllMenuOps(menuId);
			}
			return loadAssigned(userId, menuId);
		}
		// ALL、EMP 等
		if (user.isAdmin()) {
			return loadAllMenuOps(menuId);
		}
		return loadAssigned(userId, menuId);
	}

	/** 判断当前用户是否拥有指定操作码（忽略大小写）。 */
	public boolean isOpAllowForCurrentUser(Long menuId, String opCode) {
		if (opCode == null || opCode.isBlank()) {
			return false;
		}
		if (menuId == null || menuId < 0) {
			return false;
		}
		if (UserManager.isAnonymous()) {
			return false;
		}
		IUser u = UserManager.getLoginUser();
		Long userId = u.getLoginUserId();
		if (userId == null) {
			return false;
		}
		String normalized = opCode.trim();
		List<String> codes = getOpCodesForCurrentUser(menuId);
		return codes.stream().anyMatch(c -> c.equalsIgnoreCase(normalized));
	}

	/** 管理员/满足菜单类型时：该菜单下全部启用操作码（走缓存）。 */
	private List<String> loadAllMenuOps(long menuId) {
		return opAssignCache.getMenuOpCodes(menuId, () -> appOpSecurityRepository.findAllActiveOpCodesForMenu(menuId));
	}

	/** 普通用户：角色+分配表交集得到的操作码（走缓存）。 */
	private List<String> loadAssigned(long userId, long menuId) {
		return opAssignCache.getAssignedOpCodes(userId, menuId,
				() -> appOpSecurityRepository.findAssignedOpCodesForUserAndMenu(userId, menuId));
	}
}
