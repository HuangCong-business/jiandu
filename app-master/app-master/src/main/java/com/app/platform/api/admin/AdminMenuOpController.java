package com.app.platform.api.admin;

import com.app.platform.admin.AdminMenuOpService;
import com.app.platform.api.dto.ApiSuccessBody;
import com.app.platform.api.dto.admin.AdminMenuOpOptionDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端「菜单操作」辅助接口：按客户端类型列出可选操作，供角色授权等场景下拉使用。
 */
@RestController
@RequestMapping("/api/admin/menu-ops")
public class AdminMenuOpController {

	private final AdminMenuOpService adminMenuOpService;

	/** 注入菜单操作查询服务。 */
	public AdminMenuOpController(AdminMenuOpService adminMenuOpService) {
		this.adminMenuOpService = adminMenuOpService;
	}

	/** 返回当前端类型下所有可选择的操作项（含所属菜单信息）。 */
	@GetMapping("/select-options")
	public ResponseEntity<ApiSuccessBody<List<AdminMenuOpOptionDto>>> selectOptions(
			@RequestParam(defaultValue = "1") String clientType) {
		return ResponseEntity.ok(ApiSuccessBody.of(adminMenuOpService.listSelectOptions(clientType)));
	}
}
