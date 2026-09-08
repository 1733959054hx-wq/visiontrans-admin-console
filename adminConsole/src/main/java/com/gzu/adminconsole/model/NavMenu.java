package com.gzu.adminconsole.model;

/**
 * 侧边导航菜单项配置（Model 层实体）。
 *
 * @param id    菜单唯一标识，与前端路由名一致
 * @param icon  FontAwesome 图标类名
 * @param text  菜单中文名
 * @param sub   英文副标题
 * @param title 页面主标题
 * @param desc  页面描述
 */
public record NavMenu(String id, String icon, String text, String sub, String title, String desc) {
}
