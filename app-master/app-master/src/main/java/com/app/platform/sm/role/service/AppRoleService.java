package com.app.platform.sm.role.service;

import com.app.platform.core.authentication.RoleSnapshot;
import com.app.platform.sm.role.domain.AppRole;
import com.app.platform.sm.role.repository.AppRoleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/** 应用内角色查询服务：将持久化角色转为登录态可用的快照列表。 */
@Service
public class AppRoleService {

	private final AppRoleRepository appRoleRepository;

	public AppRoleService(AppRoleRepository appRoleRepository) {
		this.appRoleRepository = appRoleRepository;
	}

	/**
	 * 查询指定用户当前有效的角色，并映射为 {@link RoleSnapshot} 列表。
	 * 「有效」由仓储层 JPQL 与角色状态、日期等条件共同约束。
	 */
	public List<RoleSnapshot> getUserRoles(Long userId) {
		return appRoleRepository.findActiveRolesForUser(userId).stream()
				.map(this::toSnapshot)
				.toList();
	}

	/** 将实体转为轻量快照，避免向认证层暴露完整实体。 */
	private RoleSnapshot toSnapshot(AppRole r) {
		return new RoleSnapshot(r.getId(), r.getCode(), r.getName());
	}
}
