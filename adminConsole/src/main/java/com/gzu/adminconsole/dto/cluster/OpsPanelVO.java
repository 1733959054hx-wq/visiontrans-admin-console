package com.gzu.adminconsole.dto.cluster;

import java.util.List;

/**
 * 监控运维面板视图模型（熔断降级 / 备份策略 / 告警阈值，对应集群页扩展）。
 */
public record OpsPanelVO(List<BreakerRow> breakers,
                         List<BackupRow> backups,
                         Thresholds thresholds) {

    /** 熔断降级策略行。 */
    public record BreakerRow(Long id,
                             String service,
                             String strategy,
                             String threshold,
                             String state,
                             boolean enabled) {
    }

    /** 备份策略行。 */
    public record BackupRow(Long id,
                            String target,
                            String cycle,
                            String retention,
                            String storage,
                            boolean enabled) {
    }

    /** 告警阈值（CPU / 内存 / GPU 利用率 %）。 */
    public record Thresholds(int cpu, int mem, int gpu) {
    }
}
