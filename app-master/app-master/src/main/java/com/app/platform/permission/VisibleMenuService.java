package com.app.platform.permission;

import com.app.platform.api.dto.permission.MenuVisibleNodeDto;
import com.app.platform.core.authentication.UserManager;
import com.app.platform.core.authentication.intf.IUser;
import com.app.platform.sm.menu.domain.AppMenu;
import com.app.platform.sm.menu.repository.AppMenuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 当前登录用户可见菜单树（MVP）：管理员/超管见该客户端全部启用菜单；普通用户见角色分配操作所挂菜单及其祖先。
 */
@Service
public class VisibleMenuService {

	private final AppMenuRepository appMenuRepository;

	public VisibleMenuService(AppMenuRepository appMenuRepository) {
		this.appMenuRepository = appMenuRepository;
	}

	/** 返回当前用户在某客户端类型下的可见菜单森林（根节点列表）。 */
	@Transactional(readOnly = true)
	public List<MenuVisibleNodeDto> visibleTree(String clientType) {
		if (clientType == null || clientType.isBlank()) {
			clientType = "1";
		}
		IUser u = UserManager.getLoginUser();
		Long userId = u.getLoginUserId();
		if (userId == null) {
			return List.of();
		}

		List<AppMenu> all = appMenuRepository.findByClientTypeAndStatusOrderBySequAsc(clientType, "1");
		if (all.isEmpty()) {
			return List.of();
		}

		Map<Long, AppMenu> byId = new HashMap<>();
		for (AppMenu m : all) {
			byId.put(m.getId(), m);
		}

		Set<Long> keep = computeVisibleMenuIds(clientType, u, userId, all, byId);
		List<AppMenu> filtered = all.stream().filter(m -> keep.contains(m.getId())).toList();
		return buildForest(filtered);
	}

	/** 单菜单校验：用于 safe 模式拉取操作码前的可见性检查。 */
	@Transactional(readOnly = true)
	public boolean isMenuVisibleToCurrentUser(long menuId, String clientType) {
		if (clientType == null || clientType.isBlank()) {
			clientType = "1";
		}
		AppMenu menu = appMenuRepository.findById(menuId).orElse(null);
		if (menu == null || !"1".equals(menu.getStatus())) {
			return false;
		}
		if (!clientType.equals(menu.getClientType())) {
			return false;
		}
		IUser u = UserManager.getLoginUser();
		Long userId = u.getLoginUserId();
		if (userId == null) {
			return false;
		}
		List<AppMenu> all = appMenuRepository.findByClientTypeAndStatusOrderBySequAsc(clientType, "1");
		if (all.isEmpty()) {
			return false;
		}
		Map<Long, AppMenu> byId = new HashMap<>();
		for (AppMenu m : all) {
			byId.put(m.getId(), m);
		}
		return computeVisibleMenuIds(clientType, u, userId, all, byId).contains(menuId);
	}

	/**
	 * 管理员见全部启用菜单；普通用户见分配菜单及其祖先链。
	 */
	private Set<Long> computeVisibleMenuIds(String clientType, IUser u, long userId, List<AppMenu> all,
			Map<Long, AppMenu> byId) {
		Set<Long> keep = new HashSet<>();
		if (u.isAdmin() || u.isSuperAdmin()) {
			for (AppMenu m : all) {
				keep.add(m.getId());
			}
		}
		else {
			List<Long> direct = appMenuRepository.findVisibleMenuIdsForAssignedUser(userId, clientType);
			for (Long mid : direct) {
				expandAncestors(mid, byId, keep);
			}
		}
		return keep;
	}

	/** 自叶子向上补齐父节点，保证树结构完整。 */
	private static void expandAncestors(Long menuId, Map<Long, AppMenu> byId, Set<Long> keep) {
		Long cur = menuId;
		while (cur != null) {
			AppMenu m = byId.get(cur);
			if (m == null) {
				break;
			}
			keep.add(cur);
			cur = m.getParentId();
		}
	}

	/** 扁平列表 → id 映射 → 挂父子关系 → 多根排序。 */
	private static List<MenuVisibleNodeDto> buildForest(List<AppMenu> menus) {
		Map<Long, MenuVisibleNodeDto> nodes = new HashMap<>();
		for (AppMenu m : menus) {
			MenuVisibleNodeDto n = new MenuVisibleNodeDto();
			n.setId(m.getId());
			n.setParentId(m.getParentId());
			n.setName(m.getName());
			n.setPath(m.getPath());
			n.setIcon(m.getIcon());
			n.setMenuType(m.getMenuType());
			n.setSequ(m.getSequ());
			nodes.put(m.getId(), n);
		}
		List<MenuVisibleNodeDto> roots = new ArrayList<>();
		for (AppMenu m : menus) {
			MenuVisibleNodeDto n = nodes.get(m.getId());
			Long pid = m.getParentId();
			if (pid != null && nodes.containsKey(pid)) {
				nodes.get(pid).getChildren().add(n);
			}
			else {
				roots.add(n);
			}
		}
		sortTree(roots);
		return roots;
	}

	/** 按 sequ 再按 id 递归排序，保证前端展示顺序稳定。 */
	private static void sortTree(List<MenuVisibleNodeDto> level) {
		level.sort(Comparator.comparingInt(MenuVisibleNodeDto::getSequ)
				.thenComparing(MenuVisibleNodeDto::getId, Comparator.nullsLast(Long::compareTo)));
		for (MenuVisibleNodeDto n : level) {
			sortTree(n.getChildren());
		}
	}
}
