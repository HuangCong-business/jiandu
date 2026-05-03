package com.app.platform.sm.menu.repository;

import com.app.platform.sm.menu.domain.AppOpSecurity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** 菜单下「安全操作」定义仓储：可选列表、权限判定、按菜单维护。 */
public interface AppOpSecurityRepository extends JpaRepository<AppOpSecurity, Long> {

	/**
	 * 某客户端类型下可勾选的操作（菜单与操作均启用，且非分组行 {@code groupId IS NULL}）。
	 * 排序：菜单序号、菜单 id、操作序号、操作编码。
	 */
	@Query("""
			select o from AppOpSecurity o join fetch o.menu m
			where o.status = '1' and m.status = '1' and m.clientType = :clientType
				and o.groupId is null
			order by m.sequ asc, m.id asc, o.sequ asc, o.code asc
			""")
	List<AppOpSecurity> findSelectableOpsByClientType(@Param("clientType") String clientType);

	/** 某菜单下所有启用中的操作编码（不含分组行），用于列举定义而非用户授权。 */
	@Query(value = """
			SELECT DISTINCT aos.code FROM app_op_security aos
			WHERE aos.menu_id = :menuId AND aos.group_id IS NULL AND aos.status = '1'
			""", nativeQuery = true)
	List<String> findAllActiveOpCodesForMenu(@Param("menuId") long menuId);

	/**
	 * 某用户对某菜单实际被授予的操作编码：经角色、角色有效期、分配表关联过滤。
	 */
	@Query(value = """
			SELECT DISTINCT aos.code FROM app_op_security aos
			INNER JOIN app_op_assign aoa ON aoa.op_id = aos.id
			INNER JOIN sm_role_user sru ON sru.role_id = aoa.role_id
			INNER JOIN app_role ar ON ar.id = sru.role_id AND ar.status = '1'
				AND (ar.validated_start_date IS NULL OR ar.validated_start_date <= CURRENT_DATE)
				AND (ar.validated_end_date IS NULL OR ar.validated_end_date >= CURRENT_DATE)
			WHERE aos.menu_id = :menuId AND aos.group_id IS NULL AND aos.status = '1'
				AND sru.user_id = :userId
			""", nativeQuery = true)
	List<String> findAssignedOpCodesForUserAndMenu(@Param("userId") long userId, @Param("menuId") long menuId);

	/** 统计某菜单下指定状态的操作数量（用于删除前校验等）。 */
	long countByMenu_IdAndStatus(long menuId, String status);

	/** 某菜单下非分组操作，按序号与 id 排序。 */
	List<AppOpSecurity> findByMenu_IdAndGroupIdIsNullOrderBySequAscIdAsc(long menuId);

	/** 同菜单下同编码是否已被其他 id 占用（更新时防重）。 */
	boolean existsByMenu_IdAndGroupIdIsNullAndCodeAndIdNot(long menuId, String code, long id);

	/** 按菜单与编码定位单条默认操作（分组 id 为空）。 */
	Optional<AppOpSecurity> findByMenu_IdAndGroupIdIsNullAndCode(long menuId, String code);
}
