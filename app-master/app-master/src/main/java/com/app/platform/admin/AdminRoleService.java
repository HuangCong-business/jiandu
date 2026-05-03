package com.app.platform.admin;

import com.app.platform.api.dto.admin.AdminCreateRoleRequest;
import com.app.platform.api.dto.admin.AdminPatchRoleRequest;
import com.app.platform.api.dto.admin.AdminReplaceRoleOpsRequest;
import com.app.platform.api.dto.admin.AdminRoleDetailDto;
import com.app.platform.api.dto.admin.AdminRoleOptionDto;
import com.app.platform.api.dto.admin.AdminRoleRowDto;
import com.app.platform.core.authentication.UserManager;
import com.app.platform.exception.BadRequestException;
import com.app.platform.exception.RoleNotFoundException;
import com.app.platform.permission.OpAssignCache;
import com.app.platform.sm.menu.domain.AppOpAssign;
import com.app.platform.sm.menu.domain.AppOpSecurity;
import com.app.platform.sm.menu.repository.AppOpAssignRepository;
import com.app.platform.sm.menu.repository.AppOpSecurityRepository;
import com.app.platform.sm.role.domain.AppRole;
import com.app.platform.sm.role.repository.AppRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** 应用角色维护：CRUD、菜单-操作授权替换及缓存失效。 */
@Service
public class AdminRoleService {

	private static final Logger log = LoggerFactory.getLogger(AdminRoleService.class);

	private static final String ROLE_STATUS_DELETED = "0";
	private static final String ROLE_STATUS_NORMAL = "1";

	private final AppRoleRepository appRoleRepository;
	private final AppOpAssignRepository appOpAssignRepository;
	private final AppOpSecurityRepository appOpSecurityRepository;
	private final OpAssignCache opAssignCache;

	public AdminRoleService(AppRoleRepository appRoleRepository, AppOpAssignRepository appOpAssignRepository,
			AppOpSecurityRepository appOpSecurityRepository, OpAssignCache opAssignCache) {
		this.appRoleRepository = appRoleRepository;
		this.appOpAssignRepository = appOpAssignRepository;
		this.appOpSecurityRepository = appOpSecurityRepository;
		this.opAssignCache = opAssignCache;
	}

	/** 分页列出全部角色（含已删除行的 status 展示）。 */
	@Transactional(readOnly = true)
	public Page<AdminRoleRowDto> list(int page, int size, String sort) {
		Pageable pageable = PageRequest.of(page, size, parseSort(sort));
		return appRoleRepository.findAll(pageable).map(r -> new AdminRoleRowDto(
				r.getId(), r.getCode(), r.getName(), r.getStatus(), r.getIsViewAll(), r.getSequ()));
	}

	/** 下拉/多选用：仅正常状态角色，按 sequ、code 排序 */
	@Transactional(readOnly = true)
	public List<AdminRoleOptionDto> listSelectOptions() {
		return appRoleRepository.findAllByStatusOrderBySequAscCodeAsc(ROLE_STATUS_NORMAL).stream()
				.map(r -> new AdminRoleOptionDto(r.getId(), r.getCode(), r.getName()))
				.toList();
	}

	/** 详情含某客户端类型下已勾选的 AppOpSecurity id 列表。 */
	@Transactional(readOnly = true)
	public AdminRoleDetailDto getDetail(long id, String clientType) {
		AppRole r = appRoleRepository.findById(id).orElseThrow(RoleNotFoundException::new);
		if (ROLE_STATUS_DELETED.equals(r.getStatus())) {
			throw new RoleNotFoundException();
		}
		if (clientType == null || clientType.isBlank()) {
			clientType = "1";
		}
		List<Long> opIds = appOpAssignRepository.findOpIdsByRoleIdAndMenuClientType(id, clientType);
		return new AdminRoleDetailDto(r.getId(), r.getCode(), r.getName(), r.getRoleDesc(), r.getStatus(),
				r.getIsInner(), r.getIsViewAll(), r.getSequ(), new ArrayList<>(opIds));
	}

	/** 新建角色：code 小写唯一，默认内置标志位。 */
	@Transactional
	public AdminRoleRowDto create(AdminCreateRoleRequest req) {
		String code = req.code().trim().toLowerCase();
		if (appRoleRepository.findByCode(code).isPresent()) {
			throw new BadRequestException("角色编码已存在");
		}
		AppRole r = new AppRole();
		r.setCode(code);
		r.setName(req.name().trim());
		if (req.roleDesc() != null && !req.roleDesc().isBlank()) {
			r.setRoleDesc(req.roleDesc().trim());
		}
		r.setStatus(ROLE_STATUS_NORMAL);
		r.setSequ(0);
		r.setIsInner("0");
		r.setIsViewAll("0");
		appRoleRepository.save(r);
		log.info("admin create role operatorId={} roleId={}", UserManager.getLoginUserId(), r.getId());
		return new AdminRoleRowDto(r.getId(), r.getCode(), r.getName(), r.getStatus(), r.getIsViewAll(), r.getSequ());
	}

	/** 可改名称、描述、状态、是否全量视图、排序。 */
	@Transactional
	public AdminRoleRowDto patch(long id, AdminPatchRoleRequest req) {
		AppRole r = appRoleRepository.findById(id).orElseThrow(RoleNotFoundException::new);
		if (ROLE_STATUS_DELETED.equals(r.getStatus())) {
			throw new RoleNotFoundException();
		}
		if (req.name() != null && !req.name().isBlank()) {
			r.setName(req.name().trim());
		}
		if (req.roleDesc() != null) {
			r.setRoleDesc(req.roleDesc().isBlank() ? null : req.roleDesc().trim());
		}
		if (req.status() != null && !req.status().isBlank()) {
			r.setStatus(req.status().trim());
		}
		if (req.isViewAll() != null && !req.isViewAll().isBlank()) {
			r.setIsViewAll(req.isViewAll().trim());
		}
		if (req.sequ() != null) {
			r.setSequ(req.sequ());
		}
		appRoleRepository.save(r);
		log.info("admin patch role operatorId={} roleId={}", UserManager.getLoginUserId(), id);
		return new AdminRoleRowDto(r.getId(), r.getCode(), r.getName(), r.getStatus(), r.getIsViewAll(), r.getSequ());
	}

	/** 逻辑删除角色并清空权限缓存（全量 evict 简化一致性）。 */
	@Transactional
	public void deleteLogical(long id) {
		AppRole r = appRoleRepository.findById(id).orElseThrow(RoleNotFoundException::new);
		r.setStatus(ROLE_STATUS_DELETED);
		appRoleRepository.save(r);
		opAssignCache.evictAll();
		log.info("admin delete role operatorId={} roleId={}", UserManager.getLoginUserId(), id);
	}

	/**
	 * 按客户端维度重写 app_op_assign：先校验每个 op 属于该 clientType 且启用，再批量插入。
	 */
	@Transactional
	public List<Long> replaceMenuPermissions(long roleId, String clientType, AdminReplaceRoleOpsRequest req) {
		AppRole role = appRoleRepository.findById(roleId).orElseThrow(RoleNotFoundException::new);
		if (ROLE_STATUS_DELETED.equals(role.getStatus())) {
			throw new RoleNotFoundException();
		}
		if (clientType == null || clientType.isBlank()) {
			clientType = "1";
		}
		List<Long> opIds = req.opIds() == null ? List.of() : req.opIds().stream().distinct().toList();
		// 前置校验，避免部分写入后失败
		for (Long opId : opIds) {
			AppOpSecurity op = appOpSecurityRepository.findById(opId).orElseThrow(
					() -> new BadRequestException("无效的操作权限 id: " + opId));
			if (!"1".equals(op.getStatus())) {
				throw new BadRequestException("操作已停用: " + opId);
			}
			if (!clientType.equals(op.getMenu().getClientType())) {
				throw new BadRequestException("操作所属菜单客户端类型与参数 clientType 不一致: " + opId);
			}
		}

		appOpAssignRepository.deleteByRoleIdAndMenuClientType(roleId, clientType);
		appOpAssignRepository.flush();

		// 重建关联行
		for (Long opId : opIds) {
			AppOpSecurity op = appOpSecurityRepository.getReferenceById(opId);
			AppOpAssign assign = new AppOpAssign();
			assign.setRole(role);
			assign.setOp(op);
			appOpAssignRepository.save(assign);
		}

		opAssignCache.evictAll();
		log.info("admin replace role menu permissions operatorId={} roleId={} opCount={}",
				UserManager.getLoginUserId(), roleId, opIds.size());
		return opIds;
	}

	/** 角色列表排序解析，非法字段回退 sequ+code。 */
	private static Sort parseSort(String sortParam) {
		if (sortParam == null || sortParam.isBlank()) {
			return Sort.by(Sort.Direction.ASC, "sequ", "code");
		}
		String[] parts = sortParam.split(",");
		String prop = parts[0].trim();
		if (!List.of("code", "name", "id", "sequ", "status").contains(prop)) {
			prop = "sequ";
		}
		Sort.Direction dir = Sort.Direction.ASC;
		if (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())) {
			dir = Sort.Direction.DESC;
		}
		return Sort.by(dir, prop);
	}
}
