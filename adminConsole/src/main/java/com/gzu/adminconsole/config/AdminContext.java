package com.gzu.adminconsole.config;

/**
 * 当前登录管理员上下文（线程隔离，由 AuthInterceptor 写入、请求结束清理）。
 */
public final class AdminContext {

    private static final ThreadLocal<CurrentAdmin> HOLDER = new ThreadLocal<>();

    private AdminContext() {
    }

    /** 当前登录管理员。 */
    public record CurrentAdmin(String token, String username, String name, String roleCode, String roleName,
                               String groupName) {
    }

    public static void set(CurrentAdmin admin) {
        HOLDER.set(admin);
    }

    public static CurrentAdmin get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
