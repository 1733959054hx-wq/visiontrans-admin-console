package com.gzu.adminconsole.dto.meta;

import java.util.List;

/**
 * 侧边导航菜单视图模型（已按当前登录管理员的角色过滤）。
 *
 * @param groups 一级页面分组（系统管理 / 审核中心 / 广告运营 / 财务订单 / 监控运维）
 * @param roleCode 当前登录管理员的角色编码，前端用于兜底校验
 */
public record NavMenuVO(List<NavGroup> groups, String roleCode) {

    /**
     * @param name  分组中文名（对应需求清单的一级页面）
     * @param sub   分组英文副标题
     * @param items 该分组下的菜单项
     */
    public record NavGroup(String name, String sub, List<NavItem> items) {
    }

    /**
     * @param id    菜单唯一标识，同时作为前端路由名
     * @param icon  FontAwesome 图标类名
     * @param text  菜单中文名
     * @param sub   英文副标题
     * @param title 页面主标题
     * @param desc  页面描述
     */
    public record NavItem(String id, String icon, String text, String sub, String title, String desc) {
    }
}
