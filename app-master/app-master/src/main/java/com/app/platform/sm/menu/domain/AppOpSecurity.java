package com.app.platform.sm.menu.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 挂在某菜单下的安全操作（按钮/接口级权限点）；{@code groupId} 非空时表示分组行。
 */
@Entity
@Table(name = "app_op_security")
public class AppOpSecurity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "menu_id", nullable = false)
	private AppMenu menu;

	/** 操作编码，与前端/注解中使用的权限码一致。 */
	@Column(nullable = false, length = 64)
	private String code;

	@Column(length = 128)
	private String name;

	/** 分组 id；为空表示菜单下默认/顶层操作行。 */
	@Column(name = "group_id")
	private Long groupId;

	@Column(nullable = false)
	private int sequ;

	@Column(nullable = false, length = 1)
	private String status;

	@Column(name = "creator_id")
	private Long creatorId;

	@Column(name = "create_time", nullable = false)
	private Instant createTime;

	@Column(name = "modifyier_id")
	private Long modifyierId;

	@Column(name = "modify_time", nullable = false)
	private Instant modifyTime;

	/** 新建时写入创建时间与修改时间。 */
	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		if (createTime == null) {
			createTime = now;
		}
		modifyTime = now;
	}

	/** 更新时刷新修改时间。 */
	@PreUpdate
	void preUpdate() {
		modifyTime = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public AppMenu getMenu() {
		return menu;
	}

	public void setMenu(AppMenu menu) {
		this.menu = menu;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public Long getGroupId() {
		return groupId;
	}

	public void setGroupId(Long groupId) {
		this.groupId = groupId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public int getSequ() {
		return sequ;
	}

	public void setSequ(int sequ) {
		this.sequ = sequ;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
