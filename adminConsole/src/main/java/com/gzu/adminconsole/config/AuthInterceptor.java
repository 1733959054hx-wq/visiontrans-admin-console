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
        // 入口先清空：preHandle 抛异常时 Spring 不会回调 afterCompletion，
        // 不清会让上一个请求的身份残留在 Tomcat 复用线程上
        AdminContext.clear();
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
        // 最小权限兜底：写操作必须显式声明 @RequireRole，未声明视为配置遗漏直接拒绝，
        // 防止新增接口忘记标注角色而被任意已登录用户（含只读审计员）调用
        if (rule == null && isWriteMethod(request.getMethod())) {
            throw new BusinessException(403, "该操作未配置角色策略，已按最小权限拒绝");
        }
        return true;
    }

    /** 是否为会改变数据的写方法（GET / HEAD / OPTIONS 视为只读）。 */
    private static boolean isWriteMethod(String method) {
        if (method == null) {
            return false;
        }
        return switch (method.toUpperCase(java.util.Locale.ROOT)) {
            case "POST", "PUT", "PATCH", "DELETE" -> true;
            default -> false;
        };
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        AdminContext.clear();
    }
}
