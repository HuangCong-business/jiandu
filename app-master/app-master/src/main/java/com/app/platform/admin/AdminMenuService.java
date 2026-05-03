package com.app.platform.admin;

import com.app.platform.api.dto.admin.AdminAssignTreeOpDto;
import com.app.platform.api.dto.admin.AdminCreateMenuRequest;
import com.app.platform.api.dto.admin.AdminMenuAssignNodeDto;
import com.app.platform.api.dto.admin.AdminMenuOpDefDto;
import com.app.platform.api.dto.admin.AdminMenuOpUpsertItem;
import com.app.platform.api.dto.admin.AdminMenuRowDto;
import com.app.platform.api.dto.admin.AdminMenuTreeRowDto;
import com.app.platform.api.dto.admin.AdminPatchMenuRequest;
import com.app.platform.api.dto.admin.AdminReplaceMenuOpDefsRequest;
import com.app.platform.core.authentication.Constants;
import com.app.platform.core.authentication.UserManager;
import com.app.platform.exception.BadRequestException;
import com.app.platform.exception.MenuNotFoundException;
import com.app.platform.permission.OpAssignCache;
import com.app.platform.sm.menu.domain.AppMenu;
import com.app.platform.sm.menu.domain.AppOpSecurity;
import com.app.platform.sm.menu.repository.AppMenuRepository;
import com.app.platform.sm.menu.repository.AppOpSecurityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端菜单与默认操作定义：树展示、分配树、创建修改、批量替换操作、子树逻辑删除。
 */
@Service
public class AdminMenuService {

	private static final String MENU_STATUS_NORMAL = "1";
	private static final String MENU_STATUS_DELETED = "0";

	/** 与前端 MenuBranch 约定：空或该值时渲染默认侧栏 SVG */
	public static final String DEFAULT_MENU_ICON = "sidebar-default";

	private final AppMenuRepository appMenuRepository;
	private final AppOpSecurityRepository appOpSecurityRepository;
	private final OpAssignCache opAssignCache;

	public AdminMenuService(AppMenuRepository appMenuRepository, AppOpSecurityRepository appOpSecurityRepository,
			OpAssignCache opAssignCache) {
		this.appMenuRepository = appMenuRepository;
		this.appOpSecurityRepository = appOpSecurityRepository;
		this.opAssignCache = opAssignCache;
	}

	/** 扁平树行（含父 id），供表格树组件展示。 */
	@Transactional(readOnly = true)
	public List<AdminMenuTreeRowDto> listTreeRows(String clientType) {
		if (clientType == null || clientType.isBlank()) {
			clientType = "1";
		}
		return appMenuRepository
				.findWithParentByClientTypeAndStatusOrderBySequAscIdAsc(clientType, MENU_STATUS_NORMAL)
				.stream()
				.map(AdminMenuService::toTreeRow)
				.toList();
	}

	/** 构建「菜单 + 可选操作」嵌套树，供角色授权勾选。 */
	@Transactional(readOnly = true)
	public List<AdminMenuAssignNodeDto> buildAssignTree(String clientType) {
		if (clientType == null || clientType.isBlank()) {
			clientType = "1";
		}
		List<AppMenu> menus = appMenuRepository
				.findWithParentByClientTypeAndStatusOrderBySequAscIdAsc(clientType, MENU_STATUS_NORMAL);
		List<AppOpSecurity> ops = appOpSecurityRepository.findSelectableOpsByClientType(clientType);
		Map<Long, List<AdminAssignTreeOpDto>> opsByMenu = ops.stream()
				.collect(Collectors.groupingBy(o -> o.getMenu().getId(),
						Collectors.mapping(AdminMenuService::toAssignOp, Collectors.toList())));
		Map<Long, List<AppMenu>> byParent = new HashMap<>();
		for (AppMenu m : menus) {
			Long pid = m.getParentId();
			byParent.computeIfAbsent(pid, k -> new ArrayList<>()).add(m);
		}
		for (List<AppMenu> ch : byParent.values()) {
			ch.sort(Comparator.comparingInt(AppMenu::getSequ).thenComparing(AppMenu::getId, Comparator.nullsLast(Long::compareTo)));
		}
		List<AppMenu> roots = byParent.getOrDefault(null, List.of());
		return roots.stream().map(r -> toAssignNode(r, byParent, opsByMenu)).toList();
	}

	/** DTO 映射：操作展示名缺省为空串。 */
	private static AdminAssignTreeOpDto toAssignOp(AppOpSecurity o) {
		String n = o.getName();
		return new AdminAssignTreeOpDto(o.getId(), o.getCode(), n == null ? "" : n);
	}

	private static AdminMenuAssignNodeDto toAssignNode(AppMenu m, Map<Long, List<AppMenu>> byParent,
			Map<Long, List<AdminAssignTreeOpDto>> opsByMenu) {
		List<AdminAssignTreeOpDto> opList = List.copyOf(opsByMenu.getOrDefault(m.getId(), List.of()));
		List<AppMenu> rawKids = byParent.getOrDefault(m.getId(), List.of());
		List<AdminMenuAssignNodeDto> kids = rawKids.stream()
				.map(ch -> toAssignNode(ch, byParent, opsByMenu))
				.toList();
		return new AdminMenuAssignNodeDto(m.getId(), m.getParentId(), m.getName(), m.getSequ(), opList, kids);
	}

	/** 新建菜单：校验父节点客户端一致性与删除状态。 */
	@Transactional
	public AdminMenuRowDto create(AdminCreateMenuRequest req) {
		String clientType = normalizeClientType(req.clientType());
		String menuType = (req.menuType() == null || req.menuType().isBlank())
				? Constants.MENU_TYPE_EMP
				: req.menuType().trim();
		AppMenu m = new AppMenu();
		m.setName(req.name().trim());
		m.setClientType(clientType);
		m.setMenuType(menuType);
		m.setStatus(MENU_STATUS_NORMAL);
		m.setSequ(req.sequ() != null ? req.sequ() : 0);
		if (req.path() != null && !req.path().isBlank()) {
			m.setPath(req.path().trim());
		}
		if (req.icon() != null && !req.icon().isBlank()) {
			m.setIcon(req.icon().trim());
		}
		else {
			m.setIcon(DEFAULT_MENU_ICON);
		}
		if (req.controlType() != null && !req.controlType().isBlank()) {
			m.setControlType(req.controlType().trim());
		}
		if (req.parentId() != null) {
			AppMenu parent = appMenuRepository.findById(req.parentId())
					.orElseThrow(() -> new BadRequestException("父菜单不存在"));
			if (MENU_STATUS_DELETED.equals(parent.getStatus())) {
				throw new BadRequestException("父菜单已删除");
			}
			if (!clientType.equals(parent.getClientType())) {
				throw new BadRequestException("父菜单客户端类型不一致");
			}
			m.setParent(parent);
		}
		Long uid = UserManager.getLoginUserId();
		if (uid != null) {
			m.setCreatorId(uid);
		}
		appMenuRepository.save(m);
		return toRow(m);
	}

	/**
	 * 局部更新展示字段；父节点变更时校验环与客户端类型，保存后失效该菜单缓存。
	 */
	@Transactional
	public AdminMenuRowDto patch(long id, AdminPatchMenuRequest req) {
		AppMenu m = appMenuRepository.findById(id).orElseThrow(MenuNotFoundException::new);
		if (MENU_STATUS_DELETED.equals(m.getStatus())) {
			throw new MenuNotFoundException();
		}
		// 以下各 if 仅处理非 null 请求字段，实现 PATCH 语义
		if (req.name() != null && !req.name().isBlank()) {
			m.setName(req.name().trim());
		}
		if (req.path() != null) {
			m.setPath(req.path().isBlank() ? null : req.path().trim());
		}
		if (req.icon() != null) {
			m.setIcon(req.icon().isBlank() ? DEFAULT_MENU_ICON : req.icon().trim());
		}
		if (req.menuType() != null && !req.menuType().isBlank()) {
			m.setMenuType(req.menuType().trim());
		}
		if (req.sequ() != null) {
			m.setSequ(req.sequ());
		}
		if (req.status() != null && !req.status().isBlank()) {
			m.setStatus(req.status().trim());
		}
		if (req.controlType() != null) {
			m.setControlType(req.controlType().isBlank() ? null : req.controlType().trim());
		}
		if (Boolean.TRUE.equals(req.clearParent())) {
			m.setParent(null);
		}
		else if (req.parentId() != null) {
			if (req.parentId().equals(id)) {
				throw new BadRequestException("不能将自身设为父菜单");
			}
			AppMenu parent = appMenuRepository.findById(req.parentId())
					.orElseThrow(() -> new BadRequestException("父菜单不存在"));
			if (MENU_STATUS_DELETED.equals(parent.getStatus())) {
				throw new BadRequestException("父菜单已删除");
			}
			if (!m.getClientType().equals(parent.getClientType())) {
				throw new BadRequestException("父菜单客户端类型不一致");
			}
			if (wouldCreateCycle(id, req.parentId())) {
				throw new BadRequestException("不能形成环状菜单层级");
			}
			m.setParent(parent);
		}
		Long uid = UserManager.getLoginUserId();
		if (uid != null) {
			m.setModifyierId(uid);
		}
		appMenuRepository.save(m);
		opAssignCache.evictMenu(id);
		return toRow(m);
	}

	/** 单条菜单 DTO；已删除则 404。 */
	@Transactional(readOnly = true)
	public AdminMenuRowDto getRow(long id) {
		AppMenu m = appMenuRepository.findById(id).orElseThrow(MenuNotFoundException::new);
		if (MENU_STATUS_DELETED.equals(m.getStatus())) {
			throw new MenuNotFoundException();
		}
		return toRow(m);
	}

	@Transactional(readOnly = true)
	public List<AdminMenuOpDefDto> listOpDefinitions(long menuId) {
		assertMenuExistsAndActive(menuId);
		return appOpSecurityRepository.findByMenu_IdAndGroupIdIsNullOrderBySequAscIdAsc(menuId).stream()
				.map(AdminMenuService::toOpDef)
				.toList();
	}

	/**
	 * 以请求体全量描述替换该菜单下默认操作定义（groupId 为空）；未出现在列表中的旧行逻辑停用。
	 */
	@Transactional
	public List<AdminMenuOpDefDto> replaceOpDefinitions(long menuId, AdminReplaceMenuOpDefsRequest req) {
		AppMenu menu = appMenuRepository.findById(menuId).orElseThrow(MenuNotFoundException::new);
		if (MENU_STATUS_DELETED.equals(menu.getStatus())) {
			throw new MenuNotFoundException();
		}
		List<AdminMenuOpUpsertItem> items = req.items();
		boolean hasView = items.stream().anyMatch(i -> "VIEW".equalsIgnoreCase(i.code().trim()));
		if (!hasView) {
			throw new BadRequestException("菜单须至少包含 VIEW（查看）操作码");
		}
		Set<String> codesSeen = new HashSet<>();
		Set<Long> keptIds = new HashSet<>();
		// 逐项 upsert：带 id 则更新，否则按 code 查找或新建
		for (AdminMenuOpUpsertItem item : items) {
			String code = item.code().trim();
			if (!codesSeen.add(code)) {
				throw new BadRequestException("操作编码重复: " + code);
			}
			int sequ = item.sequ() != null ? item.sequ() : 0;
			String name = item.name() == null ? "" : item.name().trim();
			if (item.id() != null) {
				AppOpSecurity o = appOpSecurityRepository.findById(item.id())
						.orElseThrow(() -> new BadRequestException("无效的操作 id: " + item.id()));
				if (!o.getMenu().getId().equals(menuId) || o.getGroupId() != null) {
					throw new BadRequestException("操作不属于该菜单或不是可维护的默认行: " + item.id());
				}
				if (appOpSecurityRepository.existsByMenu_IdAndGroupIdIsNullAndCodeAndIdNot(menuId, code, o.getId())) {
					throw new BadRequestException("操作编码已被占用: " + code);
				}
				o.setCode(code);
				o.setName(name.isEmpty() ? null : name);
				o.setSequ(sequ);
				o.setStatus("1");
				appOpSecurityRepository.save(o);
				keptIds.add(o.getId());
			}
			else {
				Optional<AppOpSecurity> existing = appOpSecurityRepository.findByMenu_IdAndGroupIdIsNullAndCode(menuId, code);
				if (existing.isPresent()) {
					AppOpSecurity o = existing.get();
					if (keptIds.contains(o.getId())) {
						throw new BadRequestException("操作编码重复: " + code);
					}
					o.setName(name.isEmpty() ? null : name);
					o.setSequ(sequ);
					o.setStatus("1");
					appOpSecurityRepository.save(o);
					keptIds.add(o.getId());
				}
				else {
					AppOpSecurity o = new AppOpSecurity();
					o.setMenu(menu);
					o.setCode(code);
					o.setName(name.isEmpty() ? null : name);
					o.setGroupId(null);
					o.setSequ(sequ);
					o.setStatus("1");
					appOpSecurityRepository.save(o);
					keptIds.add(o.getId());
				}
			}
		}
		List<AppOpSecurity> allForMenu = appOpSecurityRepository.findByMenu_IdAndGroupIdIsNullOrderBySequAscIdAsc(menuId);
		// 请求未包含的已启用行改为停用（软删除）
		for (AppOpSecurity o : allForMenu) {
			if (!keptIds.contains(o.getId()) && "1".equals(o.getStatus())) {
				o.setStatus("0");
				appOpSecurityRepository.save(o);
			}
		}
		opAssignCache.evictMenu(menuId);
		opAssignCache.evictAll();
		return listOpDefinitions(menuId);
	}

	private void assertMenuExistsAndActive(long menuId) {
		AppMenu m = appMenuRepository.findById(menuId).orElseThrow(MenuNotFoundException::new);
		if (MENU_STATUS_DELETED.equals(m.getStatus())) {
			throw new MenuNotFoundException();
		}
	}

	private static AdminMenuOpDefDto toOpDef(AppOpSecurity o) {
		String n = o.getName();
		return new AdminMenuOpDefDto(o.getId(), o.getCode(), n == null ? "" : n, o.getSequ(), o.getStatus());
	}

	/**
	 * 逻辑删除菜单及其整棵子树：先停用各节点下默认操作码（groupId 为空），再自叶向根逻辑删除菜单。
	 */
	@Transactional
	public void deleteLogical(long id) {
		AppMenu root = appMenuRepository.findById(id).orElseThrow(MenuNotFoundException::new);
		if (MENU_STATUS_DELETED.equals(root.getStatus())) {
			throw new MenuNotFoundException();
		}
		List<Long> bfsOrder = collectSubtreeMenuIdsBfs(id);
		List<Long> leafToRoot = new ArrayList<>(bfsOrder);
		Collections.reverse(leafToRoot);
		Long uid = UserManager.getLoginUserId();
		for (Long mid : bfsOrder) {
			for (AppOpSecurity o : appOpSecurityRepository.findByMenu_IdAndGroupIdIsNullOrderBySequAscIdAsc(mid)) {
				if (MENU_STATUS_NORMAL.equals(o.getStatus())) {
					o.setStatus(MENU_STATUS_DELETED);
					appOpSecurityRepository.save(o);
				}
			}
			opAssignCache.evictMenu(mid);
		}
		for (Long mid : leafToRoot) {
			AppMenu m = appMenuRepository.findById(mid).orElse(null);
			if (m != null && MENU_STATUS_NORMAL.equals(m.getStatus())) {
				m.setStatus(MENU_STATUS_DELETED);
				if (uid != null) {
					m.setModifyierId(uid);
				}
				appMenuRepository.save(m);
			}
		}
		opAssignCache.evictAll();
	}

	/** 含根节点；仅 status=正常的子节点；BFS 顺序为根先、叶后。 */
	private List<Long> collectSubtreeMenuIdsBfs(long rootId) {
		List<Long> order = new ArrayList<>();
		Deque<Long> q = new ArrayDeque<>();
		q.add(rootId);
		while (!q.isEmpty()) {
			Long cur = q.removeFirst();
			order.add(cur);
			for (AppMenu child : appMenuRepository.findByParent_IdAndStatus(cur, MENU_STATUS_NORMAL)) {
				q.add(child.getId());
			}
		}
		return order;
	}

	private boolean wouldCreateCycle(long selfId, long newParentId) {
		Long cur = newParentId;
		int guard = 0;
		while (cur != null && guard++ < 256) {
			if (cur.equals(selfId)) {
				return true;
			}
			AppMenu p = appMenuRepository.findById(cur).orElse(null);
			if (p == null) {
				break;
			}
			cur = p.getParentId();
		}
		return false;
	}

	private static String normalizeClientType(String clientType) {
		if (clientType == null || clientType.isBlank()) {
			return "1";
		}
		return clientType.trim();
	}

	private static AdminMenuTreeRowDto toTreeRow(AppMenu m) {
		Long parentId = m.getParent() == null ? null : m.getParent().getId();
		return new AdminMenuTreeRowDto(m.getId(), parentId, m.getName(), m.getSequ());
	}

	private static AdminMenuRowDto toRow(AppMenu m) {
		return new AdminMenuRowDto(m.getId(), m.getParentId(), m.getName(), m.getPath(), m.getIcon(),
				m.getMenuType(), m.getClientType(), m.getSequ(), m.getStatus(), m.getControlType());
	}
}
