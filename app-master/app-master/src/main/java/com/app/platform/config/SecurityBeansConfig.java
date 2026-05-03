package com.app.platform.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** 启用 {@link AuthProperties} 并注册密码编码器 Bean。 */
@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityBeansConfig {

	/** BCrypt，强度来自配置 {@code app.auth.bcrypt-strength}。 */
	@Bean
	public PasswordEncoder passwordEncoder(AuthProperties authProperties) {
		return new BCryptPasswordEncoder(authProperties.getBcryptStrength());
	}
}
