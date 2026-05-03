package com.app.platform.api.dto;

/** 登录会话中暴露给前端的用户摘要（不含敏感字段）。 */
public record UserSessionDto(long userId, String loginName, String displayName) {
}
