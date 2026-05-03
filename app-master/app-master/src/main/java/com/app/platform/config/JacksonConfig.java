package com.app.platform.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

	/**
	 * Spring Boot 4 下 Web 相关自动配置未必注册 {@link ObjectMapper}，Filter 等需手动注入时使用。
	 */
	@Bean
	public ObjectMapper objectMapper() {
		// 默认实例；与 Spring MVC 消息转换器并存时需注意是否需 @Primary
		return new ObjectMapper();
	}
}
