package com.app.platform.admin;

import com.app.platform.api.dto.admin.AdminMenuOpOptionDto;
import com.app.platform.sm.menu.domain.AppOpSecurity;
import com.app.platform.sm.menu.repository.AppOpSecurityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 管理端「操作权限」下拉数据：按客户端聚合可选 AppOpSecurity。 */
@Service
public class AdminMenuOpService {

	private final AppOpSecurityRepository appOpSecurityRepository;

	public AdminMenuOpService(AppOpSecurityRepository appOpSecurityRepository) {
		this.appOpSecurityRepository = appOpSecurityRepository;
	}

	/** 默认 clientType=1；返回 id、code、名称及所属菜单信息。 */
	@Transactional(readOnly = true)
	public List<AdminMenuOpOptionDto> listSelectOptions(String clientType) {
		if (clientType == null || clientType.isBlank()) {
			clientType = "1";
		}
		return appOpSecurityRepository.findSelectableOpsByClientType(clientType).stream()
				.map(AdminMenuOpService::toDto)
				.toList();
	}

	/** 空名称用空串占位，避免前端 undefined。 */
	private static AdminMenuOpOptionDto toDto(AppOpSecurity o) {
		String name = o.getName();
		return new AdminMenuOpOptionDto(o.getId(), o.getCode(), name == null ? "" : name,
				o.getMenu().getId(), o.getMenu().getName());
	}
}
