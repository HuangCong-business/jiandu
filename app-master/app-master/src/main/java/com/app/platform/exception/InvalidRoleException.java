package com.app.platform.exception;

/** 引用的角色不存在、已停用或数据不一致。 */
public class InvalidRoleException extends RuntimeException {

	public InvalidRoleException() {
		super("角色无效或不可用");
	}
}
