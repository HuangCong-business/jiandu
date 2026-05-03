package com.app.platform.sm.role.repository;

import com.app.platform.sm.role.domain.SmRoleUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 用户与角色的多对多关联表（复合主键）仓储。
 */
public interface SmRoleUserRepository extends JpaRepository<SmRoleUser, SmRoleUser.SmRoleUserId> {

	/** 某用户绑定的全部角色关联行。 */
	List<SmRoleUser> findByUserId(Long userId);

	/** 删除某用户的所有角色绑定（例如重置角色前清空）。 */
	void deleteByUserId(Long userId);

	/** 查询拥有指定角色的去重用户 id 列表。 */
	@Query("select distinct ru.userId from SmRoleUser ru where ru.roleId = :roleId")
	List<Long> findUserIdsByRoleId(@Param("roleId") Long roleId);
}
