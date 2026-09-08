package com.gzu.adminconsole.dto.meta;

import java.util.List;

/**
 * 侧边导航菜单视图模型。
 *
 * @param group 菜单分组标题
 * @param items 菜单项
 */
public record NavMenuVO(String group, List<NavItem> items) {

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
