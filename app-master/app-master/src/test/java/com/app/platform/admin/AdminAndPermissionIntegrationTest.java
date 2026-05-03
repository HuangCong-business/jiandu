package com.app.platform.admin;

import com.app.platform.core.authentication.Constants;
import com.app.platform.org.employee.domain.SysEmployee;
import com.app.platform.org.employee.repository.SysEmployeeRepository;
import com.app.platform.sm.menu.domain.AppMenu;
import com.app.platform.sm.menu.domain.AppOpAssign;
import com.app.platform.sm.menu.domain.AppOpSecurity;
import com.app.platform.sm.menu.repository.AppMenuRepository;
import com.app.platform.sm.menu.repository.AppOpAssignRepository;
import com.app.platform.sm.menu.repository.AppOpSecurityRepository;
import com.app.platform.sm.role.domain.AppRole;
import com.app.platform.sm.role.domain.SmRoleUser;
import com.app.platform.sm.role.repository.AppRoleRepository;
import com.app.platform.sm.role.repository.SmRoleUserRepository;
import com.app.platform.sm.user.domain.SmUser;
import com.app.platform.sm.user.repository.SmUserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 管理端与权限 API 集成测试：角色、菜单、操作分配及可见菜单等端到端行为。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminAndPermissionIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private SmUserRepository smUserRepository;

	@Autowired
	private SysEmployeeRepository sysEmployeeRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AppMenuRepository appMenuRepository;

	@Autowired
	private AppOpSecurityRepository appOpSecurityRepository;

	@Autowired
	private AppOpAssignRepository appOpAssignRepository;

	@Autowired
	private AppRoleRepository appRoleRepository;

	@Autowired
	private SmRoleUserRepository smRoleUserRepository;

	@Autowired
	private ObjectMapper objectMapper;

	/** 清空权限相关表与用户数据，并创建普通用户 zhangsan 作为后续登录主体。 */
	@BeforeEach
	void reset() {
		appOpAssignRepository.deleteAll();
		appOpSecurityRepository.deleteAll();
		appMenuRepository.deleteAll();
		smRoleUserRepository.deleteAll();
		sysEmployeeRepository.deleteAll();
		smUserRepository.deleteAll();
		appRoleRepository.deleteAll();

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

	/** 以 zhangsan/secret 登录并返回绑定了会话的 MockHttpSession。 */
	private MockHttpSession loginZhangsan() throws Exception {
		var r = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login")
						.session(new MockHttpSession())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"loginName\":\"zhangsan\",\"password\":\"secret\"}"))
				.andExpect(status().isOk())
				.andReturn();
		return (MockHttpSession) r.getRequest().getSession(false);
	}

	/** 非管理员访问用户管理列表应 403。 */
	@Test
	void normalUser_cannotAccessAdminUsers() throws Exception {
		MockHttpSession session = loginZhangsan();
		mockMvc.perform(get("/api/admin/users").session(session))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	/** 未登录访问菜单操作码接口应 401。 */
	@Test
	void opCodes_requiresLogin() throws Exception {
		mockMvc.perform(get("/api/permissions/menus/1/op-codes"))
				.andExpect(status().isUnauthorized());
	}

	/** 菜单不存在时返回 404 与约定错误码。 */
	@Test
	void opCodes_menuNotFound() throws Exception {
		MockHttpSession session = loginZhangsan();
		mockMvc.perform(get("/api/permissions/menus/99999/op-codes").session(session))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("MENU_NOT_FOUND"));
	}

	/** 用户经角色分配到某操作后，可查询到对应 op code。 */
	@Test
	void opCodes_assignedRole_returnsCode() throws Exception {
		AppMenu menu = new AppMenu();
		menu.setName("测试菜单");
		menu.setMenuType(Constants.MENU_TYPE_EMP);
		menu.setClientType("1");
		menu.setStatus("1");
		appMenuRepository.save(menu);

		AppOpSecurity op = new AppOpSecurity();
		op.setMenu(menu);
		op.setCode("ADD");
		op.setStatus("1");
		appOpSecurityRepository.save(op);

		AppRole role = new AppRole();
		role.setCode("r1");
		role.setName("角色1");
		role.setStatus("1");
		appRoleRepository.save(role);

		AppOpAssign assign = new AppOpAssign();
		assign.setRole(role);
		assign.setOp(op);
		appOpAssignRepository.save(assign);

		SmUser sm = smUserRepository.findByCodeIgnoreCase("zhangsan").orElseThrow();
		SmRoleUser ru = new SmRoleUser();
		ru.setUserId(sm.getId());
		ru.setRoleId(role.getId());
		smRoleUserRepository.save(ru);

		MockHttpSession session = loginZhangsan();
		mockMvc.perform(get("/api/permissions/menus/" + menu.getId() + "/op-codes").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.opCodes[0]").value("ADD"));
	}

	/** 管理员替换角色菜单权限后，普通用户可看到对应可见菜单。 */
	@Test
	void admin_canReplaceRoleMenuPermissions_and_visibleMenus() throws Exception {
		AppMenu menu = new AppMenu();
		menu.setName("根菜单");
		menu.setMenuType(Constants.MENU_TYPE_EMP);
		menu.setClientType("1");
		menu.setStatus("1");
		menu.setSequ(0);
		appMenuRepository.save(menu);

		AppOpSecurity op = new AppOpSecurity();
		op.setMenu(menu);
		op.setCode("VIEW");
		op.setStatus("1");
		op.setSequ(0);
		appOpSecurityRepository.save(op);

		AppRole role = new AppRole();
		role.setCode("r_admin_test");
		role.setName("测试角色");
		role.setStatus("1");
		role.setSequ(0);
		role.setIsInner("0");
		role.setIsViewAll("0");
		appRoleRepository.save(role);

		SmUser adminUser = smUserRepository.findByCodeIgnoreCase("zhangsan").orElseThrow();
		adminUser.setUserType((short) 9);
		smUserRepository.save(adminUser);

		MockHttpSession adminSession = loginZhangsan();
		mockMvc.perform(put("/api/admin/roles/" + role.getId() + "/menu-permissions")
						.param("clientType", "1")
						.session(adminSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"opIds\":[" + op.getId() + "]}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.opIds[0]").value(op.getId().intValue()));

		SmUser sm = smUserRepository.findByCodeIgnoreCase("zhangsan").orElseThrow();
		sm.setUserType((short) 0);
		smUserRepository.save(sm);
		SmRoleUser ru = new SmRoleUser();
		ru.setUserId(sm.getId());
		ru.setRoleId(role.getId());
		smRoleUserRepository.save(ru);

		MockHttpSession userSession = loginZhangsan();
		mockMvc.perform(get("/api/permissions/menus/visible").param("clientType", "1").session(userSession))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].id").value(menu.getId().intValue()));
	}

	/** 管理端可拉取角色下拉选项。 */
	@Test
	void admin_canListRoleSelectOptions() throws Exception {
		AppRole role = new AppRole();
		role.setCode("opt_r");
		role.setName("可选角色");
		role.setStatus("1");
		role.setSequ(0);
		role.setIsInner("0");
		role.setIsViewAll("0");
		appRoleRepository.save(role);

		SmUser adminUser = smUserRepository.findByCodeIgnoreCase("zhangsan").orElseThrow();
		adminUser.setUserType((short) 9);
		smUserRepository.save(adminUser);

		MockHttpSession adminSession = loginZhangsan();
		mockMvc.perform(get("/api/admin/roles/select-options").session(adminSession))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].id").value(role.getId().intValue()))
				.andExpect(jsonPath("$.data[0].code").value("opt_r"))
				.andExpect(jsonPath("$.data[0].name").value("可选角色"));
	}

	/** 管理端可按客户端类型列出菜单操作下拉项。 */
	@Test
	void admin_canListMenuOpSelectOptions() throws Exception {
		AppMenu menu = new AppMenu();
		menu.setName("菜单A");
		menu.setMenuType(Constants.MENU_TYPE_EMP);
		menu.setClientType("1");
		menu.setStatus("1");
		menu.setSequ(0);
		appMenuRepository.save(menu);

		AppOpSecurity op = new AppOpSecurity();
		op.setMenu(menu);
		op.setCode("VIEW_A");
		op.setName("查看");
		op.setStatus("1");
		op.setSequ(0);
		appOpSecurityRepository.save(op);

		SmUser adminUser = smUserRepository.findByCodeIgnoreCase("zhangsan").orElseThrow();
		adminUser.setUserType((short) 9);
		smUserRepository.save(adminUser);

		MockHttpSession adminSession = loginZhangsan();
		mockMvc.perform(get("/api/admin/menu-ops/select-options").param("clientType", "1").session(adminSession))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].id").value(op.getId().intValue()))
				.andExpect(jsonPath("$.data[0].code").value("VIEW_A"))
				.andExpect(jsonPath("$.data[0].menuName").value("菜单A"));
	}

	/** 管理端树形行接口返回父子顺序与 parentId。 */
	@Test
	void admin_canListMenuTreeRows() throws Exception {
		AppMenu parent = new AppMenu();
		parent.setName("父菜单");
		parent.setMenuType(Constants.MENU_TYPE_EMP);
		parent.setClientType("1");
		parent.setStatus("1");
		parent.setSequ(0);
		appMenuRepository.save(parent);

		AppMenu child = new AppMenu();
		child.setName("子菜单");
		child.setMenuType(Constants.MENU_TYPE_EMP);
		child.setClientType("1");
		child.setStatus("1");
		child.setSequ(0);
		child.setParent(parent);
		appMenuRepository.save(child);

		SmUser adminUser = smUserRepository.findByCodeIgnoreCase("zhangsan").orElseThrow();
		adminUser.setUserType((short) 9);
		smUserRepository.save(adminUser);

		MockHttpSession adminSession = loginZhangsan();
		mockMvc.perform(get("/api/admin/menus/tree-rows").param("clientType", "1").session(adminSession))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].name").value("父菜单"))
				.andExpect(jsonPath("$.data[1].name").value("子菜单"))
				.andExpect(jsonPath("$.data[1].parentId").value(parent.getId().intValue()));
	}

	/**
	 * 替换某 clientType 下的角色菜单权限时，不应影响另一客户端类型上已有分配。
	 */
	@Test
	void replaceMenuPermissions_onlyReplacesSameClientType_assigns() throws Exception {
		AppMenu webMenu = new AppMenu();
		webMenu.setName("Web端菜单");
		webMenu.setMenuType(Constants.MENU_TYPE_EMP);
		webMenu.setClientType("1");
		webMenu.setStatus("1");
		appMenuRepository.save(webMenu);

		AppMenu appMenu = new AppMenu();
		appMenu.setName("App端菜单");
		appMenu.setMenuType(Constants.MENU_TYPE_EMP);
		appMenu.setClientType("2");
		appMenu.setStatus("1");
		appMenuRepository.save(appMenu);

		AppOpSecurity opWeb = new AppOpSecurity();
		opWeb.setMenu(webMenu);
		opWeb.setCode("W1");
		opWeb.setStatus("1");
		appOpSecurityRepository.save(opWeb);

		AppOpSecurity opApp = new AppOpSecurity();
		opApp.setMenu(appMenu);
		opApp.setCode("A1");
		opApp.setStatus("1");
		appOpSecurityRepository.save(opApp);

		AppRole role = new AppRole();
		role.setCode("dual_client_r");
		role.setName("双端角色");
		role.setStatus("1");
		role.setSequ(0);
		role.setIsInner("0");
		role.setIsViewAll("0");
		appRoleRepository.save(role);

		AppOpAssign a1 = new AppOpAssign();
		a1.setRole(role);
		a1.setOp(opWeb);
		appOpAssignRepository.save(a1);
		AppOpAssign a2 = new AppOpAssign();
		a2.setRole(role);
		a2.setOp(opApp);
		appOpAssignRepository.save(a2);

		SmUser adminUser = smUserRepository.findByCodeIgnoreCase("zhangsan").orElseThrow();
		adminUser.setUserType((short) 9);
		smUserRepository.save(adminUser);

		MockHttpSession adminSession = loginZhangsan();
		mockMvc.perform(put("/api/admin/roles/" + role.getId() + "/menu-permissions")
						.param("clientType", "1")
						.session(adminSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"opIds\":[" + opWeb.getId() + "]}"))
				.andExpect(status().isOk());

		org.assertj.core.api.Assertions.assertThat(appOpAssignRepository.findOpIdsByRoleIdAndMenuClientType(role.getId(), "1"))
				.containsExactly(opWeb.getId());
		org.assertj.core.api.Assertions.assertThat(appOpAssignRepository.findOpIdsByRoleIdAndMenuClientType(role.getId(), "2"))
				.containsExactly(opApp.getId());
	}

	/** 菜单 CRUD、分配树查询及「有子不可删父」等业务规则。 */
	@Test
	void admin_menuCrud_assignTree_andDeleteRules() throws Exception {
		SmUser adminUser = smUserRepository.findByCodeIgnoreCase("zhangsan").orElseThrow();
		adminUser.setUserType((short) 9);
		smUserRepository.save(adminUser);
		MockHttpSession adminSession = loginZhangsan();

		var created = mockMvc.perform(post("/api/admin/menus")
						.session(adminSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"根-CRUD\",\"clientType\":\"1\",\"sequ\":1}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.name").value("根-CRUD"))
				.andReturn();
		JsonNode rootNode = objectMapper.readTree(created.getResponse().getContentAsString());
		long rootId = rootNode.get("data").get("id").asLong();

		mockMvc.perform(get("/api/admin/menus/assign-tree").param("clientType", "1").session(adminSession))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].name").value("根-CRUD"))
				.andExpect(jsonPath("$.data[0].id").value((int) rootId));

		mockMvc.perform(get("/api/admin/roles/menu-assign-tree").param("clientType", "1").session(adminSession))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].name").value("根-CRUD"));

		var childCreated = mockMvc.perform(post("/api/admin/menus")
						.session(adminSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"子菜单\",\"clientType\":\"1\",\"parentId\":" + rootId + "}"))
				.andExpect(status().isCreated())
				.andReturn();
		long childId = objectMapper.readTree(childCreated.getResponse().getContentAsString()).get("data").get("id").asLong();

		mockMvc.perform(delete("/api/admin/menus/" + rootId).session(adminSession))
				.andExpect(status().isBadRequest());

		mockMvc.perform(delete("/api/admin/menus/" + childId).session(adminSession))
				.andExpect(status().isOk());

		mockMvc.perform(delete("/api/admin/menus/" + rootId).session(adminSession))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/admin/menus/tree-rows").param("clientType", "1").session(adminSession))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data", hasSize(0)));
	}

	/** safe 模式下仅允许查询用户可见菜单下的 op-codes；不可见菜单返回 403。 */
	@Test
	void opCodes_safeMode_forbiddenWhenMenuNotVisible() throws Exception {
		AppMenu menuA = new AppMenu();
		menuA.setName("可见菜单");
		menuA.setMenuType(Constants.MENU_TYPE_EMP);
		menuA.setClientType("1");
		menuA.setStatus("1");
		appMenuRepository.save(menuA);

		AppMenu menuB = new AppMenu();
		menuB.setName("不可见菜单");
		menuB.setMenuType(Constants.MENU_TYPE_EMP);
		menuB.setClientType("1");
		menuB.setStatus("1");
		appMenuRepository.save(menuB);

		AppOpSecurity opA = new AppOpSecurity();
		opA.setMenu(menuA);
		opA.setCode("XA");
		opA.setStatus("1");
		appOpSecurityRepository.save(opA);

		AppOpSecurity opB = new AppOpSecurity();
		opB.setMenu(menuB);
		opB.setCode("XB");
		opB.setStatus("1");
		appOpSecurityRepository.save(opB);

		AppRole role = new AppRole();
		role.setCode("one_menu_r");
		role.setName("单菜单角色");
		role.setStatus("1");
		role.setSequ(0);
		role.setIsInner("0");
		role.setIsViewAll("0");
		appRoleRepository.save(role);

		AppOpAssign assign = new AppOpAssign();
		assign.setRole(role);
		assign.setOp(opA);
		appOpAssignRepository.save(assign);

		SmUser sm = smUserRepository.findByCodeIgnoreCase("zhangsan").orElseThrow();
		sm.setUserType((short) 0);
		smUserRepository.save(sm);
		SmRoleUser ru = new SmRoleUser();
		ru.setUserId(sm.getId());
		ru.setRoleId(role.getId());
		smRoleUserRepository.save(ru);

		MockHttpSession session = loginZhangsan();
		mockMvc.perform(get("/api/permissions/menus/" + menuB.getId() + "/op-codes")
						.param("safe", "true")
						.param("clientType", "1")
						.session(session))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));

		mockMvc.perform(get("/api/permissions/menus/" + menuA.getId() + "/op-codes")
						.param("safe", "true")
						.param("clientType", "1")
						.session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.opCodes[0]").value("XA"));
	}
}
