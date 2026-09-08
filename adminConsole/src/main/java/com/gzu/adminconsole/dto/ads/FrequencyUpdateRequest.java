package com.gzu.adminconsole.dto.ads;

/**
 * 频次配置更新请求体。
 *
 * @param name  配置项名称
 * @param value 目标值
 */
public record FrequencyUpdateRequest(String name, int value) {
}
