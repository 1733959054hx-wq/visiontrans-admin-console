package com.gzu.adminconsole.dto.common;

/**
 * 开关型配置项视图模型（策略开关、功能开关等）。
 *
 * @param name    配置名称
 * @param enabled 是否开启
 */
public record ToggleItem(String name, boolean enabled) {
}
