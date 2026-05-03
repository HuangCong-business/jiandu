package com.app.platform.sm.user.service;

import com.app.platform.core.authentication.Constants;
import com.app.platform.core.authentication.RoleSnapshot;
import com.app.platform.core.authentication.ThreadLocalManager;
import com.app.platform.core.authentication.impl.User;
import com.app.platform.core.authentication.intf.IUser;
import com.app.platform.org.employee.domain.SysEmployee;
import com.app.platform.org.employee.service.SysEmployeeService;
import com.app.platform.sm.role.service.AppRoleService;
import com.app.platform.sm.user.domain.SmUser;
import com.app.platform.sm.user.repository.SmUserRepository;
import com.app.platform.service.UserLoginPersistence;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * IUser 装配实现：从 sm_user 拉主数据，合并员工与角色，供会话与权限使用。
 */
@Service("userServiceImpl")
public class UserServiceImpl implements IUserService {

	private final SmUserRepository smUserRepository;
	private final SysEmployeeService sysEmployeeService;
	private final AppRoleService appRoleService;
	private final UserLoginPersistence userLoginPersistence;

	public UserServiceImpl(SmUserRepository smUserRepository, SysEmployeeService sysEmployeeService,
			AppRoleService appRoleService, UserLoginPersistence userLoginPersistence) {
		this.smUserRepository = smUserRepository;
		this.sysEmployeeService = sysEmployeeService;
		this.appRoleService = appRoleService;
		this.userLoginPersistence = userLoginPersistence;
	}

	@Override
	public IUser getById(Long id) {
		SmUser sm = smUserRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
		User user = mapAccountToUser(sm);
		// 附加组织、角色、超管标记等衍生字段
		buildUserProperty(user);
		return user;
	}

	@Override
	public void setLoginUser(IUser user, String loginType, HttpServletRequest request, boolean recordLog) {
		user.setProperty(IUser.LOGIN_TYPE, loginType);
		if (request != null) {
			user.setProperty(IUser.IP, request.getRemoteAddr());
			HttpSession session = request.getSession(true);
			session.setAttribute(Constants.SESSION_USER, user);
		}
		ThreadLocalManager.setUserLocal(user);
		// 登录成功审计：更新最后登录时间与解锁状态
		if (recordLog && user.getLoginUserId() != null) {
			userLoginPersistence.recordSuccessfulLogin(user.getLoginUserId());
		}
	}

	/** 将账号表字段映射为 IUser 基础键值。 */
	private User mapAccountToUser(SmUser sm) {
		User u = new User();
		u.setProperty(IUser.USERID, sm.getId());
		u.setProperty(IUser.CODE, sm.getCode());
		u.setProperty(IUser.NAME, sm.getName());
		u.setProperty(IUser.USERTYPE, sm.getUserType());
		u.setProperty(IUser.STATUS, sm.getStatus());
		return u;
	}

	/** 合并员工扩展信息与应用角色列表（平台超管不写角色列表）。 */
	private void buildUserProperty(IUser user) {
		Long userId = user.getLoginUserId();
		if (userId == null) {
			return;
		}
		sysEmployeeService.getById(userId).ifPresent(emp -> mergeEmployee(user, emp));

		short userType = readShort(user.getProperty(IUser.USERTYPE));
		if (userType != Constants.USER_TYPE_SYS_ADMIN) {
			List<RoleSnapshot> roles = appRoleService.getUserRoles(userId);
			user.setProperty(IUser.ROLE_LIST, roles);
			// 拥有 super_admin 应用角色则打管理会话标记
			if (roles.stream().anyMatch(r -> Constants.APP_ROLE_CODE_SUPER_ADMIN.equals(r.code()))) {
				user.setProperty(IUser.IS_ADMIN_EMP, Boolean.TRUE);
			}
		}
	}

	/** 将 sys_employee 中非空字段写入 IUser 属性包。 */
	private static void mergeEmployee(IUser user, SysEmployee emp) {
		if (emp.getDeptId() != null) {
			user.setProperty(IUser.DEPT_ID, emp.getDeptId());
		}
		if (emp.getSuperiorId() != null) {
			user.setProperty(IUser.SUPERIOR_ID, emp.getSuperiorId());
		}
		if (emp.getIsLeader() != null) {
			user.setProperty(IUser.IS_LEADER, emp.getIsLeader());
		}
		if (emp.getMobile() != null) {
			user.setProperty(IUser.MOBILE, emp.getMobile());
		}
		if (emp.getFaceTime() != null) {
			user.setProperty(IUser.FACE_TIME, emp.getFaceTime().toString());
		}
		if (emp.getDealerId() != null) {
			user.setProperty(IUser.DEALER_ID, emp.getDealerId());
		}
		if (emp.getDealerName() != null) {
			user.setProperty(IUser.DEALER_NAME, emp.getDealerName());
		}
		if (emp.getDealerCode() != null) {
			user.setProperty(IUser.DEALER_CODE, emp.getDealerCode());
		}
		if (emp.getOriginType() != null) {
			user.setProperty(IUser.ORIGIN_TYPE, emp.getOriginType());
		}
		if (emp.getName() != null) {
			user.setProperty(IUser.NAME, emp.getName());
		}
	}

	/** 安全读取 user_type，缺省为 0。 */
	private static short readShort(Object v) {
		if (v instanceof Number n) {
			return n.shortValue();
		}
		return 0;
	}
}
