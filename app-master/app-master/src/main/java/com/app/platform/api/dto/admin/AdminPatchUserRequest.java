package com.app.platform.api.dto.admin;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 管理端 PATCH 用户：标准 JavaBean，仅非空字段参与更新；null 字段由 Jackson 忽略策略配合。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminPatchUserRequest {

	private String name;
	private String status;
	private Short userType;
	private Long deptId;
	private String mobile;

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

	public Short getUserType() {
		return userType;
	}

	public void setUserType(Short userType) {
		this.userType = userType;
	}

	public Long getDeptId() {
		return deptId;
	}

	public void setDeptId(Long deptId) {
		this.deptId = deptId;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}
}
