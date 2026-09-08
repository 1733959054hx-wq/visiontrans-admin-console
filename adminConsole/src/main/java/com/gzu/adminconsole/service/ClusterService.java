package com.gzu.adminconsole.service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.gzu.adminconsole.common.BusinessException;
import com.gzu.adminconsole.common.DateRange;
import com.gzu.adminconsole.common.TrendUtils;
import com.gzu.adminconsole.config.AppProperties;
import com.gzu.adminconsole.dto.cluster.ClusterOverviewVO;
import com.gzu.adminconsole.dto.common.KpiMetric;
import com.gzu.adminconsole.dto.meta.ActionResultVO;
import com.gzu.adminconsole.entity.MetricSampleEntity;
import com.gzu.adminconsole.model.AlarmEvent;
import com.gzu.adminconsole.model.ClusterNode;
import com.gzu.adminconsole.repository.ClusterRepository;
import com.gzu.adminconsole.repository.MetricRepository;

/**
 * 集群态势感知 ViewModel 层：把 Model 层原始数据组装成页面可直接消费的视图模型。
 */
@Service
public class ClusterService {

    // 会话 / QPS 折算系数、延迟基准与健康度阈值统一由 admin-console.cluster.* 配置提供；
    // 波形、QPS 曲线与趋势迷你图一律由 metric_sample 表中的真实采样数据统计得出。
    private final ClusterRepository repository;
    private final AppProperties properties;
    private final MetricRepository metrics;

    public ClusterService(ClusterRepository repository, AppProperties properties, MetricRepository metrics) {
        this.repository = repository;
        this.properties = properties;
        this.metrics = metrics;
    }

    /** 集群态势感知大盘视图模型（可按告警时间范围过滤，yyyy-MM-dd）。 */
    public ClusterOverviewVO overview(String start, String end) {
        AppProperties.Cluster cfg = properties.getCluster();
        List<ClusterNode> nodes = repository.findNodes();
        boolean randomize = properties.getData().isRandomize();

        // 容器总数、节点平均延迟、会话数与吞吐量均由节点台账实时推导（增删节点后 KPI 同步变化）
        int totalContainers = nodes.stream().mapToInt(ClusterNode::containers).sum();
        int abnormalNodes = (int) nodes.stream().filter(n -> !"健康".equals(n.status())).count();
        int abnormalContainers = nodes.stream()
                .filter(n -> !"健康".equals(n.status()))
                .mapToInt(ClusterNode::containers)
                .sum();
        int sessions = totalContainers * cfg.getSessionsPerContainer();
        double avgLatency = nodes.isEmpty()
                ? cfg.getBaseLatencyMs()
                : Math.round(nodes.stream().mapToInt(ClusterNode::latencyMs).average().orElse(cfg.getBaseLatencyMs()));
        int qps = totalContainers * cfg.getQpsPerContainer();

        // 趋势与环比均由 metric_sample 中按天聚合的真实采样计算，避免写死曲线与百分比
        List<Double> sessionTrend = dailyTrend(MetricRepository.MetricKey.CLUSTER_QPS_HOURLY, start, end, sessions);
        List<Double> latencyTrend = dailyTrend(MetricRepository.MetricKey.LATENCY_SEGMENT, start, end, avgLatency);
        List<Double> qpsTrend = dailyTrend(MetricRepository.MetricKey.CLUSTER_QPS_HOURLY, start, end, qps);

        List<KpiMetric> kpis = List.of(
                new KpiMetric("全网并发会话数", String.format("%,d", sessions), null, "fa-users-rays",
                        "#1E3A8A", "#0EA5E9",
                        deltaOf(sessionTrend), rising(sessionTrend), "green",
                        "由 " + totalContainers + " 个容器按 " + cfg.getSessionsPerContainer() + " 会话/容器折算",
                        sessionTrend),
                new KpiMetric("端到端平均延迟", String.valueOf((long) avgLatency), " ms", "fa-stopwatch",
                        "#2563EB", "#0EA5E9", deltaOf(latencyTrend), rising(latencyTrend), "green",
                        "SLA 目标 ≤ " + properties.getCluster().getSlaLatencyMs() + " ms · 取 "
                                + nodes.size() + " 个节点均值",
                        latencyTrend),
                new KpiMetric("核心 API 吞吐量（QPS）", String.format("%,d", qps), null, "fa-bolt",
                        "#0B1E4D", "#2563EB",
                        deltaOf(qpsTrend), rising(qpsTrend), "green",
                        "设计容量 " + String.format("%,d", properties.getCluster().getDesignCapacityQps())
                                + " QPS（≥500 QPS 达标）",
                        qpsTrend));

        // TODO(演示数据)：节点健康度 / 采样可用率 / 数据时延为 SLA 汇总口径（非实时统计），
        //  接入 Prometheus / 监控系统后由指标数据推导。
        ClusterOverviewVO.HealthCard health = new ClusterOverviewVO.HealthCard(
                cfg.getHealthPct(), cfg.getHealthPct() + "%", "节点健康度",
                totalContainers - abnormalContainers, totalContainers, abnormalNodes);

        // 采样可用率与数据时延同样取自真实采样（范围内均值）
        double availability = metrics.avg(MetricRepository.MetricKey.CLUSTER_AVAILABILITY, start, end);
        double dataDelaySeconds = metrics.avg(MetricRepository.MetricKey.CLUSTER_DATA_DELAY_SECONDS, start, end);

        ClusterOverviewVO.Summary summary = new ClusterOverviewVO.Summary(
                properties.getCluster().getSamplingPeriod(),
                properties.getCluster().getDataDelay(),
                "实时监控中",
                String.format("%.1f%%", availability),
                String.format("%.1fs", dataDelaySeconds),
                "双可用区 " + nodes.size() + " 个服务组 / " + totalContainers + " 个容器 · 实时采样周期 "
                        + properties.getCluster().getSamplingPeriod());

        List<AlarmEvent> alarms = repository.findAlarms().stream()
                .filter(a -> DateRange.inRange(a.time(), start, end))
                .toList();
        double regionDelta = qpsTrend.size() >= 2
                ? (qpsTrend.get(qpsTrend.size() - 1) - qpsTrend.get(0)) * 100.0 / Math.max(1, qpsTrend.get(0))
                : 0;
        return new ClusterOverviewVO(kpis, health, latencyCard(start, end), qpsCard(start, end),
                regions(nodes, Math.round(regionDelta * 10.0) / 10.0),
                nodeRows(nodes, randomize), alarmRows(alarms), derivedAlerts(start, end), summary);
    }

    /* ------------------------------ 容器节点 ------------------------------ */

    /** 新增节点。 */
    public ActionResultVO createNode(ClusterNode node) {
        requireText(node.id(), "节点 ID");
        if (repository.findNode(node.id()) != null) {
            throw new BusinessException("节点已存在：" + node.id());
        }
        repository.insertNode(node);
        return ActionResultVO.ok("节点 " + node.id() + " 已新增", node.id());
    }

    /** 更新节点。 */
    public ActionResultVO updateNode(ClusterNode node) {
        requireText(node.id(), "节点 ID");
        if (repository.findNode(node.id()) == null) {
            throw new BusinessException("未找到节点：" + node.id());
        }
        repository.updateNode(node);
        return ActionResultVO.ok("节点 " + node.id() + " 已更新", node.id());
    }

    /** 删除节点。 */
    public ActionResultVO deleteNode(String id) {
        repository.deleteNode(id);
        return ActionResultVO.ok("节点 " + id + " 已删除", id);
    }

    /* ------------------------------ 告警事件 ------------------------------ */

    /** 新增告警（纯时间自动补当天日期，便于参与时间范围过滤）。 */
    public ActionResultVO createAlarm(AlarmEvent alarm) {
        requireText(alarm.message(), "告警内容");
        String time = DateRange.withDate(alarm.time());
        repository.insertAlarm(new AlarmEvent(alarm.id(), time, alarm.level(), alarm.message(), alarm.result()));
        return ActionResultVO.ok("告警已登记", alarm.level());
    }

    /** 删除告警。 */
    public ActionResultVO deleteAlarm(Long id) {
        repository.deleteAlarm(id);
        return ActionResultVO.ok("告警 #" + id + " 已删除", String.valueOf(id));
    }

    /* ------------------------------ 容量推演 ------------------------------ */

    /**
     * 容量推演：基于近 24 小时真实吞吐重算未来推演曲线，并返回预测峰值。
     * 接入真实预测服务（LSTM / Prophet）时，只需替换本方法内部实现。
     */
    public ActionResultVO simulateCapacity() {
        // 容量推演基于全部历史采样（不限日期范围）
        ClusterOverviewVO.QpsCard qps = qpsCard(null, null);
        int peak = qps.peak();
        int capacity = qps.capacity();
        double usage = peak * 100.0 / Math.max(1, capacity);
        String advice = usage >= 85 ? "容量水位 " + String.format("%.1f", usage) + "%，建议立即扩容"
                : usage >= 60 ? "容量水位 " + String.format("%.1f", usage) + "%，建议观察并预留 20% 余量"
                : "容量水位 " + String.format("%.1f", usage) + "%，水位健康";
        return ActionResultVO.ok("推演完成：峰值 " + peak + " QPS / 设计容量 " + capacity + " QPS · " + advice,
                String.valueOf(peak));
    }

    private void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(label + "不能为空");
        }
    }

    /**
     * 端到端延迟拆解：分段耗时与堆叠波形均取自 metric_sample 中**范围最新一天**的真实采样，
     * 因此选择不同日期范围会看到不同的曲线形态。
     */
    private ClusterOverviewVO.LatencyCard latencyCard(String start, String end) {
        List<ClusterOverviewVO.Segment> segments = metrics.latestDay(MetricRepository.MetricKey.LATENCY_SEGMENT,
                        start, end).stream()
                .map(s -> new ClusterOverviewVO.Segment(s.getBucket(), (int) Math.round(s.getValue()),
                        s.getC1(), s.getC2()))
                .toList();

        StringBuilder text = new StringBuilder();
        for (int i = 0; i < segments.size(); i++) {
            ClusterOverviewVO.Segment s = segments.get(i);
            if (i > 0) {
                text.append(" · ");
            }
            text.append(s.name()).append(' ').append(s.value()).append(" ms");
        }

        // 波形按层分组，保持采样表中的层顺序
        Map<String, List<MetricSampleEntity>> byLayer = metrics
                .latestDay(MetricRepository.MetricKey.LATENCY_WAVE, start, end).stream()
                .collect(Collectors.groupingBy(MetricSampleEntity::getBucket, LinkedHashMap::new,
                        Collectors.toList()));
        List<ClusterOverviewVO.WaveLayer> layers = new ArrayList<>();
        byLayer.forEach((name, points) -> {
            String c1 = points.isEmpty() ? "#2563EB" : points.get(0).getC1();
            String c2 = points.isEmpty() ? "#60A5FA" : points.get(0).getC2();
            layers.add(new ClusterOverviewVO.WaveLayer(name, c1, c2,
                    points.stream().map(MetricSampleEntity::getValue).toList()));
        });
        int points = layers.isEmpty() ? 0 : layers.get(0).samples().size();
        // Y 轴上限按堆叠后的实际峰值计算
        double peak = 0;
        for (int i = 0; i < points; i++) {
            double sum = 0;
            for (ClusterOverviewVO.WaveLayer layer : layers) {
                sum += layer.samples().get(i);
            }
            peak = Math.max(peak, sum);
        }
        int maxAxis = (int) (Math.ceil(Math.max(peak, 1) * 1.08 / 10) * 10);
        int total = segments.stream().mapToInt(ClusterOverviewVO.Segment::value).sum();

        return new ClusterOverviewVO.LatencyCard(
                total > 0 ? total : properties.getCluster().getBaseLatencyMs(),
                properties.getCluster().getSlaLatencyMs(),
                "ms", text.toString(), segments,
                new ClusterOverviewVO.WaveChart(points, maxAxis, "ms", waveLabels(points), layers));
    }

    /** 波形时间轴刻度：以当前时刻往前推 N 分钟（N = 采样点数），均匀取 5 个刻度。 */
    private static List<ClusterOverviewVO.WaveLabel> waveLabels(int points) {
        List<ClusterOverviewVO.WaveLabel> labels = new ArrayList<>();
        if (points <= 0) {
            return labels;
        }
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);
        for (int i = 0; i < 5; i++) {
            int at = (points - 1) * i / 4;
            labels.add(new ClusterOverviewVO.WaveLabel(at,
                    now.minusMinutes(points - 1 - at).toString().substring(0, 5)));
        }
        return labels;
    }

    /** QPS 吞吐：按小时取所选范围内的真实采样均值（多天时取平均，单日即当天曲线）。 */
    private ClusterOverviewVO.QpsCard qpsCard(String start, String end) {
        Map<String, Double> byHour = metrics.avgByBucket(MetricRepository.MetricKey.CLUSTER_QPS_HOURLY, start, end);
        List<String> labels = new ArrayList<>();
        List<Integer> real = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            String hour = String.format("%02d", h);
            labels.add(hour + ":00");
            real.add((int) Math.round(byHour.getOrDefault(hour, 0.0)));
        }
        // 预测段：以 20 点为锚按固定衰减率外推（接入预测服务后替换）
        List<Integer> forecast = new ArrayList<>(real);
        if (real.size() == 24) {
            int anchor = real.get(20);
            for (int i = 21; i < 24; i++) {
                forecast.set(i, (int) Math.round(anchor * (1 - (i - 20) * 0.11)));
            }
        }
        int capacity = properties.getCluster().getDesignCapacityQps();
        int peak = real.stream().mapToInt(Integer::intValue).max().orElse(0);
        return new ClusterOverviewVO.QpsCard(labels, real, forecast, capacity,
                Math.max(capacity, peak) + 100, peak);
    }

    /** 可用区并发排行：由节点台账按可用区聚合容器数推导，delta 取整体 QPS 环比。 */
    private List<ClusterOverviewVO.RegionRank> regions(List<ClusterNode> nodes, double delta) {
        int perContainer = properties.getCluster().getSessionsPerContainer();
        Map<String, List<ClusterNode>> byZone = nodes.stream()
                .collect(Collectors.groupingBy(ClusterNode::zone, LinkedHashMap::new, Collectors.toList()));
        List<ClusterOverviewVO.RegionRank> rows = new ArrayList<>();
        byZone.forEach((zone, list) -> {
            int containers = list.stream().mapToInt(ClusterNode::containers).sum();
            boolean healthy = list.stream().allMatch(n -> "健康".equals(n.status()));
            rows.add(new ClusterOverviewVO.RegionRank(zone,
                    containers + " 个容器 · " + (healthy ? "全部健康" : "存在高负载"),
                    containers * perContainer, delta));
        });
        rows.sort(Comparator.comparingInt(ClusterOverviewVO.RegionRank::value).reversed());
        return rows;
    }

    private List<ClusterOverviewVO.NodeRow> nodeRows(List<ClusterNode> nodes, boolean randomize) {
        List<ClusterOverviewVO.NodeRow> rows = new ArrayList<>();
        for (ClusterNode n : nodes) {
            double cpu = n.cpu();
            double gpu = n.gpu();
            if (randomize) {
                cpu = round1(clamp(cpu + ThreadLocalRandom.current().nextDouble(-1.5, 1.5), 0, 100));
                gpu = round1(clamp(gpu + ThreadLocalRandom.current().nextDouble(-1.5, 1.5), 0, 100));
            }
            rows.add(new ClusterOverviewVO.NodeRow(n.id(), n.zone(), n.role(), n.containers(), cpu, gpu,
                    n.latencyMs() + " ms", n.status()));
        }
        return rows;
    }

    private List<ClusterOverviewVO.AlarmRow> alarmRows(List<AlarmEvent> events) {
        return events.stream()
                .map(e -> new ClusterOverviewVO.AlarmRow(e.id(), e.time(), e.level(), e.message(), e.result()))
                .toList();
    }

    /**
     * 由 metric_sample 中的真实采样推导实时告警，供顶部消息铃铛展示。
     *
     * <p>与 alarm_event 台账（人工登记、可增删改）不同，这里的每一条都由所选日期区间内
     * 实际越阈值的采样计算得出，因此会随日期范围与采样数据真实变化，而不再是固定文案。
     */
    private List<ClusterOverviewVO.AlarmRow> derivedAlerts(String start, String end) {
        List<ClusterOverviewVO.AlarmRow> rows = new ArrayList<>();

        // 1) 采样可用率低于 99.5%（跌破 99% 升级 P1）
        below(metrics.sumByDate(MetricRepository.MetricKey.CLUSTER_AVAILABILITY, start, end), 99.5)
                .ifPresent(b -> {
                    boolean critical = b.worst() < 99.0;
                    rows.add(row(b.latest(), critical ? "P1" : "P2",
                            String.format("区间内 %d/%d 天采样可用率低于 99.5%%，最低 %.2f%%（%s）%s",
                                    b.count(), b.total(), b.worst(), b.worstDay(),
                                    critical ? "，已跌破 99% 红线，请核查节点健康度" : ""),
                            critical ? "待处理" : "已关注"));
                });

        // 2) 链路数据时延超过 3 s（超过 5 s 升级 P2）
        above(metrics.sumByDate(MetricRepository.MetricKey.CLUSTER_DATA_DELAY_SECONDS, start, end), 3.0)
                .ifPresent(b -> {
                    boolean severe = b.worst() > 5.0;
                    rows.add(row(b.latest(), severe ? "P2" : "P3",
                            String.format("区间内 %d/%d 天链路数据时延超过 3 s，峰值 %.1f s（%s）",
                                    b.count(), b.total(), b.worst(), b.worstDay()),
                            severe ? "待处理" : "已关注"));
                });

        // 3) 越权拦截显著高于区间日均
        Map<String, Double> blocks = metrics.sumByDate(MetricRepository.MetricKey.SECURITY_BLOCK, start, end);
        double blockAvg = blocks.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        above(blocks, Math.max(10.0, blockAvg * 1.4))
                .ifPresent(b -> rows.add(row(b.latest(), "P2",
                        String.format("%s 越权访问拦截 %.0f 次，达区间日均 %.1f 次的 %.1f 倍，建议核查来源",
                                b.worstDay(), b.worst(), blockAvg, blockAvg > 0 ? b.worst() / blockAvg : 0),
                        "已关注")));

        // 4) 模型灰度自动回滚
        double rollbackTotal = metrics.sum(MetricRepository.MetricKey.MODEL_ROLLBACK, start, end);
        above(metrics.sumByDate(MetricRepository.MetricKey.MODEL_ROLLBACK, start, end), 0.0)
                .ifPresent(b -> rows.add(row(b.latest(), "P2",
                        String.format("区间内模型灰度自动回滚 %.0f 次，涉及 %d 天，最近一次 %s",
                                rollbackTotal, b.count(), b.latest()),
                        "已回滚")));

        // 5) 广告填充率低于 90% 目标线
        below(metrics.sumByDate(MetricRepository.MetricKey.ADS_FILL_RATE, start, end), 90.0)
                .ifPresent(b -> rows.add(row(b.latest(), "P3",
                        String.format("区间内 %d/%d 天广告填充率低于 90%%，最低 %.1f%%（%s）",
                                b.count(), b.total(), b.worst(), b.worstDay()),
                        "已关注")));

        List<ClusterOverviewVO.AlarmRow> sorted = rows.stream()
                .sorted(Comparator.comparing(ClusterOverviewVO.AlarmRow::time).reversed()
                        .thenComparing(ClusterOverviewVO.AlarmRow::level))
                .limit(8)
                .toList();
        // 推导告警没有库表主键，这里补一个稳定序号供前端做 key
        List<ClusterOverviewVO.AlarmRow> out = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            ClusterOverviewVO.AlarmRow r = sorted.get(i);
            out.add(new ClusterOverviewVO.AlarmRow((long) (i + 1), r.time(), r.level(), r.message(), r.result()));
        }
        return out;
    }

    private static ClusterOverviewVO.AlarmRow row(String time, String level, String message, String result) {
        return new ClusterOverviewVO.AlarmRow(null, time, level, message, result);
    }

    /** 逐日序列的越线汇总：越线天数 / 总天数 / 最差值 / 最差值日期 / 最近一次越线日期。 */
    private record Breach(int count, int total, double worst, String worstDay, String latest) {
    }

    /** 统计低于阈值的天数（daily 按日期升序，故 latest 自然为最近日期）。 */
    private static Optional<Breach> below(Map<String, Double> daily, double threshold) {
        int count = 0;
        double worst = Double.MAX_VALUE;
        String worstDay = "";
        String latest = "";
        for (Map.Entry<String, Double> e : daily.entrySet()) {
            double v = e.getValue();
            if (v < threshold) {
                count++;
                latest = e.getKey();
                if (v < worst) {
                    worst = v;
                    worstDay = e.getKey();
                }
            }
        }
        return count == 0 ? Optional.empty() : Optional.of(new Breach(count, daily.size(), worst, worstDay, latest));
    }

    /** 统计高于阈值的天数（daily 按日期升序）。 */
    private static Optional<Breach> above(Map<String, Double> daily, double threshold) {
        int count = 0;
        double worst = -Double.MAX_VALUE;
        String worstDay = "";
        String latest = "";
        for (Map.Entry<String, Double> e : daily.entrySet()) {
            double v = e.getValue();
            if (v > threshold) {
                count++;
                latest = e.getKey();
                if (v > worst) {
                    worst = v;
                    worstDay = e.getKey();
                }
            }
        }
        return count == 0 ? Optional.empty() : Optional.of(new Breach(count, daily.size(), worst, worstDay, latest));
    }

    /** 取某指标按天聚合的最后 12 个点作为趋势迷你图；无真实数据时回退为收敛曲线。 */
    private List<Double> dailyTrend(String metricKey, String start, String end, double fallback) {
        List<Double> values = new ArrayList<>(metrics.sumByDate(metricKey, start, end).values());
        if (values.isEmpty()) {
            return trend(fallback);
        }
        return values.size() > 12 ? values.subList(values.size() - 12, values.size()) : values;
    }

    /** 环比百分比文案（统一走 {@link TrendUtils}）。 */
    private static String deltaOf(List<Double> series) {
        return TrendUtils.deltaOf(series);
    }

    /** 趋势方向（统一走 {@link TrendUtils}）。 */
    private static boolean rising(List<Double> series) {
        return TrendUtils.rising(series);
    }

    /** 收敛趋势迷你图（统一走 {@link TrendUtils}）。 */
    private static List<Double> trend(double current) {
        return TrendUtils.converge(current);
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static double clamp(double v, double min, double max) {
        return Math.min(max, Math.max(min, v));
    }
}
