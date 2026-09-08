package com.gzu.adminconsole.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.gzu.adminconsole.common.BusinessException;
import com.gzu.adminconsole.common.DateRange;
import com.gzu.adminconsole.common.TrendUtils;
import com.gzu.adminconsole.dto.ads.AdOverviewVO;
import com.gzu.adminconsole.dto.ads.FrequencyUpdateRequest;
import com.gzu.adminconsole.dto.common.KpiMetric;
import com.gzu.adminconsole.dto.meta.ActionResultVO;
import com.gzu.adminconsole.model.AdSlot;
import com.gzu.adminconsole.model.FrequencyCap;
import com.gzu.adminconsole.repository.AdRepository;
import com.gzu.adminconsole.repository.MetricRepository;

/**
 * 全网广告位排期与调度引擎 ViewModel 层。
 */
@Service
public class AdService {

    /** eCPM 矩阵最大值（用于色阶归一化）。 */
    private static final int MAX_ECPM = 92;

    private final AdRepository repository;
    private final MetricRepository metrics;

    public AdService(AdRepository repository, MetricRepository metrics) {
        this.repository = repository;
        this.metrics = metrics;
    }

    /** 广告排期大盘视图模型（可按排期日期范围过滤甘特图，yyyy-MM-dd）。 */
    public AdOverviewVO overview(String start, String end) {
        // KPI 由广告位台账实时推导：新增 / 编辑 / 删除排期后指标同步变化
        List<AdSlot> slots = repository.findSlots();
        long requests = (long) metrics.sum(MetricRepository.MetricKey.ADS_REQUEST_HOURLY, start, end);
        List<Double> requestTrend = dailyTrend(MetricRepository.MetricKey.ADS_REQUEST_HOURLY, start, end, requests);
        double decisionMs = metrics.avg(MetricRepository.MetricKey.ADS_DECISION_MS, start, end);
        double fillRate = metrics.avg(MetricRepository.MetricKey.ADS_FILL_RATE, start, end);
        long onSale = slots.stream().filter(s -> !"空闲可购".equals(s.status())).count();
        double occupancy = slots.stream()
                .filter(s -> s.ratio() >= 0)
                .mapToDouble(AdSlot::ratio)
                .average()
                .orElse(0.0);
        long conflicts = slots.stream()
                .filter(s -> "已售罄".equals(s.status()) || s.ratio() >= 90)
                .count();

        List<KpiMetric> kpis = List.of(
                new KpiMetric("在售广告位", String.format("%,d", onSale), null, "fa-rectangle-ad", "#1E3A8A",
                        "#2563EB", null, null, null,
                        "台账共 " + slots.size() + " 个广告位，其中售出 / 预售 " + onSale + " 个",
                        trend(onSale)),
                new KpiMetric("未来 7 日库存占用率", String.format("%.2f", occupancy), " %", "fa-chart-pie",
                        "#B45309", "#F59E0B", null, null, null,
                        "按 " + slots.size() + " 个广告位占用比例均值",
                        trend(occupancy)),
                new KpiMetric("排期冲突告警", String.valueOf(conflicts), null, "fa-triangle-exclamation",
                        "#0B1E4D", "#1E3A8A", null, null, null,
                        "占用比例 ≥ 90% 或已售罄的广告位",
                        trend(conflicts)),
                // 调度请求数由 metric_sample 中按小时的真实采样求和，随所选日期范围变化
                new KpiMetric("调度请求数", String.format("%,d", requests), null, "fa-arrows-spin", "#065F46",
                        "#10B981", deltaOf(requestTrend), rising(requestTrend), "green",
                        "平均决策 " + String.format("%.1f", decisionMs) + "ms · 填充率 "
                                + String.format("%.1f", fillRate) + "%",
                        requestTrend));

        List<String> days = repository.findDays(start, end);
        return new AdOverviewVO(kpis, days, slotRows(days.size()), caps(),
                repository.findFreqStrategies(), ecpmMatrix(), adviceCard());
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

    /** 更新单用户频次限制。 */
    public ActionResultVO updateFrequency(FrequencyUpdateRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new BusinessException("频次配置名称不能为空");
        }
        FrequencyCap cap = repository.findCap(request.name());
        if (cap == null) {
            throw new BusinessException("未找到频次配置：" + request.name());
        }
        if (request.value() < 0 || request.value() > cap.max()) {
            throw new BusinessException("取值超出范围：0 ~ " + cap.max());
        }
        repository.updateCap(cap.withValue(request.value()));
        return ActionResultVO.ok("「" + cap.name() + "」已更新为 " + request.value(), cap.name());
    }

    /** 采纳 AI 调优建议。 */
    public ActionResultVO adoptAdvice() {
        repository.setAdviceAdopted(true);
        return ActionResultVO.ok("已采纳 AI 建议：「机场 × EN↔ZH」溢价 12%", "机场 × EN↔ZH");
    }

    /** 新增广告位。 */
    public ActionResultVO createSlot(AdSlot slot) {
        if (slot == null || slot.name() == null || slot.name().isBlank()) {
            throw new BusinessException("广告位名称不能为空");
        }
        repository.insertSlot(slot);
        return ActionResultVO.ok("广告位「" + slot.name() + "」已创建", slot.name());
    }

    /** 更新广告位。 */
    public ActionResultVO updateSlot(AdSlot slot) {
        if (slot == null || slot.id() == null) {
            throw new BusinessException("缺少广告位主键，无法更新");
        }
        repository.updateSlot(slot);
        return ActionResultVO.ok("广告位「" + slot.name() + "」已更新", slot.name());
    }

    /** 删除广告位。 */
    public ActionResultVO deleteSlot(Long id) {
        repository.deleteSlot(id);
        return ActionResultVO.ok("广告位 #" + id + " 已删除", String.valueOf(id));
    }

    private List<AdOverviewVO.AdSlotRow> slotRows(int dayCount) {
        List<AdSlot> slots = repository.findSlots();
        List<AdOverviewVO.AdSlotRow> rows = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            AdSlot slot = slots.get(i);
            List<Boolean> cells = new ArrayList<>();
            for (int j = 0; j < dayCount; j++) {
                boolean fill = switch (slot.status()) {
                    case "已售罄" -> true;
                    case "空闲可购" -> false;
                    default -> seed(i, j) > 0.45;
                };
                cells.add(fill);
            }
            String tone = switch (slot.remain()) {
                case "100%" -> "red";
                case "0%" -> "green";
                case "预售" -> "blue";
                default -> "amber";
            };
            rows.add(new AdOverviewVO.AdSlotRow(slot.id(), slot.name(), slot.status(), slot.color(),
                    slot.remain(), tone, cells));
        }
        return rows;
    }

    /** 确定性伪随机，保证每次渲染结果一致。 */
    private double seed(int i, int j) {
        return ((i * 7 + j * 13 + 5) % 10) / 10.0;
    }

    private List<AdOverviewVO.FrequencyCapRow> caps() {
        return repository.findCaps().stream()
                .map(c -> new AdOverviewVO.FrequencyCapRow(c.name(), c.display(), c.max(), c.value()))
                .toList();
    }

    private AdOverviewVO.EcpmMatrix ecpmMatrix() {
        List<String> scenes = repository.findScenes();
        List<String> langs = repository.findLangs();
        List<List<Integer>> values = new ArrayList<>();
        for (int i = 0; i < scenes.size(); i++) {
            List<Integer> row = new ArrayList<>();
            for (int j = 0; j < langs.size(); j++) {
                double v = 28 + 34 * Math.abs(Math.sin((i + 1) * 1.7 + (j + 1) * 0.9)) + (i == j ? 22 : 0);
                row.add((int) Math.round(v));
            }
            values.add(row);
        }
        return new AdOverviewVO.EcpmMatrix(scenes, langs, values, MAX_ECPM);
    }

    private AdOverviewVO.AdviceCard adviceCard() {
        return new AdOverviewVO.AdviceCard(
                "「机场 × EN↔ZH」eCPM 指数最高，建议提升该组合溢价 12% 并追加预算",
                repository.isAdviceAdopted());
    }
}
