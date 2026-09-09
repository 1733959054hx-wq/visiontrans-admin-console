package com.gzu.adminconsole.model;

/**
 * 侧边导航菜单项配置（Model 层实体）。
 *
 * @param id        菜单唯一标识，与前端路由名一致
 * @param icon      FontAwesome 图标类名
 * @param text      菜单中文名
 * @param sub       英文副标题
 * @param title     页面主标题
 * @param desc      页面描述
 * @param groupName 一级页面分组名（系统管理 / 审核中心 / 广告运营 / 财务订单 / 监控运维）
 * @param groupSub  分组英文副标题
 * @param roles     可见角色编码，逗号分隔；空表示对所有后台管理角色开放
 */
public record NavMenu(String id, String icon, String text, String sub, String title, String desc,
                      String groupName, String groupSub, String roles) {

    /** 默认分组（兼容未指定分组的历史数据）。 */
    public static final String DEFAULT_GROUP = "平台治理";

    /** 所有后台管理角色（顺序与 RBAC 角色域一致）。 */
    public static final String ALL_ROLES = "SUPER_ADMIN,OPERATIONS,AUDITOR";

    /** 便捷构造：分组与角色取默认值。 */
    public NavMenu(String id, String icon, String text, String sub, String title, String desc) {
        this(id, icon, text, sub, title, desc, DEFAULT_GROUP, "", ALL_ROLES);
    }

    /** 该菜单对指定角色是否可见（roles 为空表示不限制）。 */
    public boolean visibleTo(String roleCode) {
        if (roles == null || roles.isBlank() || roleCode == null || roleCode.isBlank()) {
            return true;
        }
        for (String role : roles.split(",")) {
            if (role.trim().equalsIgnoreCase(roleCode.trim())) {
                return true;
            }
        }
        return false;
    }

    /** 复制一份并替换可见角色列表。 */
    public NavMenu withRoles(String newRoles) {
        return new NavMenu(id, icon, text, sub, title, desc, groupName, groupSub, newRoles);
    }
}
