package com.app.platform.api.admin;

import com.app.platform.admin.AdminMenuService;
import com.app.platform.admin.AdminRoleService;
import com.app.platform.api.dto.ApiSuccessBody;
import com.app.platform.api.dto.admin.AdminCreateRoleRequest;
import com.app.platform.api.dto.admin.AdminPatchRoleRequest;
import com.app.platform.api.dto.admin.AdminReplaceRoleOpsRequest;
import com.app.platform.api.dto.admin.AdminRoleDetailDto;
import com.app.platform.api.dto.admin.AdminRoleOptionDto;
import com.app.platform.api.dto.admin.AdminMenuAssignNodeDto;
import com.app.platform.api.dto.admin.AdminRoleRowDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 管理端角色 REST：列表、详情、增删改、菜单权限替换及与菜单分配树相关查询。 */
@RestController
@RequestMapping("/api/admin/roles")
public class AdminRoleController {

	private final AdminRoleService adminRoleService;
	private final AdminMenuService adminMenuService;

	/** 注入角色服务与菜单服务（分配树由菜单服务构建）。 */
	public AdminRoleController(AdminRoleService adminRoleService, AdminMenuService adminMenuService) {
		this.adminRoleService = adminRoleService;
		this.adminMenuService = adminMenuService;
	}

	/**
	 * 返回指定客户端类型下的「菜单 + 操作」分配树，供角色勾选权限时使用。
	 */
	@GetMapping("/menu-assign-tree")
	public ResponseEntity<ApiSuccessBody<List<AdminMenuAssignNodeDto>>> menuAssignTree(
			@RequestParam(defaultValue = "1") String clientType) {
		return ResponseEntity.ok(ApiSuccessBody.of(adminMenuService.buildAssignTree(clientType)));
	}

	/**
	 * 分页查询角色列表，并将 Spring Data 分页元数据一并放入统一成功体。
	 */
	@GetMapping
	public ResponseEntity<ApiSuccessBody<Map<String, Object>>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String sort) {
		Page<AdminRoleRowDto> pg = adminRoleService.list(page, size, sort);
		Map<String, Object> body = Map.of(
				"content", pg.getContent(),
				"totalElements", pg.getTotalElements(),
				"totalPages", pg.getTotalPages(),
				"number", pg.getNumber(),
				"size", pg.getSize());
		return ResponseEntity.ok(ApiSuccessBody.of(body));
	}

	/** 下拉/选择器用的角色简要列表（通常仅含 id、code、name）。 */
	@GetMapping("/select-options")
	public ResponseEntity<ApiSuccessBody<List<AdminRoleOptionDto>>> selectOptions() {
		return ResponseEntity.ok(ApiSuccessBody.of(adminRoleService.listSelectOptions()));
	}

	/**
	 * 角色详情，含已分配的操作 id 等；clientType 用于区分多端菜单权限。
	 */
	@GetMapping("/{id}")
	public ResponseEntity<ApiSuccessBody<AdminRoleDetailDto>> detail(
			@PathVariable long id,
			@RequestParam(defaultValue = "1") String clientType) {
		return ResponseEntity.ok(ApiSuccessBody.of(adminRoleService.getDetail(id, clientType)));
	}

	/** 新建角色，校验通过后返回 201 与行 DTO。 */
	@PostMapping
	public ResponseEntity<ApiSuccessBody<AdminRoleRowDto>> create(@Valid @RequestBody AdminCreateRoleRequest req) {
		return ResponseEntity.status(201).body(ApiSuccessBody.of(adminRoleService.create(req)));
	}

	/** 局部更新角色属性。 */
	@PatchMapping("/{id}")
	public ResponseEntity<ApiSuccessBody<AdminRoleRowDto>> patch(
			@PathVariable long id,
			@Valid @RequestBody AdminPatchRoleRequest req) {
		return ResponseEntity.ok(ApiSuccessBody.of(adminRoleService.patch(id, req)));
	}

	/** 逻辑删除角色，响应中 success 恒为 true（业务层已处理异常）。 */
	@DeleteMapping("/{id}")
	public ResponseEntity<ApiSuccessBody<Map<String, Boolean>>> delete(@PathVariable long id) {
		adminRoleService.deleteLogical(id);
		return ResponseEntity.ok(ApiSuccessBody.of(Map.of("success", true)));
	}

	/**
	 * 按客户端类型全量替换该角色在菜单操作上的授权（opIds）。
	 * 返回当前保存后的操作 id 列表。
	 */
	@PutMapping("/{id}/menu-permissions")
	public ResponseEntity<ApiSuccessBody<Map<String, List<Long>>>> replaceMenuPermissions(
			@PathVariable long id,
			@RequestParam(defaultValue = "1") String clientType,
			@RequestBody AdminReplaceRoleOpsRequest req) {
		List<Long> opIds = adminRoleService.replaceMenuPermissions(id, clientType, req);
		return ResponseEntity.ok(ApiSuccessBody.of(Map.of("opIds", opIds)));
	}
}
