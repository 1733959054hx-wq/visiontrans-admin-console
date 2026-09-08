package com.gzu.adminconsole.jingchen.common;

/**
 * 商户模块常量（模块自有，不引用主工程 RBAC 常量）。
 *
 * <p>角色编码同时需在主工程 {@code AuthRepository#roleCodeOf} 中登记中文名映射，
 * 两处保持一致；如需变更请同步修改。
 */
public final class MerchantConstants {

    /** 角色编码（用于 {@code @RequireRole} 与前端路由分发）。 */
    public static final String ROLE_CODE = "MERCHANT";

    /** 角色中文名（展示用）。 */
    public static final String ROLE_NAME = "商户用户";

    /** 商户模块接口路径前缀。 */
    public static final String API_PREFIX = "/merchant";

    private MerchantConstants() {
    }
}
