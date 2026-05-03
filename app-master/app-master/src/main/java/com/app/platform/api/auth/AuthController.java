package com.app.platform.api.auth;

import com.app.platform.api.dto.ApiSuccessBody;
import com.app.platform.api.dto.LoginRequest;
import com.app.platform.api.dto.LogoutResponse;
import com.app.platform.api.dto.RegisterRequest;
import com.app.platform.api.dto.UserSessionDto;
import com.app.platform.core.authentication.UserManager;
import com.app.platform.core.authentication.intf.IUser;
import com.app.platform.exception.UnauthorizedException;
import com.app.platform.service.LoginService;
import com.app.platform.service.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 认证相关 REST：注册、登录、登出与当前用户查询。 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final LoginService loginService;
	private final RegistrationService registrationService;

	/** 注入登录与注册业务服务。 */
	public AuthController(LoginService loginService, RegistrationService registrationService) {
		this.loginService = loginService;
		this.registrationService = registrationService;
	}

	/** 新用户注册，成功则返回会话信息（通常已等价于登录态）。 */
	@PostMapping("/register")
	public ResponseEntity<ApiSuccessBody<UserSessionDto>> register(@Valid @RequestBody RegisterRequest request) {
		UserSessionDto data = registrationService.register(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiSuccessBody.of(data));
	}

	/** 用户名密码登录，服务端建立 Session。 */
	@PostMapping("/login")
	public ResponseEntity<ApiSuccessBody<UserSessionDto>> login(@Valid @RequestBody LoginRequest request,
			HttpServletRequest httpRequest) {
		UserSessionDto data = loginService.login(request, httpRequest);
		return ResponseEntity.ok(ApiSuccessBody.of(data));
	}

	/** 销毁服务端 Session，客户端需丢弃 Cookie。 */
	@PostMapping("/logout")
	public ResponseEntity<LogoutResponse> logout(HttpServletRequest httpRequest) {
		loginService.logout(httpRequest);
		return ResponseEntity.ok(LogoutResponse.ok());
	}

	/** 返回当前登录用户摘要；未登录返回 401。 */
	@GetMapping("/me")
	public ResponseEntity<ApiSuccessBody<UserSessionDto>> me() {
		IUser u = UserManager.getLoginUser();
		if (UserManager.isAnonymous()) {
			throw new UnauthorizedException();
		}
		UserSessionDto dto = new UserSessionDto(u.getLoginUserId(), u.getUserCode(), u.getDisplayName());
		return ResponseEntity.ok(ApiSuccessBody.of(dto));
	}
}
