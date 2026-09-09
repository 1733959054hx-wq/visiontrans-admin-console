package com.gzu.adminconsole.dto.cluster;

import java.util.List;

/**
 * 核心服务与第三方接口可用性监控视图模型。
 *
 * @param rows       依赖服务明细
 * @param probeMode  auto（定时拨测已开启）/ manual（仅手动拨测）
 * @param updatedAt  最近一次拨测时间
 */
public record DependencyVO(List<DependencyRow> rows, String probeMode, String updatedAt) {

    /** 单个依赖服务的可用性与响应耗时。 */
    public record DependencyRow(Long id,
                                String name,
                                String category,
                                String endpoint,
                                int timeoutMs,
                                String status,
                                String tone,
                                int latencyMs,
                                double successRate,
                                String lastCheck,
                                String remark) {
    }
}
