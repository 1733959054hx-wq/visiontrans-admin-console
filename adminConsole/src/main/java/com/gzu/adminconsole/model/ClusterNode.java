package com.gzu.adminconsole.model;

/**
 * 集群微服务容器节点（Model 层领域实体）。
 *
 * @param id         节点 ID
 * @param zone       可用区
 * @param role       服务角色
 * @param containers 容器数量
 * @param cpu        CPU 利用率（百分比）
 * @param gpu        GPU / 显存利用率（百分比）
 * @param latencyMs  平均延迟（毫秒）
 * @param status     节点状态：健康 / 高负载
 */
public record ClusterNode(String id,
                          String zone,
                          String role,
                          int containers,
                          double cpu,
                          double gpu,
                          int latencyMs,
                          String status) {
}
