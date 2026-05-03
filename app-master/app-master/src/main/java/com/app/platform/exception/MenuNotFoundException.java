package com.app.platform.exception;

/** 菜单主键不存在或已逻辑删除（HTTP 404）。 */
public class MenuNotFoundException extends RuntimeException {

	public MenuNotFoundException() {
		super("菜单不存在");
	}
}
