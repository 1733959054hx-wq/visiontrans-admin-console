package com.gzu.adminconsole.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记接口所需角色，由 {@link AuthInterceptor} 校验。
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {

    /** 允许访问的角色编码（SUPER_ADMIN / OPERATIONS / AUDITOR），为空表示仅需登录。 */
    String[] value() default {};
}
