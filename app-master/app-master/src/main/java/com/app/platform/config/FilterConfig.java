package com.app.platform.config;

import com.app.platform.auth.filter.AdminAuthorizationFilter;
import com.app.platform.auth.filter.ApiAccessControlFilter;
import com.app.platform.auth.filter.AuthContextFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/** 注册 Servlet Filter 链顺序：先绑定用户上下文，再鉴权，再管理端二次校验。 */
@Configuration
public class FilterConfig {

	/** Session → ThreadLocal，优先级最高业务 Filter 之一。 */
	@Bean
	public FilterRegistrationBean<AuthContextFilter> authContextFilterRegistration() {
		FilterRegistrationBean<AuthContextFilter> bean = new FilterRegistrationBean<>();
		bean.setFilter(new AuthContextFilter());
		bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
		bean.addUrlPatterns("/*");
		return bean;
	}

	/** /api/** 登录门槛（除白名单）。 */
	@Bean
	public FilterRegistrationBean<ApiAccessControlFilter> apiAccessControlFilterRegistration(
			AuthProperties authProperties,
			ObjectMapper objectMapper) {
		FilterRegistrationBean<ApiAccessControlFilter> bean = new FilterRegistrationBean<>();
		bean.setFilter(new ApiAccessControlFilter(authProperties, objectMapper));
		bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 30);
		bean.addUrlPatterns("/*");
		return bean;
	}

	/** /api/admin/** 平台管理员校验。 */
	@Bean
	public FilterRegistrationBean<AdminAuthorizationFilter> adminAuthorizationFilterRegistration(
			AuthProperties authProperties,
			ObjectMapper objectMapper) {
		FilterRegistrationBean<AdminAuthorizationFilter> bean = new FilterRegistrationBean<>();
		bean.setFilter(new AdminAuthorizationFilter(authProperties, objectMapper));
		bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 35);
		bean.addUrlPatterns("/*");
		return bean;
	}
}
