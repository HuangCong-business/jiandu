package com.app.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Spring Boot 入口：扫描组件并启动内嵌 Web 容器。 */
@SpringBootApplication
public class AppApplication {

	/**
	 * JVM 入口：委托 Spring 加载配置并启动应用上下文。
	 */
	public static void main(String[] args) {
		SpringApplication.run(AppApplication.class, args);
	}

}
