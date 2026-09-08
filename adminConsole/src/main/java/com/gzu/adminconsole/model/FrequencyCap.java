package com.gzu.adminconsole.model;

/**
 * 单用户跨广告位联合频控项。
 *
 * @param name    配置名称
 * @param display 展示文案
 * @param max     滑块最大值
 * @param value   当前值
 */
public record FrequencyCap(String name, String display, int max, int value) {

    /** 复制一份并替换当前值。 */
    public FrequencyCap withValue(int newValue) {
        return new FrequencyCap(name, display, max, newValue);
    }
}
