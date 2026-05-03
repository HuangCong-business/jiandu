package com.app.platform.org.employee.domain;

import com.app.platform.sm.user.domain.SmUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 员工主数据；与 {@link SmUser} 一对一共享主键（{@code @MapsId}）。
 */
@Entity
@Table(name = "sys_employee")
public class SysEmployee {

	@Id
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId
	@JoinColumn(name = "id")
	private SmUser smUser;

	@Column(nullable = false, length = 1)
	private String status;

	@Column(length = 64)
	private String code;

	@Column(length = 128)
	private String name;

	@Column(name = "name_spell", length = 128)
	private String nameSpell;

	@Column(name = "dept_id")
	private Long deptId;

	@Column(name = "superior_id")
	private Long superiorId;

	@Column(name = "is_leader", length = 1)
	private String isLeader;

	@Column(length = 32)
	private String mobile;

	@Column(length = 32)
	private String tel;

	@Column(length = 128)
	private String email;

	@Column(name = "face_time")
	private Instant faceTime;

	@Column(name = "dealer_id")
	private Long dealerId;

	@Column(name = "dealer_name", length = 128)
	private String dealerName;

	@Column(name = "dealer_code", length = 64)
	private String dealerCode;

	@Column(name = "user_id")
	private Long userId;

	@Column(name = "origin_type", length = 16)
	private String originType;

	@Column(name = "creator_id")
	private Long creatorId;

	@Column(name = "create_time", nullable = false)
	private Instant createTime;

	@Column(name = "modifyier_id")
	private Long modifyierId;

	@Column(name = "modify_time", nullable = false)
	private Instant modifyTime;

	/** 员工记录首次持久化时补齐时间戳。 */
	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		if (createTime == null) {
			createTime = now;
		}
		modifyTime = now;
	}

	/** 更新员工信息时刷新修改时间。 */
	@PreUpdate
	void preUpdate() {
		modifyTime = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public SmUser getSmUser() {
		return smUser;
	}

	public void setSmUser(SmUser smUser) {
		this.smUser = smUser;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
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

	public Long getDeptId() {
		return deptId;
	}

	public void setDeptId(Long deptId) {
		this.deptId = deptId;
	}

	public Long getSuperiorId() {
		return superiorId;
	}

	public void setSuperiorId(Long superiorId) {
		this.superiorId = superiorId;
	}

	public String getIsLeader() {
		return isLeader;
	}

	public void setIsLeader(String isLeader) {
		this.isLeader = isLeader;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public Instant getFaceTime() {
		return faceTime;
	}

	public void setFaceTime(Instant faceTime) {
		this.faceTime = faceTime;
	}

	public Long getDealerId() {
		return dealerId;
	}

	public void setDealerId(Long dealerId) {
		this.dealerId = dealerId;
	}

	public String getDealerName() {
		return dealerName;
	}

	public void setDealerName(String dealerName) {
		this.dealerName = dealerName;
	}

	public String getDealerCode() {
		return dealerCode;
	}

	public void setDealerCode(String dealerCode) {
		this.dealerCode = dealerCode;
	}

	public String getOriginType() {
		return originType;
	}

	public void setOriginType(String originType) {
		this.originType = originType;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}
}
