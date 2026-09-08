package com.gzu.adminconsole.model;

import java.util.List;

/**
 * RBAC 角色域。
 *
 * @param name    角色显示名
 * @param code    角色编码
 * @param icon    FontAwesome 图标
 * @param members 成员数
 * @param groups  权限组
 */
public record RoleDomain(String name, String code, String icon, int members, List<PermGroup> groups) {
}
