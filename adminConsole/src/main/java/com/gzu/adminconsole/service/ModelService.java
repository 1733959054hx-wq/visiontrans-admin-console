package com.gzu.adminconsole.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.gzu.adminconsole.common.BusinessException;
import com.gzu.adminconsole.common.DateRange;
import com.gzu.adminconsole.common.TrendUtils;
import com.gzu.adminconsole.dto.common.KpiMetric;
import com.gzu.adminconsole.dto.meta.ActionResultVO;
import com.gzu.adminconsole.dto.model.ModelOverviewVO;
import com.gzu.adminconsole.model.ModelRelease;
import com.gzu.adminconsole.model.ReleaseEvent;
import com.gzu.adminconsole.repository.MetricRepository;
import com.gzu.adminconsole.repository.ModelRepository;

/**
 * AI 模型生命周期与热更中心 ViewModel 层。
 */
@Service
public class ModelService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ModelRepository repository;
    private final MetricRepository metrics;

    public ModelService(ModelRepository repository, MetricRepository metrics) {
        this.repository = repository;
        this.metrics = metrics;
    }

    /** 模型热更中心视图模型（可按版本时间线时间范围过滤，yyyy-MM-dd）。 */
    public ModelOverviewVO overview(String start, String end) {
        // KPI 由模型台账实时推导：增删模型后纳管数与覆盖率同步变化
        List<ModelRelease> models = repository.findModels();
        long cloudCount = models.stream()
                .filter(m -> m.type() != null && m.type().startsWith("云端"))
                .count();
        long edgeCount = models.size() - cloudCount;
        // 云端模型覆盖率为 "—"（不适用），只在有数值的模型上取均值
        double coverage = models.stream()
                .map(ModelRelease::coverage)
                .filter(c -> c != null && c.contains("%"))
                .mapToDouble(ModelService::parsePercent)
                .average()
                .orElse(0.0);
        long covered = models.stream().filter(m -> m.coverage() != null && m.coverage().contains("%")).count();
        long rollbacks = (long) metrics.sum(MetricRepository.MetricKey.MODEL_ROLLBACK, start, end);
        List<Double> rollbackTrend = dailyTrend(MetricRepository.MetricKey.MODEL_ROLLBACK, start, end, rollbacks);
        double hotfixSeconds = metrics.avg(MetricRepository.MetricKey.MODEL_HOTFIX_SECONDS, start, end);
        List<Double> hotfixTrend = dailyTrend(MetricRepository.MetricKey.MODEL_HOTFIX_SECONDS, start, end,
                hotfixSeconds);

        List<KpiMetric> kpis = List.of(
                new KpiMetric("纳管模型", String.valueOf(models.size()), null, "fa-cubes", "#1E3A8A", "#2563EB",
                        null, null, null,
                        "端侧 " + edgeCount + " · 云端 " + cloudCount,
                        trend(models.size())),
                new KpiMetric("端侧覆盖率", String.format("%.2f", coverage), " %", "fa-mobile-screen",
                        "#0EA5E9", "#38BDF8", null, null, null,
                        "取 " + covered + " 个有覆盖率版本均值（云端模型不适用）",
                        trend(coverage)),
                // 热更耗时与回滚次数均由 metric_sample 中真实采样统计，随所选日期范围变化
                new KpiMetric("平均热更耗时", String.format("%.1f", hotfixSeconds), " s", "fa-bolt", "#0B1E4D",
                        "#1E3A8A", null, null, null, "SLA ≤ 10s · 秒级下发", hotfixTrend),
                new KpiMetric("自动回滚", String.valueOf(rollbacks), " 次",
                        "fa-rotate-left", "#065F46", "#10B981", null, null, null, "所选范围内累计 · 全部成功",
                        rollbackTrend));

        return new ModelOverviewVO(kpis, modelRows(), timeline(start, end), grayscaleCard(),
                repository.findStrategies());
    }

    /** 取某指标按天聚合的最后 12 个点作为趋势迷你图；无真实数据时回退为收敛曲线。 */
    private List<Double> dailyTrend(String metricKey, String start, String end, double fallback) {
        List<Double> values = new ArrayList<>(metrics.sumByDate(metricKey, start, end).values());
        if (values.isEmpty()) {
            return trend(fallback);
        }
        return values.size() > 12 ? values.subList(values.size() - 12, values.size()) : values;
    }

    /** 覆盖率文案 "92.1%" → 数值，无法解析时按 0 计。 */
    private static double parsePercent(String text) {
        if (text == null) {
            return 0.0;
        }
        String digits = text.replaceAll("[^0-9.]", "");
        if (digits.isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(digits);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /** 收敛趋势迷你图（统一走 {@link TrendUtils}）。 */
    private static List<Double> trend(double current) {
        return TrendUtils.converge(current);
    }

    /** 调整全局灰度比例。 */
    public ActionResultVO updateGrayscale(int ratio) {
        if (ratio < 0 || ratio > 100) {
            throw new BusinessException("灰度比例取值范围为 0 ~ 100");
        }
        repository.setGrayscaleRatio(ratio);
        repository.pushEvent(new ReleaseEvent("灰度比例调整", now(),
                "全局灰度比例调整为 " + ratio + "%", "amber"));
        return ActionResultVO.ok("灰度比例已更新为 " + ratio + "%", String.valueOf(ratio));
    }

    /** 对指定模型执行秒级热更（置为全量）。 */
    public ActionResultVO hotUpdate(String name) {
        ModelRelease target = requireModel(name);
        repository.updateModel(target.withGrayRatio(100).withStatus("全量"));
        repository.pushEvent(new ReleaseEvent(name + " 秒级热更", now(),
                name + " 已下发至全量设备，热更耗时 6.4s", "green"));
        return ActionResultVO.ok(name + " 已触发秒级热更", name);
    }

    /** 对指定模型执行一键回滚。 */
    public ActionResultVO rollback(String name) {
        ModelRelease target = requireModel(name);
        repository.updateModel(target.withGrayRatio(0).withStatus("已回滚"));
        repository.increaseAutoRollback();
        repository.pushEvent(new ReleaseEvent(name + " 一键回滚", now(),
                name + " 已回滚至上一稳定版本（耗时 4.2s）", "rose"));
        return ActionResultVO.ok(name + " 已回滚至上一稳定版本", name);
    }

    /** 新增模型版本。 */
    public ActionResultVO createModel(ModelRelease model) {
        if (model == null || model.name() == null || model.name().isBlank()) {
            throw new BusinessException("模型名称不能为空");
        }
        if (repository.findModel(model.name()) != null) {
            throw new BusinessException("模型已存在：" + model.name());
        }
        repository.insertModel(model);
        repository.pushEvent(new ReleaseEvent(model.name() + " 版本登记", now(),
                model.name() + " 已登记（" + model.type() + " · " + model.precision() + "）", "amber"));
        return ActionResultVO.ok("模型 " + model.name() + " 已登记", model.name());
    }

    /** 更新模型信息（名称作为业务主键，不可改）。 */
    public ActionResultVO updateModel(ModelRelease model) {
        ModelRelease target = requireModel(model.name());
        repository.updateModel(model);
        repository.pushEvent(new ReleaseEvent(model.name() + " 配置更新", now(),
                model.name() + " 灰度比例调整为 " + model.grayRatio() + "%", "amber"));
        return ActionResultVO.ok("模型 " + target.name() + " 已更新", model.name());
    }

    /** 删除模型版本。 */
    public ActionResultVO deleteModel(String name) {
        ModelRelease target = requireModel(name);
        repository.deleteModel(name);
        repository.pushEvent(new ReleaseEvent(name + " 版本下线", now(),
                name + " 已从纳管清单中移除", "rose"));
        return ActionResultVO.ok("模型 " + target.name() + " 已删除", name);
    }

    /** 切换灰度 / 热更策略开关。 */
    public ActionResultVO updateStrategy(String name, boolean enabled) {
        repository.updateStrategy(name, enabled);
        return ActionResultVO.ok("策略「" + name + "」已" + (enabled ? "开启" : "关闭"), name);
    }

    private ModelRelease requireModel(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("模型名称不能为空");
        }
        ModelRelease target = repository.findModel(name);
        if (target == null) {
            throw new BusinessException("未找到模型：" + name);
        }
        return target;
    }

    private List<ModelOverviewVO.ModelRow> modelRows() {
        List<ModelOverviewVO.ModelRow> rows = new ArrayList<>();
        for (ModelRelease m : repository.findModels()) {
            boolean full = m.status().contains("全量");
            String statusTone = full ? "green" : m.status().contains("观察") ? "amber" : "blue";
            rows.add(new ModelOverviewVO.ModelRow(m.name(), m.type(), m.precision(), m.size(), m.coverage(),
                    m.grayRatio(), m.status(), statusTone,
                    full ? "fa-bolt" : "fa-circle-half-stroke",
                    full ? "green" : "amber"));
        }
        return rows;
    }

    private List<ModelOverviewVO.TimelineRow> timeline(String start, String end) {
        return repository.findTimeline().stream()
                .filter(e -> DateRange.inRange(e.time(), start, end))
                .map(e -> new ModelOverviewVO.TimelineRow(e.title(), e.time(), e.desc(), e.tone()))
                .toList();
    }

    private ModelOverviewVO.GrayscaleCard grayscaleCard() {
        List<ModelOverviewVO.BatchRow> batches = List.of(
                new ModelOverviewVO.BatchRow("批次 A", "10%", "已通过", "green"),
                new ModelOverviewVO.BatchRow("批次 B", "35%", "观察中", "amber"),
                new ModelOverviewVO.BatchRow("批次 C", "60%", "未开始", "blue"),
                new ModelOverviewVO.BatchRow("全量", "100%", "待触发", "slate"));
        return new ModelOverviewVO.GrayscaleCard(repository.getGrayscaleRatio(), batches);
    }

    private static String now() {
        return LocalDateTime.now().format(FORMATTER);
    }
}
