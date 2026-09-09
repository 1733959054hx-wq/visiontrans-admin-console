package com.gzu.adminconsole.dto.security;

import java.util.List;

import com.gzu.adminconsole.dto.common.ToggleItem;

/**
 * 系统配置视图模型：运行参数 + 功能开关。
 */
public record SysConfigVO(List<ParamItem> params, List<ToggleItem> features) {

    /** 运行参数项。 */
    public record ParamItem(String name, String label, String value, String unit) {
    }
}
