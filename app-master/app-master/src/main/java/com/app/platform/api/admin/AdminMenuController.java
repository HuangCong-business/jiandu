package com.app.platform.api.admin;

import com.app.platform.admin.AdminMenuService;
import com.app.platform.api.dto.ApiSuccessBody;
import com.app.platform.api.dto.admin.AdminCreateMenuRequest;
import com.app.platform.api.dto.admin.AdminMenuAssignNodeDto;
import com.app.platform.api.dto.admin.AdminMenuOpDefDto;
import com.app.platform.api.dto.admin.AdminMenuRowDto;
import com.app.platform.api.dto.admin.AdminMenuTreeRowDto;
import com.app.platform.api.dto.admin.AdminPatchMenuRequest;
import com.app.platform.api.dto.admin.AdminReplaceMenuOpDefsRequest;
import jakarta.validation.Valid;
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

/** 管理端菜单 REST：树形行、分配树、CRUD、操作定义查询与全量替换。 */
@RestController
@RequestMapping("/api/admin/menus")
public class AdminMenuController {

	private final AdminMenuService adminMenuService;

	/** 注入菜单领域服务。 */
	public AdminMenuController(AdminMenuService adminMenuService) {
		this.adminMenuService = adminMenuService;
	}

	/** 按客户端类型返回扁平菜单树行（含 parentId），用于管理端树表格展示。 */
	@GetMapping("/tree-rows")
	public ResponseEntity<ApiSuccessBody<List<AdminMenuTreeRowDto>>> treeRows(
			@RequestParam(defaultValue = "1") String clientType) {
		return ResponseEntity.ok(ApiSuccessBody.of(adminMenuService.listTreeRows(clientType)));
	}

	/** 构建带操作子节点的分配树，与角色权限勾选场景共用同一套结构。 */
	@GetMapping("/assign-tree")
	public ResponseEntity<ApiSuccessBody<List<AdminMenuAssignNodeDto>>> assignTree(
			@RequestParam(defaultValue = "1") String clientType) {
		return ResponseEntity.ok(ApiSuccessBody.of(adminMenuService.buildAssignTree(clientType)));
	}

	/** 创建菜单节点，默认 201。 */
	@PostMapping
	public ResponseEntity<ApiSuccessBody<AdminMenuRowDto>> create(@Valid @RequestBody AdminCreateMenuRequest req) {
		return ResponseEntity.status(201).body(ApiSuccessBody.of(adminMenuService.create(req)));
	}

	/** 查询单个菜单的完整行信息。 */
	@GetMapping("/{id}")
	public ResponseEntity<ApiSuccessBody<AdminMenuRowDto>> getOne(@PathVariable long id) {
		return ResponseEntity.ok(ApiSuccessBody.of(adminMenuService.getRow(id)));
	}

	/** 列出某菜单下「默认」操作定义（非分组行），含停用项便于界面展示。 */
	@GetMapping("/{id}/op-definitions")
	public ResponseEntity<ApiSuccessBody<List<AdminMenuOpDefDto>>> opDefinitions(@PathVariable long id) {
		return ResponseEntity.ok(ApiSuccessBody.of(adminMenuService.listOpDefinitions(id)));
	}

	/**
	 * 全量替换指定菜单下的操作定义集合。
	 * 未出现在请求中的既有定义会被逻辑停用（由服务层约定）。
	 */
	@PutMapping("/{id}/op-definitions")
	public ResponseEntity<ApiSuccessBody<List<AdminMenuOpDefDto>>> replaceOpDefinitions(
			@PathVariable long id,
			@Valid @RequestBody AdminReplaceMenuOpDefsRequest req) {
		return ResponseEntity.ok(ApiSuccessBody.of(adminMenuService.replaceOpDefinitions(id, req)));
	}

	/** 局部更新菜单字段（名称、路径、排序、父节点等）。 */
	@PatchMapping("/{id}")
	public ResponseEntity<ApiSuccessBody<AdminMenuRowDto>> patch(
			@PathVariable long id,
			@Valid @RequestBody AdminPatchMenuRequest req) {
		return ResponseEntity.ok(ApiSuccessBody.of(adminMenuService.patch(id, req)));
	}

	/** 逻辑删除菜单；若存在子节点等业务约束则由服务层返回错误。 */
	@DeleteMapping("/{id}")
	public ResponseEntity<ApiSuccessBody<Map<String, Boolean>>> delete(@PathVariable long id) {
		adminMenuService.deleteLogical(id);
		return ResponseEntity.ok(ApiSuccessBody.of(Map.of("success", true)));
	}
}
