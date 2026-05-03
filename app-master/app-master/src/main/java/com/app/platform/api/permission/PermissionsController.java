package com.app.platform.api.permission;

import com.app.platform.api.dto.ApiSuccessBody;
import com.app.platform.api.dto.permission.MenuOpCodesDto;
import com.app.platform.api.dto.permission.MenuVisibleNodeDto;
import com.app.platform.core.authentication.Constants;
import com.app.platform.core.authentication.UserManager;
import com.app.platform.exception.BadRequestException;
import com.app.platform.exception.ForbiddenException;
import com.app.platform.exception.MenuNotFoundException;
import com.app.platform.permission.VisibleMenuService;
import com.app.platform.sm.menu.repository.AppMenuRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 前台权限查询：可见菜单树与按菜单批量/单个操作码。 */
@RestController
@RequestMapping("/api/permissions")
public class PermissionsController {

	private final AppMenuRepository appMenuRepository;
	private final VisibleMenuService visibleMenuService;

	public PermissionsController(AppMenuRepository appMenuRepository, VisibleMenuService visibleMenuService) {
		this.appMenuRepository = appMenuRepository;
		this.visibleMenuService = visibleMenuService;
	}

	/** 当前登录用户可见菜单（按 clientType 过滤）。 */
	@GetMapping("/menus/visible")
	public ResponseEntity<ApiSuccessBody<List<MenuVisibleNodeDto>>> visibleMenus(
			@RequestParam(defaultValue = "1") String clientType) {
		return ResponseEntity.ok(ApiSuccessBody.of(visibleMenuService.visibleTree(clientType)));
	}

	/**
	 * 某菜单下当前用户操作码；safe=true 时先校验菜单可见性。
	 */
	@GetMapping("/menus/{menuId}/op-codes")
	public ResponseEntity<ApiSuccessBody<MenuOpCodesDto>> opCodesForMenu(
			@PathVariable Long menuId,
			@RequestParam(defaultValue = "false") boolean safe,
			@RequestParam(defaultValue = "1") String clientType) {
		if (!appMenuRepository.existsById(menuId)) {
			throw new MenuNotFoundException();
		}
		if (safe && !visibleMenuService.isMenuVisibleToCurrentUser(menuId, clientType)) {
			throw new ForbiddenException("没有菜单访问权限");
		}
		List<String> codes = UserManager.getOpCodes(menuId);
		return ResponseEntity.ok(ApiSuccessBody.of(new MenuOpCodesDto(String.valueOf(menuId), codes)));
	}

	/** 逗号分隔菜单 id 批量查询；不存在的 id 对应空列表。 */
	@GetMapping(value = "/menus/op-codes", params = "ids")
	public ResponseEntity<ApiSuccessBody<Map<String, List<String>>>> opCodesBatch(@RequestParam String ids) {
		List<Long> idList = parseMenuIds(ids);
		Map<String, List<String>> map = new LinkedHashMap<>();
		for (Long mid : idList) {
			if (!appMenuRepository.existsById(mid)) {
				map.put(String.valueOf(mid), List.of());
			}
			else {
				map.put(String.valueOf(mid), UserManager.getOpCodes(mid));
			}
		}
		return ResponseEntity.ok(ApiSuccessBody.of(map));
	}

	/** 解析并校验 ids 参数：非空、个数上限、合法 Long。 */
	private static List<Long> parseMenuIds(String ids) {
		if (ids == null || ids.isBlank()) {
			throw new BadRequestException("ids 不能为空");
		}
		String[] parts = ids.split(",");
		if (parts.length > Constants.MAX_MENU_IDS_BATCH) {
			throw new BadRequestException("ids 数量不能超过 " + Constants.MAX_MENU_IDS_BATCH);
		}
		List<Long> out = new ArrayList<>();
		for (String p : parts) {
			String t = p.trim();
			if (t.isEmpty()) {
				continue;
			}
			try {
				out.add(Long.parseLong(t));
			}
			catch (NumberFormatException ex) {
				throw new BadRequestException("非法菜单 id: " + t);
			}
		}
		if (out.isEmpty()) {
			throw new BadRequestException("ids 不能为空");
		}
		return out;
	}
}
