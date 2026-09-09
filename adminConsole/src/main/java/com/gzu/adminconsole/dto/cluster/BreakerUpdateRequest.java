package com.gzu.adminconsole.dto.cluster;

/**
 * 熔断降级策略更新请求体。
 *
 * @param id      策略主键
 * @param state   目标状态：开启 / 半开 / 关闭 / 降级中
 * @param enabled 是否启用该策略
 */
public record BreakerUpdateRequest(Long id, String state, boolean enabled) {
}
