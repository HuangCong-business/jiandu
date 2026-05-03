package com.app.platform.service;

import com.app.platform.config.AuthProperties;
import com.app.platform.sm.user.domain.SmUser;
import com.app.platform.sm.user.repository.SmUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** 登录失败/成功时的用户表字段更新（锁定、清零错误次数等）。 */
@Service
public class UserLoginPersistence {

	private final SmUserRepository smUserRepository;
	private final AuthProperties authProperties;

	/** 注入仓储与认证阈值配置。 */
	public UserLoginPersistence(SmUserRepository smUserRepository, AuthProperties authProperties) {
		this.smUserRepository = smUserRepository;
		this.authProperties = authProperties;
	}

	/** 密码错误：累加计数，达到阈值则设置 lockedUntil。 */
	@Transactional
	public void recordWrongPassword(SmUser user) {
		SmUser managed = smUserRepository.findById(user.getId()).orElseThrow();
		int next = managed.getFailedLoginCount() + 1;
		managed.setFailedLoginCount(next);
		// 超过配置次数则临时锁定账号
		if (next >= authProperties.getMaxFailedLogins()) {
			managed.setLockedUntil(Instant.now().plusSeconds((long) authProperties.getLockDurationMinutes() * 60));
		}
		smUserRepository.save(managed);
	}

	/** 登录成功：刷新最后登录时间并解除锁定/清零失败次数。 */
	@Transactional
	public void recordSuccessfulLogin(Long userId) {
		SmUser managed = smUserRepository.findById(userId).orElseThrow();
		managed.setLastLoginTime(Instant.now());
		managed.setFailedLoginCount(0);
		managed.setLockedUntil(null);
		smUserRepository.save(managed);
	}
}
