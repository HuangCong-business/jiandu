package com.app.platform.core.authentication;

import java.io.Serial;
import java.io.Serializable;

/**
 * 放入 Session 的角色摘要（避免把 JPA 实体放进 HttpSession）。
 *
 * @param id   角色主键
 * @param code 角色编码（业务唯一）
 * @param name 展示名称
 */
public record RoleSnapshot(Long id, String code, String name) implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;
}
