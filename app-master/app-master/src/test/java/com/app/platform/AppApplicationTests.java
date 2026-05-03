package com.app.platform;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Spring Boot 应用上下文加载冒烟测试。 */
@SpringBootTest
@ActiveProfiles("test")
class AppApplicationTests {

	/** 若容器无法启动，本方法会失败。 */
	@Test
	void contextLoads() {
	}

}
