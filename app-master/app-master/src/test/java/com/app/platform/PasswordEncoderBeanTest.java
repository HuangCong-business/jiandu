package com.app.platform;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/** 验证 BCrypt 编码与 matches 行为（与业务中密码存储方式一致）。 */
class PasswordEncoderBeanTest {

	@Test
	void encodeAndMatch_roundTrip() {
		PasswordEncoder encoder = new BCryptPasswordEncoder(10);
		String raw = "my-UTF8-密码";
		String hash = encoder.encode(raw);
		assertThat(hash).isNotBlank();
		assertThat(encoder.matches(raw, hash)).isTrue();
		assertThat(encoder.matches("wrong", hash)).isFalse();
	}
}
