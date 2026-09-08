package com.gzu.adminconsole.model;

import java.util.List;

/**
 * 权限组。
 *
 * @param name        权限组名称
 * @param members     组内成员数
 * @param permissions 细粒度权限项
 */
public record PermGroup(String name, int members, List<Permission> permissions) {
}
