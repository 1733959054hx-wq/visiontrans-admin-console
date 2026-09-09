package com.gzu.adminconsole.dto.cluster;

import java.util.List;

import com.gzu.adminconsole.dto.common.KpiMetric;

/**
 * 集群态势感知大盘视图模型（对应页面 a6）。
 */
public record ClusterOverviewVO(List<KpiMetric> kpis,
                                HealthCard health,
                                LatencyCard latency,
                                QpsCard qps,
                                List<RegionRank> regions,
                                List<NodeRow> nodes,
                                List<AlarmRow> alarms,
                                /** 由 metric_sample 真实采样推导的实时告警（顶部消息铃铛） */
                                List<AlarmRow> alerts,
                                /** 核心服务与第三方接口可用性监控 */
                                List<DependencyVO.DependencyRow> dependencies,
                                Summary summary) {

    /** 节点健康度环形卡。 */
    public record HealthCard(double pct, String value, String label, int online, int total, int abnormal) {
    }

    /** 端到端延迟拆解。 */
    public record LatencyCard(int total, int sla, String unit, String breakdownText, List<Segment> segments,
                              WaveChart wave) {
    }

    /** 管线分段耗时。 */
    public record Segment(String name, int value, String c1, String c2) {
    }

    /** 延迟堆叠波形图。 */
    public record WaveChart(int points, int max, String unit, List<WaveLabel> labels, List<WaveLayer> layers) {
    }

    /** 波形图时间轴刻度。 */
    public record WaveLabel(int at, String t) {
    }

    /** 波形图单层数据。 */
    public record WaveLayer(String name, String c1, String c2, List<Double> samples) {
    }

    /** QPS 吞吐与容量推演。 */
    public record QpsCard(List<String> labels, List<Integer> real, List<Integer> forecast, int capacity,
                          int maxAxis, int peak) {
    }

    /** 可用区并发排行。 */
    public record RegionRank(String name, String sub, int value, double delta) {
    }

    /** 微服务容器健康度明细行。 */
    public record NodeRow(String id, String zone, String role, int containers, double cpu, double gpu,
                          String latency, String status) {
    }

    /** 实时告警与自愈事件。 */
    public record AlarmRow(Long id, String time, String level, String message, String result) {
    }

    /** 页面级汇总文案。 */
    public record Summary(String sampling, String dataDelay, String monitorLabel, String selfHealRate,
                          String selfHealCost, String containerSummary) {
    }
}
