package com.app.platform.service;

import com.app.platform.api.dto.LoginRequest;
import com.app.platform.api.dto.UserSessionDto;
import com.app.platform.core.authentication.Constants;
import com.app.platform.core.authentication.UserManager;
import com.app.platform.core.authentication.intf.IUser;
import com.app.platform.exception.AuthFailedException;
import com.app.platform.sm.user.crypto.LoginPasswordVerifier;
import com.app.platform.sm.user.domain.SmUser;
import com.app.platform.sm.user.repository.SmUserRepository;
import com.app.platform.sm.user.service.IUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

/** 登录会话编排：查库校验、防爆破、重建 Session 并绑定当前用户。 */
@Service
public class LoginService {

	private static final Logger log = LoggerFactory.getLogger(LoginService.class);

	// 持久化用户与密码相关依赖
	private final SmUserRepository smUserRepository;
	private final PasswordEncoder passwordEncoder;
	private final UserLoginPersistence userLoginPersistence;
	private final IUserService userService;

	/** 构造注入仓储、编码器与登录副作用服务。 */
	public LoginService(SmUserRepository smUserRepository, PasswordEncoder passwordEncoder,
			UserLoginPersistence userLoginPersistence, IUserService userService) {
		this.smUserRepository = smUserRepository;
		this.passwordEncoder = passwordEncoder;
		this.userLoginPersistence = userLoginPersistence;
		this.userService = userService;
	}

	/**
	 * 登录：校验 sm_user；成功后写入 Session（{@link Constants#SESSION_USER}）与 ThreadLocal。
	 */
	public UserSessionDto login(LoginRequest req, HttpServletRequest httpRequest) {
		// 登录名统一小写，避免大小写导致重复账号歧义
		String codeNorm = req.loginName().toLowerCase(Locale.ROOT);

		Optional<SmUser> userOpt = smUserRepository.findByCodeIgnoreCase(codeNorm);
		// 用户不存在时统一抛认证失败，避免枚举有效账号
		if (userOpt.isEmpty()) {
			log.warn("Login failed: user not found (masked)");
			throw new AuthFailedException();
		}
		SmUser account = userOpt.get();

		if (Constants.USER_STATUS_DELETED.equals(account.getStatus())) {
			log.warn("Login failed: deleted user userId={}", account.getId());
			throw new AuthFailedException();
		}
		// 非「正常」状态一律拒绝登录
		if (!Constants.USER_STATUS_NORMAL.equals(account.getStatus())) {
			log.warn("Login failed: invalid user status userId={} status={}", account.getId(), account.getStatus());
			throw new AuthFailedException();
		}

		Instant now = Instant.now();
		// 账户在锁定期内直接拒绝
		if (account.getLockedUntil() != null && account.getLockedUntil().isAfter(now)) {
			log.warn("Login failed: account locked userId={}", account.getId());
			throw new AuthFailedException();
		}

		if (!LoginPasswordVerifier.matches(req.password(), account, passwordEncoder)) {
			log.warn("Login failed: bad password userId={}", account.getId());
			// 记录错误次数，可能触发锁定策略
			userLoginPersistence.recordWrongPassword(account);
			throw new AuthFailedException();
		}

		// 登录成功：废弃旧 Session，防止会话固定攻击
		HttpSession old = httpRequest.getSession(false);
		if (old != null) {
			old.invalidate();
		}
		httpRequest.getSession(true);

		IUser user = userService.getById(account.getId());
		// 写入 ThreadLocal 与 HttpSession，供后续请求识别身份
		UserManager.setLoginUser(user, Constants.LOGIN_TYPE_WEB, httpRequest, true);

		return new UserSessionDto(account.getId(), account.getCode(), user.getDisplayName());
	}

	/** 显式登出：销毁当前 HTTP Session。 */
	public void logout(HttpServletRequest httpRequest) {
		HttpSession session = httpRequest.getSession(false);
		if (session != null) {
			session.invalidate();
		}
	}
}
