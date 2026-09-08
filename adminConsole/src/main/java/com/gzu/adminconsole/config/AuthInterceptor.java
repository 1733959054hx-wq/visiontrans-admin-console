package com.gzu.adminconsole.config;

import java.util.Arrays;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.gzu.adminconsole.common.BusinessException;
import com.gzu.adminconsole.entity.AuthSessionEntity;
import com.gzu.adminconsole.repository.AuthRepository;

/**
 * 登录鉴权拦截器：校验 X-Auth-Token，并按 {@link RequireRole} 校验角色。
 *
 * <p>放行规则：OPTIONS 预检、登录接口、以及未被 Spring 管理的静态资源。</p>
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /** 令牌请求头。 */
    public static final String TOKEN_HEADER = "X-Auth-Token";

    private final AuthRepository authRepository;

    public AuthInterceptor(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod()) || !(handler instanceof HandlerMethod method)) {
            return true;
        }

        // 登录相关接口（登录 / 验证码 / 公钥下发）不需要令牌
        String uri = request.getRequestURI();
        if (uri.endsWith("/auth/login") || uri.endsWith("/auth/captcha") || uri.endsWith("/auth/public-key")) {
            return true;
        }

        // 其余所有接口必须携带有效令牌（即：未登录无法访问任何后台接口）
        String token = request.getHeader(TOKEN_HEADER);
        AuthSessionEntity session = authRepository.findValidSession(token);
        if (session == null) {
            throw new BusinessException(401, "未登录或登录已过期，请重新登录");
        }
        AuthRepository.bind(session);

        RequireRole classRule = method.getBeanType().getAnnotation(RequireRole.class);
        RequireRole methodRule = method.getMethodAnnotation(RequireRole.class);
        RequireRole rule = methodRule != null ? methodRule : classRule;
        if (rule != null && rule.value().length > 0
                && !Arrays.asList(rule.value()).contains(session.getRoleCode())) {
            throw new BusinessException(403, "当前角色无此操作权限：" + session.getRoleName());
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        AdminContext.clear();
    }
}
