package com.app.platform.api.admin;

import com.app.platform.admin.AdminUserService;
import com.app.platform.api.dto.ApiSuccessBody;
import com.app.platform.api.dto.admin.AdminCreateUserRequest;
import com.app.platform.api.dto.admin.AdminPatchUserRequest;
import com.app.platform.api.dto.admin.AdminReplaceRolesRequest;
import com.app.platform.api.dto.admin.AdminResetPasswordRequest;
import com.app.platform.api.dto.admin.AdminUserPageDto;
import com.app.platform.api.dto.admin.AdminUserRowDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

/** 管理端用户 REST：分页查询、详情、创建、局部更新、重置密码、替换角色、逻辑删除。 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

	private final AdminUserService adminUserService;

	/** 注入用户领域服务，供本控制器调用。 */
	public AdminUserController(AdminUserService adminUserService) {
		this.adminUserService = adminUserService;
	}

	/**
	 * 分页列出用户，支持排序、关键字、状态、角色、部门等筛选。
	 * size 会被限制在 [1,100]，避免单次拉取过大。
	 */
	@GetMapping
	public ResponseEntity<ApiSuccessBody<AdminUserPageDto>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String sort,
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) Long roleId,
			@RequestParam(required = false) Long deptId) {
		int safeSize = Math.min(Math.max(size, 1), 100);
		AdminUserPageDto data = adminUserService.list(page, safeSize, sort, keyword, status, roleId, deptId);
		return ResponseEntity.ok(ApiSuccessBody.of(data));
	}

	/** 按主键查询单条用户行数据。 */
	@GetMapping("/{id}")
	public ResponseEntity<ApiSuccessBody<AdminUserRowDto>> get(@PathVariable long id) {
		return ResponseEntity.ok(ApiSuccessBody.of(adminUserService.getById(id)));
	}

	/** 创建用户，请求体需通过校验；成功返回 201 与新建行。 */
	@PostMapping
	public ResponseEntity<ApiSuccessBody<AdminUserRowDto>> create(@Valid @RequestBody AdminCreateUserRequest request) {
		AdminUserRowDto row = adminUserService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiSuccessBody.of(row));
	}

	/** 局部更新用户信息（非空字段覆盖）。 */
	@PatchMapping("/{id}")
	public ResponseEntity<ApiSuccessBody<AdminUserRowDto>> patch(@PathVariable long id,
			@RequestBody AdminPatchUserRequest request) {
		return ResponseEntity.ok(ApiSuccessBody.of(adminUserService.patch(id, request)));
	}

	/** 管理员重置指定用户密码，无响应体。 */
	@PostMapping("/{id}/reset-password")
	public ResponseEntity<Void> resetPassword(@PathVariable long id,
			@Valid @RequestBody AdminResetPasswordRequest request) {
		adminUserService.resetPassword(id, request);
		return ResponseEntity.noContent().build();
	}

	/**
	 * 全量替换用户的角色绑定列表。
	 * 响应中返回当前生效的角色 id 列表。
	 */
	@PutMapping("/{id}/roles")
	public ResponseEntity<ApiSuccessBody<Map<String, List<Long>>>> replaceRoles(@PathVariable long id,
			@Valid @RequestBody AdminReplaceRolesRequest request) {
		List<Long> roleIds = adminUserService.replaceRoles(id, request);
		return ResponseEntity.ok(ApiSuccessBody.of(Map.of("roleIds", roleIds)));
	}

	/** 逻辑删除用户，HTTP 204。 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable long id) {
		adminUserService.deleteLogical(id);
		return ResponseEntity.noContent().build();
	}
}
