package com.gzu.adminconsole.dto.cluster;

/**
 * 备份策略更新请求体。
 *
 * @param id      策略主键
 * @param enabled 是否启用该策略
 */
public record BackupUpdateRequest(Long id, boolean enabled) {
}
