package com.app.platform.admin;

import com.app.platform.api.dto.admin.AdminCreateUserRequest;
import com.app.platform.api.dto.admin.AdminPatchUserRequest;
import com.app.platform.api.dto.admin.AdminReplaceRolesRequest;
import com.app.platform.api.dto.admin.AdminResetPasswordRequest;
import com.app.platform.api.dto.admin.AdminUserPageDto;
import com.app.platform.api.dto.admin.AdminUserRowDto;
import com.app.platform.core.authentication.Constants;
import com.app.platform.core.authentication.UserManager;
import com.app.platform.exception.BadRequestException;
import com.app.platform.exception.InvalidRoleException;
import com.app.platform.exception.UserNotFoundException;
import com.app.platform.exception.UsernameTakenException;
import com.app.platform.org.employee.domain.SysEmployee;
import com.app.platform.org.employee.repository.SysEmployeeRepository;
import com.app.platform.permission.OpAssignCache;
import com.app.platform.sm.role.domain.AppRole;
import com.app.platform.sm.role.domain.SmRoleUser;
import com.app.platform.sm.role.repository.AppRoleRepository;
import com.app.platform.sm.role.repository.SmRoleUserRepository;
import com.app.platform.sm.user.domain.SmUser;
import com.app.platform.sm.user.repository.SmUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端用户生命周期：分页查询、创建/修改、重置密码、替换角色、逻辑删除；同步 sys_employee 与权限缓存。
 */
@Service
public class AdminUserService {

	private static final Logger log = LoggerFactory.getLogger(AdminUserService.class);

	/** 允许前端传入的排序字段白名单，防止属性注入。 */
	private static final Set<String> ALLOWED_SORT = Set.of("code", "name", "id", "createTime", "modifyTime", "status");

	private final SmUserRepository smUserRepository;
	private final SysEmployeeRepository sysEmployeeRepository;
	private final SmRoleUserRepository smRoleUserRepository;
	private final AppRoleRepository appRoleRepository;
	private final PasswordEncoder passwordEncoder;
	private final OpAssignCache opAssignCache;

	public AdminUserService(SmUserRepository smUserRepository, SysEmployeeRepository sysEmployeeRepository,
			SmRoleUserRepository smRoleUserRepository, AppRoleRepository appRoleRepository,
			PasswordEncoder passwordEncoder, OpAssignCache opAssignCache) {
		this.smUserRepository = smUserRepository;
		this.sysEmployeeRepository = sysEmployeeRepository;
		this.smRoleUserRepository = smRoleUserRepository;
		this.appRoleRepository = appRoleRepository;
		this.passwordEncoder = passwordEncoder;
		this.opAssignCache = opAssignCache;
	}

	/** 动态 Specification 分页列表，聚合角色与员工信息到行 DTO。 */
	@Transactional(readOnly = true)
	public AdminUserPageDto list(int page, int size, String sortParam, String keyword, String status, Long roleId,
			Long deptId) {
		Sort sort = parseSort(sortParam);
		Pageable pageable = PageRequest.of(page, size, sort);
		Specification<SmUser> spec = Specification.allOf(
				SmUserSpecifications.statusFilter(status),
				SmUserSpecifications.keywordLike(keyword),
				SmUserSpecifications.hasRole(roleId),
				SmUserSpecifications.deptIdEq(deptId));
		Page<SmUser> pg = smUserRepository.findAll(spec, pageable);
		List<AdminUserRowDto> rows = pg.getContent().stream().map(this::toRow).toList();
		return new AdminUserPageDto(rows, pg.getTotalElements(), pg.getTotalPages(), pg.getNumber(), pg.getSize());
	}

	/** 单条详情；已逻辑删除用户视为不存在。 */
	@Transactional(readOnly = true)
	public AdminUserRowDto getById(long id) {
		SmUser sm = smUserRepository.findById(id).orElseThrow(UserNotFoundException::new);
		if (Constants.USER_STATUS_DELETED.equals(sm.getStatus())) {
			throw new UserNotFoundException();
		}
		return toRow(sm);
	}

	/** 新建账号 + 员工行 + 角色关联；编码唯一校验。 */
	@Transactional
	public AdminUserRowDto create(AdminCreateUserRequest req) {
		if (smUserRepository.existsByCodeIgnoreCase(req.code())) {
			throw new UsernameTakenException();
		}
		validateRoleIds(req.roleIds());

		SmUser sm = new SmUser();
		sm.setCode(req.code());
		sm.setName(req.displayName() != null ? req.displayName() : req.code());
		sm.setPasswordBcrypt(passwordEncoder.encode(req.password()));
		short ut;
		try {
			ut = Short.parseShort(req.userType());
		}
		catch (NumberFormatException ex) {
			throw new BadRequestException("userType 无效");
		}
		sm.setUserType(ut);
		sm.setStatus(req.status());
		sm.setFailedLoginCount(0);
		smUserRepository.save(sm);

		SysEmployee emp = new SysEmployee();
		emp.setSmUser(sm);
		emp.setStatus(req.status());
		emp.setCode(req.code());
		emp.setName(req.displayName() != null ? req.displayName() : req.code());
		emp.setUserId(sm.getId());
		if (req.deptId() != null) {
			emp.setDeptId(req.deptId());
		}
		sysEmployeeRepository.save(emp);

		saveRoleAssignments(sm.getId(), req.roleIds());

		log.info("admin create user operatorId={} targetCode={}", UserManager.getLoginUserId(), sm.getCode());
		return toRow(smUserRepository.findById(sm.getId()).orElseThrow());
	}

	/** 部分更新用户与关联员工档案（若存在）。 */
	@Transactional
	public AdminUserRowDto patch(long id, AdminPatchUserRequest req) {
		SmUser sm = smUserRepository.findById(id).orElseThrow(UserNotFoundException::new);
		if (Constants.USER_STATUS_DELETED.equals(sm.getStatus())) {
			throw new UserNotFoundException();
		}
		if (req.getName() != null) {
			sm.setName(req.getName().trim());
		}
		if (req.getStatus() != null) {
			sm.setStatus(req.getStatus().trim());
		}
		if (req.getUserType() != null) {
			sm.setUserType(req.getUserType());
		}
		smUserRepository.save(sm);

		SysEmployee emp = sysEmployeeRepository.findById(id).orElse(null);
		if (emp != null) {
			if (req.getName() != null) {
				emp.setName(req.getName().trim());
			}
			if (req.getStatus() != null) {
				emp.setStatus(req.getStatus().trim());
			}
			if (req.getDeptId() != null) {
				emp.setDeptId(req.getDeptId());
			}
			if (req.getMobile() != null) {
				emp.setMobile(req.getMobile().trim());
			}
			sysEmployeeRepository.save(emp);
		}

		log.info("admin patch user operatorId={} targetUserId={}", UserManager.getLoginUserId(), id);
		return toRow(smUserRepository.findById(id).orElseThrow());
	}

	/** 重置 bcrypt 密码；可选强制下次登录改密（user_ex1 标记）。 */
	@Transactional
	public void resetPassword(long id, AdminResetPasswordRequest req) {
		SmUser sm = smUserRepository.findById(id).orElseThrow(UserNotFoundException::new);
		if (Constants.USER_STATUS_DELETED.equals(sm.getStatus())) {
			throw new UserNotFoundException();
		}
		sm.setPasswordBcrypt(passwordEncoder.encode(req.newPassword()));
		if (Boolean.TRUE.equals(req.forceChangeOnNextLogin())) {
			sm.setUserEx1("1");
		}
		else {
			sm.setUserEx1(null);
		}
		smUserRepository.save(sm);
		log.info("admin reset password operatorId={} targetUserId={}", UserManager.getLoginUserId(), id);
	}

	/** 全量替换用户-角色关联并失效其操作码缓存。 */
	@Transactional
	public List<Long> replaceRoles(long id, AdminReplaceRolesRequest req) {
		SmUser sm = smUserRepository.findById(id).orElseThrow(UserNotFoundException::new);
		if (Constants.USER_STATUS_DELETED.equals(sm.getStatus())) {
			throw new UserNotFoundException();
		}
		validateRoleIds(req.roleIds());
		smRoleUserRepository.deleteByUserId(id);
		saveRoleAssignments(id, req.roleIds());
		opAssignCache.evictUser(id);
		log.info("admin replace roles operatorId={} targetUserId={}", UserManager.getLoginUserId(), id);
		return req.roleIds();
	}

	/** 逻辑删除用户与员工，并清理角色与缓存。 */
	@Transactional
	public void deleteLogical(long id) {
		SmUser sm = smUserRepository.findById(id).orElseThrow(UserNotFoundException::new);
		sm.setStatus(Constants.USER_STATUS_DELETED);
		smUserRepository.save(sm);
		sysEmployeeRepository.findById(id).ifPresent(emp -> {
			emp.setStatus(Constants.USER_STATUS_DELETED);
			sysEmployeeRepository.save(emp);
		});
		smRoleUserRepository.deleteByUserId(id);
		opAssignCache.evictUser(id);
		log.info("admin delete user operatorId={} targetUserId={}", UserManager.getLoginUserId(), id);
	}

	/** 校验角色 id 均存在且状态为启用。 */
	private void validateRoleIds(List<Long> roleIds) {
		if (roleIds == null || roleIds.isEmpty()) {
			return;
		}
		List<AppRole> roles = appRoleRepository.findAllById(roleIds);
		if (roles.size() != roleIds.size()) {
			throw new InvalidRoleException();
		}
		for (AppRole r : roles) {
			if (!"1".equals(r.getStatus())) {
				throw new InvalidRoleException();
			}
		}
	}

	/** 去重后写入 sm_role_user。 */
	private void saveRoleAssignments(long userId, List<Long> roleIds) {
		if (roleIds == null || roleIds.isEmpty()) {
			return;
		}
		for (Long rid : roleIds.stream().distinct().toList()) {
			SmRoleUser ru = new SmRoleUser();
			ru.setUserId(userId);
			ru.setRoleId(rid);
			smRoleUserRepository.save(ru);
		}
	}

	/** 解析 {@code field,(asc|desc)}，非法字段回退 code 升序。 */
	private Sort parseSort(String sortParam) {
		if (sortParam == null || sortParam.isBlank()) {
			return Sort.by(Sort.Direction.ASC, "code");
		}
		String[] parts = sortParam.split(",");
		String prop = parts[0].trim();
		if (!ALLOWED_SORT.contains(prop)) {
			prop = "code";
		}
		Sort.Direction dir = Sort.Direction.ASC;
		if (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())) {
			dir = Sort.Direction.DESC;
		}
		return Sort.by(dir, prop);
	}

	/** 组装列表行：员工部门、角色 id/名称、登录风控字段。 */
	private AdminUserRowDto toRow(SmUser sm) {
		SysEmployee emp = sysEmployeeRepository.findById(sm.getId()).orElse(null);
		List<SmRoleUser> links = smRoleUserRepository.findByUserId(sm.getId());
		List<Long> roleIds = links.stream().map(SmRoleUser::getRoleId).toList();
		List<AppRole> roles = roleIds.isEmpty() ? List.of() : appRoleRepository.findAllById(roleIds);
		Map<Long, String> roleNameById = roles.stream()
				.collect(Collectors.toMap(AppRole::getId, AppRole::getName, (a, b) -> a, LinkedHashMap::new));
		List<String> roleNames = roleIds.stream().map(roleNameById::get).filter(n -> n != null).toList();

		return new AdminUserRowDto(
				sm.getId(),
				sm.getCode(),
				sm.getName(),
				sm.getStatus(),
				String.valueOf(sm.getUserType()),
				emp != null ? emp.getDeptId() : null,
				null,
				new ArrayList<>(roleIds),
				roleNames,
				sm.getLastLoginTime(),
				sm.getLockedUntil());
	}
}
