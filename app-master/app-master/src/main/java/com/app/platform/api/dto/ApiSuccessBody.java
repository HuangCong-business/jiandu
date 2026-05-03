package com.app.platform.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/** 统一成功包装：{@code success} 恒为 true，{@code data} 可为业务载荷。 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiSuccessBody<T>(boolean success, T data) {

	/** 仅携带业务数据的成功体。 */
	public static <T> ApiSuccessBody<T> of(T data) {
		return new ApiSuccessBody<>(true, data);
	}

	/** 无载荷的成功体（例如 204 场景若需 JSON 可用）。 */
	public static ApiSuccessBody<Void> empty() {
		return new ApiSuccessBody<>(true, null);
	}
}
