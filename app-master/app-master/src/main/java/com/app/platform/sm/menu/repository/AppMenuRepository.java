package com.app.platform.sm.menu.repository;

import com.app.platform.sm.menu.domain.AppMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** 菜单树仓储：按客户端与状态加载、可见菜单 id 计算、父子关系统计。 */
public interface AppMenuRepository extends JpaRepository<AppMenu, Long> {

	/**
	 * 预抓取父节点后按客户端类型与状态查询，避免 N+1。
	 */
	@Query("select distinct m from AppMenu m left join fetch m.parent "
			+ "where m.clientType = :clientType and m.status = :status order by m.sequ asc, m.id asc")
	List<AppMenu> findWithParentByClientTypeAndStatusOrderBySequAscIdAsc(
			@Param("clientType") String clientType,
			@Param("status") String status);

	/** 不抓取父节点的简化列表查询。 */
	List<AppMenu> findByClientTypeAndStatusOrderBySequAsc(String clientType, String status);

	/**
	 * 用户通过角色在某客户端类型下「可见」的菜单 id：需存在已分配操作且角色在有效期内。
	 */
	@Query(value = """
			SELECT DISTINCT m.id FROM app_menu m
			INNER JOIN app_op_security aos ON aos.menu_id = m.id AND aos.status = '1'
			INNER JOIN app_op_assign aoa ON aoa.op_id = aos.id
			INNER JOIN sm_role_user sru ON sru.role_id = aoa.role_id
			INNER JOIN app_role ar ON ar.id = sru.role_id AND ar.status = '1'
				AND (ar.validated_start_date IS NULL OR ar.validated_start_date <= CURRENT_DATE)
				AND (ar.validated_end_date IS NULL OR ar.validated_end_date >= CURRENT_DATE)
			WHERE m.client_type = :clientType AND m.status = '1' AND sru.user_id = :userId
			""", nativeQuery = true)
	List<Long> findVisibleMenuIdsForAssignedUser(@Param("userId") long userId, @Param("clientType") String clientType);

	/** 某父节点下指定状态的子菜单数量。 */
	long countByParent_IdAndStatus(long parentId, String status);

	/** 列出某父下的子菜单（指定状态）。 */
	List<AppMenu> findByParent_IdAndStatus(Long parentId, String status);
}
