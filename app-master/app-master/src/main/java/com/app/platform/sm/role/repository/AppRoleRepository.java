package com.app.platform.sm.role.repository;

import com.app.platform.sm.role.domain.AppRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** 角色主表仓储：按状态排序列表、按编码查找、按用户查有效角色。 */
public interface AppRoleRepository extends JpaRepository<AppRole, Long> {

	/** 按状态查询全部角色，排序规则：序号升序、编码升序。 */
	List<AppRole> findAllByStatusOrderBySequAscCodeAsc(String status);

	/** 按业务编码唯一查找角色（可选）。 */
	Optional<AppRole> findByCode(String code);

	/**
	 * 用户已分配且状态为启用（'1'）的角色列表。
	 * 通过子查询关联 {@code SmRoleUser}。
	 */
	@Query("select r from AppRole r where r.status = '1' and r.id in "
			+ "(select ru.roleId from SmRoleUser ru where ru.userId = :userId)")
	List<AppRole> findActiveRolesForUser(@Param("userId") Long userId);
}
