package com.app.platform.api.error;

import com.app.platform.api.dto.ApiErrorBody;
import com.app.platform.exception.BadRequestException;
import com.app.platform.exception.AuthFailedException;
import com.app.platform.exception.ForbiddenException;
import com.app.platform.exception.InvalidRoleException;
import com.app.platform.exception.MenuNotFoundException;
import com.app.platform.exception.RoleNotFoundException;
import com.app.platform.exception.UnauthorizedException;
import com.app.platform.exception.UserNotFoundException;
import com.app.platform.exception.UsernameTakenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 全局异常到统一 JSON 错误体的映射，保证 API 响应结构一致。 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	/** 登录凭证错误或业务拒绝登录。 */
	@ExceptionHandler(AuthFailedException.class)
	public ResponseEntity<ApiErrorBody> authFailed(AuthFailedException ex) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiErrorBody.of("AUTH_FAILED", ex.getMessage()));
	}

	/** 未登录或会话失效访问受保护资源。 */
	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<ApiErrorBody> unauthorized(UnauthorizedException ex) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiErrorBody.of("UNAUTHORIZED", ex.getMessage()));
	}

	/** 注册/改名时用户名已被占用。 */
	@ExceptionHandler(UsernameTakenException.class)
	public ResponseEntity<ApiErrorBody> usernameTaken(UsernameTakenException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ApiErrorBody.of("USERNAME_TAKEN", ex.getMessage()));
	}

	/** Bean Validation 校验失败（如 @NotBlank）。 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorBody> validation(MethodArgumentNotValidException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiErrorBody.of("VALIDATION_ERROR", "请求参数无效"));
	}

	/** 已认证但无权限执行该操作。 */
	@ExceptionHandler(ForbiddenException.class)
	public ResponseEntity<ApiErrorBody> forbidden(ForbiddenException ex) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(ApiErrorBody.of("FORBIDDEN", ex.getMessage()));
	}

	/** 菜单 ID 不存在。 */
	@ExceptionHandler(MenuNotFoundException.class)
	public ResponseEntity<ApiErrorBody> menuNotFound(MenuNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiErrorBody.of("MENU_NOT_FOUND", ex.getMessage()));
	}

	/** 角色 ID 不存在。 */
	@ExceptionHandler(RoleNotFoundException.class)
	public ResponseEntity<ApiErrorBody> roleNotFound(RoleNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiErrorBody.of("ROLE_NOT_FOUND", ex.getMessage()));
	}

	/** 用户 ID 不存在。 */
	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<ApiErrorBody> userNotFound(UserNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiErrorBody.of("USER_NOT_FOUND", ex.getMessage()));
	}

	/** 角色数据不合法（如互斥约束）。 */
	@ExceptionHandler(InvalidRoleException.class)
	public ResponseEntity<ApiErrorBody> invalidRole(InvalidRoleException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiErrorBody.of("INVALID_ROLE", ex.getMessage()));
	}

	/** 通用 400：业务规则不满足。 */
	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ApiErrorBody> badRequest(BadRequestException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiErrorBody.of("BAD_REQUEST", ex.getMessage()));
	}
}
