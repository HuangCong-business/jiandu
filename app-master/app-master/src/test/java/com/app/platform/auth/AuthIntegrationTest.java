package com.app.platform.auth;

import com.app.platform.core.authentication.Constants;
import com.app.platform.org.employee.domain.SysEmployee;
import com.app.platform.org.employee.repository.SysEmployeeRepository;
import com.app.platform.sm.role.domain.AppRole;
import com.app.platform.sm.role.domain.SmRoleUser;
import com.app.platform.sm.role.repository.AppRoleRepository;
import com.app.platform.sm.role.repository.SmRoleUserRepository;
import com.app.platform.sm.user.domain.SmUser;
import com.app.platform.sm.user.repository.SmUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 认证相关集成测试：登录、会话、登出、注册及 Actuator 匿名访问等。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private SmUserRepository smUserRepository;

	@Autowired
	private SysEmployeeRepository sysEmployeeRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AppRoleRepository appRoleRepository;

	@Autowired
	private SmRoleUserRepository smRoleUserRepository;

	/** 每个用例前清空用户/员工并插入标准测试账号「zhangsan」。 */
	@BeforeEach
	void resetData() {
		sysEmployeeRepository.deleteAll();
		smUserRepository.deleteAll();

		SmUser sm = new SmUser();
		sm.setCode("zhangsan");
		sm.setName("张三");
		sm.setPasswordBcrypt(passwordEncoder.encode("secret"));
		sm.setUserType((short) 0);
		sm.setStatus(Constants.USER_STATUS_NORMAL);
		sm.setFailedLoginCount(0);
		smUserRepository.save(sm);

		SysEmployee emp = new SysEmployee();
		emp.setSmUser(sm);
		emp.setStatus(Constants.USER_STATUS_NORMAL);
		emp.setCode("zhangsan");
		emp.setName("张三");
		emp.setUserId(sm.getId());
		sysEmployeeRepository.save(emp);
	}

	/** AC1：正确登录后同一会话可访问 /me。 */
	@Test
	void ac1_correctLogin_thenMe() throws Exception {
		MockHttpSession sessionBefore = new MockHttpSession();
		var loginResult = mockMvc.perform(post("/api/auth/login")
						.session(sessionBefore)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"loginName\":\"zhangsan\",\"password\":\"secret\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.userId").exists())
				.andExpect(jsonPath("$.data.loginName").value("zhangsan"))
				.andReturn();

		MockHttpSession sessionAfterLogin = (MockHttpSession) loginResult.getRequest().getSession(false);
		assertThat(sessionAfterLogin).isNotNull();

		mockMvc.perform(get("/api/auth/me").session(sessionAfterLogin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.loginName").value("zhangsan"));
	}

	/** AC2：错误密码与不存在用户返回相同提示，避免枚举有效账号。 */
	@Test
	void ac2_wrongPassword_sameMessageAsMissingUser() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"loginName\":\"zhangsan\",\"password\":\"bad\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("AUTH_FAILED"))
				.andExpect(jsonPath("$.message").value("账号或密码错误"));

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"loginName\":\"nobody\",\"password\":\"secret\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("账号或密码错误"));
	}

	/** AC4：冻结用户无法登录。 */
	@Test
	void ac4_disabledUser() throws Exception {
		SmUser u = smUserRepository.findByCodeIgnoreCase("zhangsan").orElseThrow();
		u.setStatus(Constants.USER_STATUS_FREEZE);
		smUserRepository.save(u);

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"loginName\":\"zhangsan\",\"password\":\"secret\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("账号或密码错误"));
	}

	/** AC6：登出后会话失效，再访问 /me 返回 401。 */
	@Test
	void ac6_logoutThenMe401() throws Exception {
		var loginResult = mockMvc.perform(post("/api/auth/login")
						.session(new MockHttpSession())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"loginName\":\"zhangsan\",\"password\":\"secret\"}"))
				.andExpect(status().isOk())
				.andReturn();
		MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
		assertThat(session).isNotNull();

		mockMvc.perform(post("/api/auth/logout").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

		mockMvc.perform(get("/api/auth/me").session(session))
				.andExpect(status().isUnauthorized());
	}

	/** AC7：无会话访问 /me 应未授权。 */
	@Test
	void ac7_meWithoutSession401() throws Exception {
		mockMvc.perform(get("/api/auth/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	/** Actuator 健康检查允许匿名。 */
	@Test
	void actuatorHealthAnonymous() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk());
	}

	/** 登录请求参数校验失败时返回 400 与校验错误码。 */
	@Test
	void validationError400() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"loginName\":\"\",\"password\":\"x\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	/** 注册成功创建用户与员工，并绑定默认角色（无种子时本地补建角色）。 */
	@Test
	void registerCreatesUser_201() throws Exception {
		sysEmployeeRepository.deleteAll();
		smUserRepository.deleteAll();
		// test 环境未跑 Flyway V4 种子时，补建默认角色
		if (appRoleRepository.findByCode(Constants.APP_ROLE_CODE_NORMAL_USER).isEmpty()) {
			AppRole r = new AppRole();
			r.setCode(Constants.APP_ROLE_CODE_NORMAL_USER);
			r.setName("普通用户");
			r.setStatus("1");
			r.setSequ(0);
			r.setIsInner("0");
			r.setIsViewAll("0");
			appRoleRepository.save(r);
		}

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"loginName\":\"newuser\",\"password\":\"pass12345\",\"displayName\":\"新用户\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.loginName").value("newuser"))
				.andExpect(jsonPath("$.data.displayName").value("新用户"));

		assertThat(smUserRepository.findByCodeIgnoreCase("newuser")).isPresent();
		Long newUserId = smUserRepository.findByCodeIgnoreCase("newuser").orElseThrow().getId();
		assertThat(sysEmployeeRepository.findById(newUserId)).isPresent();
		var normalRole = appRoleRepository.findByCode(Constants.APP_ROLE_CODE_NORMAL_USER);
		assertThat(normalRole).as("本用例已补建 app_role normal_user").isPresent();
		assertThat(smRoleUserRepository.findByUserId(newUserId))
				.extracting(SmRoleUser::getRoleId)
				.contains(normalRole.get().getId());
	}

	/** 重复注册同一登录名返回 409。 */
	@Test
	void registerDuplicate_409() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"loginName\":\"zhangsan\",\"password\":\"other\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("USERNAME_TAKEN"))
				.andExpect(jsonPath("$.message").value("用户名已被使用"));
	}

	/** 注册接口不应建立登录会话。 */
	@Test
	void registerDoesNotCreateSession() throws Exception {
		sysEmployeeRepository.deleteAll();
		smUserRepository.deleteAll();

		MockHttpSession session = new MockHttpSession();
		mockMvc.perform(post("/api/auth/register")
						.session(session)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"loginName\":\"solo\",\"password\":\"secret99\"}"))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/auth/me").session(session))
				.andExpect(status().isUnauthorized());
	}
}
