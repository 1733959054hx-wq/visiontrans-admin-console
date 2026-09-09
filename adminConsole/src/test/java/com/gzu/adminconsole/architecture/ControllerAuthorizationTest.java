package com.gzu.adminconsole.architecture;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RestController;

import com.gzu.adminconsole.config.RequireRole;

/**
 * RBAC 架构守护测试：防止「新增 controller 忘记声明角色」或「角色编码拼错」悄悄带入生产。
 *
 * <p>为什么需要这两条断言：
 * <ul>
 *   <li>{@code AuthInterceptor} 只对<b>写操作</b>做「未标注 {@code @RequireRole} 即拒绝」的兜底，
 *       <b>GET 不会被兜底</b>。一旦 controller 漏加类级注解，所有 GET 接口就对任何已登录身份开放
 *       （历史上的 {@code MetaController} 就是这样让商户读到了整套后台导航结构）。</li>
 *   <li>角色编码是字符串常量，拼错（如 {@code "SUPERADMIN"}）不会编译报错，
 *       但会永久拒绝所有真实用户，且很难被发现。</li>
 * </ul>
 *
 * <p>刻意不启动 Spring 上下文：纯反射扫描，秒级完成，不依赖 MySQL 与演示数据。
 */
class ControllerAuthorizationTest {

    /** 扫描范围：整个主包（含各业务模块），新增模块自动纳入保障。 */
    private static final String BASE_PACKAGE = "com.gzu.adminconsole";

    /** 系统已知角色编码（与 AuthRepository#roleCodeOf 及角色域保持一致）。 */
    private static final Set<String> KNOWN_ROLES = Set.of("SUPER_ADMIN", "OPERATIONS", "AUDITOR", "MERCHANT");

    /**
     * 允许不带类级角色的控制器白名单。
     *
     * <p>{@code AuthController} 本身就包含完全公开的接口（登录 / 验证码 / 公钥下发），
     * 因此采用逐方法声明的方式而非类级统一声明，属于设计上的例外。
     */
    private static final Set<String> EXEMPT_CONTROLLERS = Set.of("com.gzu.adminconsole.controller.AuthController");

    /** 除白名单外，每个 @RestController 都必须声明类级 @RequireRole 且角色列表非空。 */
    @Test
    void everyRestControllerDeclaresClassLevelRoles() {
        List<String> offenders = new ArrayList<>();
        for (Class<?> controller : restControllers()) {
            if (EXEMPT_CONTROLLERS.contains(controller.getName())) {
                continue;
            }
            RequireRole rule = controller.getAnnotation(RequireRole.class);
            if (rule == null || rule.value().length == 0) {
                offenders.add(controller.getName());
            }
        }
        org.junit.jupiter.api.Assertions.assertTrue(offenders.isEmpty(),
                () -> "以下 controller 缺少类级 @RequireRole，其 GET 接口将对任何已登录身份开放（含商户）：" + offenders);
    }

    /** 所有 @RequireRole 的角色编码必须是系统已知值，防止拼写错误导致接口永久不可达。 */
    @Test
    void requireRoleValuesAreKnown() {
        List<String> offenders = new ArrayList<>();
        for (Class<?> controller : restControllers()) {
            offenders.addAll(unknownRoles(controller.getAnnotation(RequireRole.class), controller.getName()));
            for (Method method : controller.getDeclaredMethods()) {
                RequireRole rule = method.getAnnotation(RequireRole.class);
                offenders.addAll(unknownRoles(rule, controller.getName() + "#" + method.getName()));
            }
        }
        org.junit.jupiter.api.Assertions.assertTrue(offenders.isEmpty(),
                () -> "以下 @RequireRole 使用了未知角色编码，会导致该接口对所有角色都不可达：" + offenders
                        + "，已知角色：" + KNOWN_ROLES);
    }

    /** 扫描全部 @RestController（未实例化、未加载 Bean，仅读取类元数据 + 反射注解）。 */
    private List<Class<?>> restControllers() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        List<Class<?>> controllers = new ArrayList<>();
        for (var definition : scanner.findCandidateComponents(BASE_PACKAGE)) {
            String className = definition.getBeanClassName();
            if (className == null) {
                continue;
            }
            try {
                controllers.add(ClassUtils.forName(className, getClass().getClassLoader()));
            } catch (ClassNotFoundException ex) {
                throw new IllegalStateException("扫描到的 controller 类无法加载：" + className, ex);
            }
        }
        return controllers;
    }

    /** 收集单个注解里所有未知角色编码，返回「定义位置=未知编码」列表。 */
    private List<String> unknownRoles(RequireRole rule, String where) {
        if (rule == null || rule.value().length == 0) {
            // 空值等价于「任何已登录角色」，是合法用法（如登出），不做校验
            return List.of();
        }
        List<String> offenders = new ArrayList<>();
        for (String role : rule.value()) {
            if (!KNOWN_ROLES.contains(role)) {
                offenders.add(where + "=" + role);
            }
        }
        return offenders;
    }
}
