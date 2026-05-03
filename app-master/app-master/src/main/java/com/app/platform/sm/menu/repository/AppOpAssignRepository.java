package com.app.platform.sm.menu.repository;

import com.app.platform.sm.menu.domain.AppOpAssign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** 角色与菜单操作授权关系仓储。 */
public interface AppOpAssignRepository extends JpaRepository<AppOpAssign, Long> {

	/** 某角色下的全部操作分配行。 */
	List<AppOpAssign> findByRole_Id(Long roleId);

	/** 删除指定角色的所有操作授权。 */
	@Modifying(clearAutomatically = true)
	@Query("delete from AppOpAssign a where a.role.id = :roleId")
	void deleteByRole_Id(@Param("roleId") long roleId);

	/**
	 * 仅删除某角色在指定客户端类型菜单下的操作授权（多端隔离替换用）。
	 */
	@Modifying(clearAutomatically = true)
	@Query("delete from AppOpAssign a where a.role.id = :roleId and a.op.menu.clientType = :clientType")
	void deleteByRoleIdAndMenuClientType(@Param("roleId") long roleId, @Param("clientType") String clientType);

	/** 查询某角色在某客户端类型下已分配的操作主键 id 列表。 */
	@Query("select o.id from AppOpAssign a join a.op o join o.menu m where a.role.id = :roleId and m.clientType = :clientType")
	List<Long> findOpIdsByRoleIdAndMenuClientType(@Param("roleId") long roleId, @Param("clientType") String clientType);
}
