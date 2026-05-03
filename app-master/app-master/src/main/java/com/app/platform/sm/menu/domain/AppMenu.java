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
 * 系统菜单节点，支持父子层级与多端 {@code clientType}。
 */
@Entity
@Table(name = "app_menu")
public class AppMenu {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_id")
	private AppMenu parent;

	@Column(nullable = false, length = 128)
	private String name;

	@Column(length = 256)
	private String path;

	@Column(length = 128)
	private String icon;

	@Column(name = "menu_type", nullable = false, length = 1)
	private String menuType;

	@Column(name = "client_type", nullable = false, length = 16)
	private String clientType;

	@Column(nullable = false)
	private int sequ;

	@Column(nullable = false, length = 1)
	private String status;

	@Column(name = "control_type", length = 16)
	private String controlType;

	@Column(name = "creator_id")
	private Long creatorId;

	@Column(name = "create_time", nullable = false)
	private Instant createTime;

	@Column(name = "modifyier_id")
	private Long modifyierId;

	@Column(name = "modify_time", nullable = false)
	private Instant modifyTime;

	/** 新建菜单时初始化创建时间与修改时间。 */
	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		if (createTime == null) {
			createTime = now;
		}
		modifyTime = now;
	}

	/** 任意字段更新时刷新修改时间。 */
	@PreUpdate
	void preUpdate() {
		modifyTime = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getMenuType() {
		return menuType;
	}

	public void setMenuType(String menuType) {
		this.menuType = menuType;
	}

	public String getClientType() {
		return clientType;
	}

	public void setClientType(String clientType) {
		this.clientType = clientType;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public AppMenu getParent() {
		return parent;
	}

	public void setParent(AppMenu parent) {
		this.parent = parent;
	}

	/** 父节点主键，无父时为 null。 */
	public Long getParentId() {
		return parent != null ? parent.getId() : null;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public int getSequ() {
		return sequ;
	}

	public void setSequ(int sequ) {
		this.sequ = sequ;
	}

	public String getControlType() {
		return controlType;
	}

	public void setControlType(String controlType) {
		this.controlType = controlType;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setCreatorId(Long creatorId) {
		this.creatorId = creatorId;
	}

	public void setModifyierId(Long modifyierId) {
		this.modifyierId = modifyierId;
	}
}
