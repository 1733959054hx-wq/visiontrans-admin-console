package com.gzu.adminconsole.dto.security;

import java.util.List;

/**
 * 菜单权限配置视图模型：每条菜单 × 每个角色的可见性矩阵。
 *
 * @param roles 后台管理角色列表（编码 + 中文名）
 * @param menus 菜单可见性行
 */
public record MenuAccessVO(List<RoleOption> roles, List<MenuRow> menus) {

    /** 后台管理角色选项。 */
    public record RoleOption(String code, String name) {
    }

    /**
     * @param id       菜单唯一标识（同时是前端路由名）
     * @param text     菜单中文名
     * @param group    所属一级页面分组
     * @param roles    各角色对该菜单的可见性
     * @param sortOrder 展示顺序
     */
    public record MenuRow(String id, String text, String group, List<RoleVisible> roles, int sortOrder) {
    }

    /** 某角色对某菜单是否可见。 */
    public record RoleVisible(String code, boolean visible) {
    }
}
