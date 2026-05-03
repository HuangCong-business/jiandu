package com.app.platform.exception;

/** 角色主键不存在（HTTP 404）。 */
public class RoleNotFoundException extends RuntimeException {

	public RoleNotFoundException() {
		super("角色不存在");
	}
}
