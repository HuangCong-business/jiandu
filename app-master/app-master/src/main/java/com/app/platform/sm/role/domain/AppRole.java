package com.app.platform.sm.role.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.time.LocalDate;

/** 角色主数据实体；{@code code} 在库表层唯一。 */
@Entity
@Table(name = "app_role", uniqueConstraints = @UniqueConstraint(name = "uq_app_role_code", columnNames = "code"))
public class AppRole {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 64)
	private String code;

	@Column(nullable = false, length = 128)
	private String name;

	@Column(name = "role_desc", length = 512)
	private String roleDesc;

	@Column(nullable = false)
	private int sequ;

	/** 是否内置角色，'1' 表示内置（业务约定）。 */
	@Column(name = "is_inner", nullable = false, length = 1)
	private String isInner;

	/** 是否可查看全部数据等粗粒度数据权限开关。 */
	@Column(name = "is_view_all", nullable = false, length = 1)
	private String isViewAll;

	@Column(nullable = false, length = 1)
	private String status;

	@Column(name = "validated_start_date")
	private LocalDate validatedStartDate;

	@Column(name = "validated_end_date")
	private LocalDate validatedEndDate;

	@Column(name = "creator_id")
	private Long creatorId;

	@Column(name = "create_time", nullable = false)
	private Instant createTime;

	@Column(name = "modifyier_id")
	private Long modifyierId;

	@Column(name = "modify_time", nullable = false)
	private Instant modifyTime;

	/** 首次持久化：补齐时间戳与布尔类字段默认值。 */
	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		if (createTime == null) {
			createTime = now;
		}
		modifyTime = now;
		if (isInner == null) {
			isInner = "0";
		}
		if (isViewAll == null) {
			isViewAll = "0";
		}
	}

	/** 更新前刷新修改时间。 */
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

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getRoleDesc() {
		return roleDesc;
	}

	public void setRoleDesc(String roleDesc) {
		this.roleDesc = roleDesc;
	}

	public int getSequ() {
		return sequ;
	}

	public void setSequ(int sequ) {
		this.sequ = sequ;
	}

	public String getIsInner() {
		return isInner;
	}

	public void setIsInner(String isInner) {
		this.isInner = isInner;
	}

	public String getIsViewAll() {
		return isViewAll;
	}

	public void setIsViewAll(String isViewAll) {
		this.isViewAll = isViewAll;
	}

	public LocalDate getValidatedStartDate() {
		return validatedStartDate;
	}

	public void setValidatedStartDate(LocalDate validatedStartDate) {
		this.validatedStartDate = validatedStartDate;
	}

	public LocalDate getValidatedEndDate() {
		return validatedEndDate;
	}

	public void setValidatedEndDate(LocalDate validatedEndDate) {
		this.validatedEndDate = validatedEndDate;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
