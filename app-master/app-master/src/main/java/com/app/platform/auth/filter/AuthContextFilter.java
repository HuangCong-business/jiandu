package com.app.platform.auth.filter;

import com.app.platform.core.authentication.Constants;
import com.app.platform.core.authentication.ThreadLocalManager;
import com.app.platform.core.authentication.intf.IUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 将 Session 中的 {@link IUser} 绑定到当前线程；请求结束清理 ThreadLocal。
 */
public class AuthContextFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		try {
			HttpSession session = request.getSession(false);
			if (session != null) {
				Object raw = session.getAttribute(Constants.SESSION_USER);
				// 仅接受 IUser 实现，忽略异常 Session 属性
				if (raw instanceof IUser iu) {
					ThreadLocalManager.setUserLocal(iu);
				}
			}
			filterChain.doFilter(request, response);
		}
		finally {
			// 与线程池配合：必须释放 ThreadLocal
			ThreadLocalManager.clearUserLocal();
		}
	}
}
