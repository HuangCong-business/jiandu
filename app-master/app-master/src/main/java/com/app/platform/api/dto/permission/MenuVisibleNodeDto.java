package com.app.platform.api.dto.permission;

import java.util.ArrayList;
import java.util.List;

/**
 * 前端可见菜单树节点（可变 POJO），子节点列表在构造时初始化为空 ArrayList。
 */
public class MenuVisibleNodeDto {

	/** 菜单主键 */
	private Long id;
	/** 父菜单 id，根节点可为 null */
	private Long parentId;
	private String name;
	private String path;
	private String icon;
	/** 菜单类型（与 app_menu.menu_type 一致） */
	private String menuType;
	private int sequ;
	/** 子节点列表，树构建时往里追加 */
	private final List<MenuVisibleNodeDto> children = new ArrayList<>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getParentId() {
		return parentId;
	}

	public void setParentId(Long parentId) {
		this.parentId = parentId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	public String getMenuType() {
		return menuType;
	}

	public void setMenuType(String menuType) {
		this.menuType = menuType;
	}

	public int getSequ() {
		return sequ;
	}

	public void setSequ(int sequ) {
		this.sequ = sequ;
	}

	public List<MenuVisibleNodeDto> getChildren() {
		return children;
	}
}
