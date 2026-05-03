package com.app.platform.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/** 统一错误包装：含业务错误码、可读说明与可选结构化详情。 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorBody(boolean success, String code, String message, Object details) {

	public static ApiErrorBody of(String code, String message) {
		return new ApiErrorBody(false, code, message, null);
	}

	/** 附带校验明细等扩展信息。 */
	public static ApiErrorBody withDetails(String code, String message, Object details) {
		return new ApiErrorBody(false, code, message, details);
	}
}
