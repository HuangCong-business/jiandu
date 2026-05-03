package com.app.platform.sm.menu.domain;

import com.app.platform.sm.role.domain.AppRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/** 角色被授予某条 {@link AppOpSecurity} 的关联记录。 */
@Entity
@Table(name = "app_op_assign")
public class AppOpAssign {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "role_id", nullable = false)
	private AppRole role;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "op_id", nullable = false)
	private AppOpSecurity op;

	@Column(name = "creator_id")
	private Long creatorId;

	@Column(name = "create_time", nullable = false)
	private Instant createTime;

	/** 分配行首次保存时写入创建时间。 */
	@PrePersist
	void prePersist() {
		if (createTime == null) {
			createTime = Instant.now();
		}
	}

	public Long getId() {
		return id;
	}

	public AppRole getRole() {
		return role;
	}

	public void setRole(AppRole role) {
		this.role = role;
	}

	public AppOpSecurity getOp() {
		return op;
	}

	public void setOp(AppOpSecurity op) {
		this.op = op;
	}
}
