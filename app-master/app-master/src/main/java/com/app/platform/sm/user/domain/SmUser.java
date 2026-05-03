package com.app.platform.sm.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;

/** 平台登录账号实体：多版本密码字段、状态与审计时间戳。 */
@Entity
@Table(name = "sm_user", uniqueConstraints = @UniqueConstraint(name = "uq_sm_user_code", columnNames = "code"))
public class SmUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 64)
	private String code;

	@Column(length = 128)
	private String name;

	// 历史 MD5 / SHA / 现代 bcrypt 三选一有值即可登录校验
	@Column(length = 64)
	private String password;

	@Column(name = "sha_password", length = 128)
	private String shaPassword;

	@Column(name = "password_bcrypt", length = 255)
	private String passwordBcrypt;

	@Column(name = "user_type", nullable = false)
	private short userType;

	@Column(nullable = false, length = 1)
	private String status;

	@Column(name = "last_login_time")
	private Instant lastLoginTime;

	@Column(name = "last_use_time")
	private Instant lastUseTime;

	@Column(name = "pwdedit_time")
	private Instant pwdeditTime;

	@Column(name = "failed_login_count", nullable = false)
	private int failedLoginCount;

	@Column(name = "locked_until")
	private Instant lockedUntil;

	@Column(name = "user_ex1", length = 32)
	private String userEx1;

	@Column(name = "user_ex2", length = 32)
	private String userEx2;

	@Column(name = "user_ex3", length = 32)
	private String userEx3;

	@Column(name = "user_ex4", length = 32)
	private String userEx4;

	@Column(name = "client_type", precision = 19)
	private BigDecimal clientType;

	// 审计字段
	@Column(name = "creator_id")
	private Long creatorId;

	@Column(name = "create_time", nullable = false)
	private Instant createTime;

	@Column(name = "modifyier_id")
	private Long modifyierId;

	@Column(name = "modify_time", nullable = false)
	private Instant modifyTime;

	/** 新建记录时若未设 create_time 则填当前时间，并同步 modify_time。 */
	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		if (createTime == null) {
			createTime = now;
		}
		modifyTime = now;
	}

	/** 每次更新刷新 modify_time。 */
	@PreUpdate
	void preUpdate() {
		modifyTime = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getShaPassword() {
		return shaPassword;
	}

	public void setShaPassword(String shaPassword) {
		this.shaPassword = shaPassword;
	}

	public String getPasswordBcrypt() {
		return passwordBcrypt;
	}

	public void setPasswordBcrypt(String passwordBcrypt) {
		this.passwordBcrypt = passwordBcrypt;
	}

	public short getUserType() {
		return userType;
	}

	public void setUserType(short userType) {
		this.userType = userType;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Instant getLastLoginTime() {
		return lastLoginTime;
	}

	public void setLastLoginTime(Instant lastLoginTime) {
		this.lastLoginTime = lastLoginTime;
	}

	public Instant getLastUseTime() {
		return lastUseTime;
	}

	public void setLastUseTime(Instant lastUseTime) {
		this.lastUseTime = lastUseTime;
	}

	public Instant getPwdeditTime() {
		return pwdeditTime;
	}

	public void setPwdeditTime(Instant pwdeditTime) {
		this.pwdeditTime = pwdeditTime;
	}

	public int getFailedLoginCount() {
		return failedLoginCount;
	}

	public void setFailedLoginCount(int failedLoginCount) {
		this.failedLoginCount = failedLoginCount;
	}

	public Instant getLockedUntil() {
		return lockedUntil;
	}

	public void setLockedUntil(Instant lockedUntil) {
		this.lockedUntil = lockedUntil;
	}

	public String getUserEx1() {
		return userEx1;
	}

	public void setUserEx1(String userEx1) {
		this.userEx1 = userEx1;
	}
}
