package com.gzu.adminconsole.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.gzu.adminconsole.common.BusinessException;
import com.gzu.adminconsole.common.DateRange;
import com.gzu.adminconsole.common.TrendUtils;
import com.gzu.adminconsole.config.AdminContext;
import com.gzu.adminconsole.dto.ads.AdOverviewVO;
import com.gzu.adminconsole.dto.ads.FrequencyUpdateRequest;
import com.gzu.adminconsole.dto.common.KpiMetric;
import com.gzu.adminconsole.dto.meta.ActionResultVO;
import com.gzu.adminconsole.entity.MetricSampleEntity;
import com.gzu.adminconsole.model.AdSlot;
import com.gzu.adminconsole.model.AuditLogEntry;
import com.gzu.adminconsole.model.FrequencyCap;
import com.gzu.adminconsole.repository.AdRepository;
import com.gzu.adminconsole.repository.MetricRepository;
import com.gzu.adminconsole.repository.SecurityRepository;

/**
 * 全网广告位排期与调度引擎 ViewModel 层。
 */
@Service
public class AdService {

    /** eCPM 矩阵最大值（用于色阶归一化）。 */
    private static final int MAX_ECPM = 92;
    /** 每次调度请求折算的素材曝光次数。 */
    private static final long IMPRESSIONS_PER_REQUEST = 3;
    /** 行业基准点击率（用于折算点击量）。 */
    private static final double BASE_CTR = 0.068;
    /** 行业基准转化率（用于折算转化量）。 */
    private static final double BASE_CVR = 0.112;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /** 无法从会话中识别操作人时的占位值。 */
    private static final String UNKNOWN = "未知";
    /** 反向代理透传客户端 IP 的请求头。 */
    private static final String FORWARDED_FOR = "X-Forwarded-For";

    private final AdRepository repository;
    private final MetricRepository metrics;
    private final SecurityRepository securityRepository;

    public AdService(AdRepository repository, MetricRepository metrics, SecurityRepository securityRepository) {
        this.repository = repository;
        this.metrics = metrics;
        this.securityRepository = securityRepository;
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

        // 曝光 / 点击 / 转化：优先取 metric_sample 真实采样，无采样时按确定性系数从调度请求折算
        long impressions = metricOrDerived(MetricRepository.MetricKey.ADS_IMPRESSION_HOURLY, start, end,
                requests * IMPRESSIONS_PER_REQUEST);
        long clicks = metricOrDerived(MetricRepository.MetricKey.ADS_CLICK_HOURLY, start, end,
                Math.round(impressions * BASE_CTR));
        long conversions = metricOrDerived(MetricRepository.MetricKey.ADS_CONVERSION_HOURLY, start, end,
                Math.round(clicks * BASE_CVR));
        double ctr = clicks * 100.0 / Math.max(1, impressions);
        double cvr = conversions * 100.0 / Math.max(1, clicks);

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
                        requestTrend),
                new KpiMetric("曝光量", String.format("%,d", impressions), null, "fa-eye", "#1E3A8A",
                        "#0EA5E9", null, null, null,
                        "调度请求 × " + IMPRESSIONS_PER_REQUEST + " 次素材曝光折算",
                        trend(impressions)),
                new KpiMetric("点击量", String.format("%,d", clicks), null, "fa-mouse-pointer", "#065F46",
                        "#10B981", null, null, null,
                        "按行业基准点击率 " + String.format("%.1f", BASE_CTR * 100) + "% 估算",
                        trend(clicks)),
                new KpiMetric("点击率", String.format("%.2f", ctr), " %",
                        "fa-chart-line", "#B45309", "#F59E0B", null, null, null,
                        "点击量 / 曝光量 · 在线广告位 "
                                + slots.stream().filter(AdSlot::online).count() + " 个",
                        trend(ctr)),
                new KpiMetric("转化量", String.format("%,d", conversions), null, "fa-bullseye", "#1E3A8A",
                        "#6366F1", null, null, null,
                        "按行业基准转化率 " + String.format("%.1f", BASE_CVR * 100) + "% 估算",
                        trend(conversions)),
                new KpiMetric("转化率", String.format("%.2f", cvr), " %", "fa-funnel-dollar",
                        "#065F46", "#10B981", null, null, null,
                        "转化量 / 点击量 · 跨广告位去重口径", trend(cvr)));

        List<String> days = repository.findDays(start, end);
        return new AdOverviewVO(kpis, days, slotRows(days.size()), caps(),
                repository.findFreqStrategies(), ecpmMatrix(), adviceCard(),
                realtimeCard(start, end, requests, impressions, clicks, conversions, ctr, cvr, slots));
    }

    /* ------------------------ 实时数据看板（曝光 / 点击 / 转化） ------------------------ */

    /** 实时数据看板：指标卡 + 24 小时趋势 + 广告位表现排行。 */
    private AdOverviewVO.RealtimeCard realtimeCard(String start, String end, long requests, long impressions,
                                                   long clicks, long conversions, double ctr, double cvr,
                                                   List<AdSlot> slots) {
        List<Double> requestSeries = hourlySeries(MetricRepository.MetricKey.ADS_REQUEST_HOURLY, start, end);
        List<Double> impressionSeries = hourlySeries(MetricRepository.MetricKey.ADS_IMPRESSION_HOURLY, start, end);
        List<Double> clickSeries = hourlySeries(MetricRepository.MetricKey.ADS_CLICK_HOURLY, start, end);
        List<Double> conversionSeries = hourlySeries(MetricRepository.MetricKey.ADS_CONVERSION_HOURLY, start, end);
        // 指标未落库时按确定系数从调度请求曲线折算，保证图表始终有形态
        if (impressionSeries.isEmpty()) {
            impressionSeries = scale(requestSeries, IMPRESSIONS_PER_REQUEST);
        }
        if (clickSeries.isEmpty()) {
            clickSeries = scale(impressionSeries, BASE_CTR);
        }
        if (conversionSeries.isEmpty()) {
            conversionSeries = scale(clickSeries, BASE_CVR);
        }
        List<String> hours = hoursOf(impressionSeries.size());

        double decisionMs = metrics.avg(MetricRepository.MetricKey.ADS_DECISION_MS, start, end);
        double fillRate = metrics.avg(MetricRepository.MetricKey.ADS_FILL_RATE, start, end);
        List<AdOverviewVO.RealtimeMetric> cards = List.of(
                new AdOverviewVO.RealtimeMetric("曝光量", String.format("%,d", impressions), "次", "fa-eye",
                        "调度请求 " + String.format("%,d", requests) + " 次 · 每请求 "
                                + IMPRESSIONS_PER_REQUEST + " 次素材曝光", "blue"),
                new AdOverviewVO.RealtimeMetric("点击量", String.format("%,d", clicks), "次", "fa-mouse-pointer",
                        "点击率 " + String.format("%.2f", ctr) + "%", "green"),
                new AdOverviewVO.RealtimeMetric("转化量", String.format("%,d", conversions), "次", "fa-bullseye",
                        "转化率 " + String.format("%.2f", cvr) + "%", "indigo"),
                new AdOverviewVO.RealtimeMetric("点击率", String.format("%.2f", ctr), "%", "fa-chart-line",
                        "点击量 / 曝光量", "amber"),
                new AdOverviewVO.RealtimeMetric("转化率", String.format("%.2f", cvr), "%", "fa-funnel-dollar",
                        "转化量 / 点击量", "emerald"),
                new AdOverviewVO.RealtimeMetric("平均决策耗时", String.format("%.1f", decisionMs), "ms",
                        "fa-stopwatch", "填充率 " + String.format("%.1f", fillRate) + "%", "slate"));

        return new AdOverviewVO.RealtimeCard(cards, hours, impressionSeries, clickSeries, conversionSeries,
                slotPerformance(slots), now());
    }

    /** 单个广告位的投放表现：由广告位台账按确定性权重推导，保证列表稳定可复现。 */
    private List<AdOverviewVO.SlotPerformance> slotPerformance(List<AdSlot> slots) {
        long totalWeight = 0;
        double[] weights = new double[slots.size()];
        for (int i = 0; i < slots.size(); i++) {
            AdSlot slot = slots.get(i);
            // 在线广告位权重更高；预售 / 已售罄按库存比例加权
            double base = slot.online() ? 1.0 : 0.25;
            double stock = slot.ratio() < 0 ? 0.5 : Math.max(0.15, slot.ratio() / 100.0);
            weights[i] = base * stock * (0.7 + seed(i, 3) * 0.6);
            totalWeight += (long) (weights[i] * 1000);
        }
        long pool = 1_000_000;
        List<AdOverviewVO.SlotPerformance> rows = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            long imp = totalWeight == 0 ? 0 : (long) (pool * weights[i] * 1000 / totalWeight);
            double slotCtr = 3.2 + seed(i, 5) * 6.4;
            long clk = Math.round(imp * slotCtr / 100.0);
            long conv = Math.round(clk * (6.0 + seed(i, 7) * 8.0) / 100.0);
            rows.add(new AdOverviewVO.SlotPerformance(slots.get(i).name(), imp, clk, conv, slotCtr,
                    clk == 0 ? 0 : conv * 100.0 / clk, slots.get(i).online() ? "blue" : "slate"));
        }
        return rows.stream()
                .sorted(Comparator.comparingLong(AdOverviewVO.SlotPerformance::impressions).reversed())
                .limit(6)
                .toList();
    }

    /** 取某指标最新一天的逐小时采样；无数据时返回空列表。 */
    private List<Double> hourlySeries(String metricKey, String start, String end) {
        return metrics.latestDay(metricKey, start, end).stream()
                .map(MetricSampleEntity::getValue)
                .toList();
    }

    /** 按系数缩放一条曲线（用于无真实采样时的折算）。 */
    private static List<Double> scale(List<Double> source, double factor) {
        return source.stream().map(v -> Math.max(0, v * factor)).toList();
    }

    /** 生成横轴刻度：pointCount 个点按 24 小时均匀取标签。 */
    private static List<String> hoursOf(int pointCount) {
        if (pointCount <= 0) {
            return List.of();
        }
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < pointCount; i++) {
            labels.add(String.format("%02d", Math.min(23, i * 24 / pointCount)));
        }
        return labels;
    }

    /** 取指标在范围内的求和；无采样时回退到按系数折算的推导值。 */
    private long metricOrDerived(String metricKey, String start, String end, long derived) {
        double value = metrics.sum(metricKey, start, end);
        return value > 0 ? (long) value : derived;
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
        writeLog("广告频控配置", "「" + cap.name() + "」更新为 " + request.value());
        return ActionResultVO.ok("「" + cap.name() + "」已更新为 " + request.value(), cap.name());
    }

    /** 采纳 AI 调优建议。 */
    public ActionResultVO adoptAdvice() {
        repository.setAdviceAdopted(true);
        writeLog("AI 调优建议", "采纳建议：「机场 × EN↔ZH」溢价 12%");
        return ActionResultVO.ok("已采纳 AI 建议：「机场 × EN↔ZH」溢价 12%", "机场 × EN↔ZH");
    }

    /** 新增广告位。 */
    public ActionResultVO createSlot(AdSlot slot) {
        if (slot == null || slot.name() == null || slot.name().isBlank()) {
            throw new BusinessException("广告位名称不能为空");
        }
        repository.insertSlot(slot);
        writeLog("广告位排期维护", "新增广告位「" + slot.name() + "」");
        return ActionResultVO.ok("广告位「" + slot.name() + "」已创建", slot.name());
    }

    /** 更新广告位。 */
    public ActionResultVO updateSlot(AdSlot slot) {
        if (slot == null || slot.id() == null) {
            throw new BusinessException("缺少广告位主键，无法更新");
        }
        repository.updateSlot(slot);
        writeLog("广告位排期维护", "更新广告位「" + slot.name() + "」");
        return ActionResultVO.ok("广告位「" + slot.name() + "」已更新", slot.name());
    }

    /** 删除广告位。 */
    public ActionResultVO deleteSlot(Long id) {
        repository.deleteSlot(id);
        writeLog("广告位排期维护", "删除广告位 #" + id);
        return ActionResultVO.ok("广告位 #" + id + " 已删除", String.valueOf(id));
    }

    /** 上线 / 下线广告位：online = true 上线 / false 下线（重复操作时拒绝）。 */
    public ActionResultVO toggleSlotOnline(Long id, boolean online) {
        AdSlot slot = repository.findSlot(id);
        if (slot == null) {
            throw new BusinessException("未找到广告位 #" + id);
        }
        if (slot.online() == online) {
            throw new BusinessException("广告位「" + slot.name() + "」已处于" + (online ? "上线" : "下线") + "状态");
        }
        repository.updateSlotOnline(id, online);
        writeLog("广告位上下线", "广告位「" + slot.name() + "」已" + (online ? "上线" : "下线"));
        return ActionResultVO.ok("广告位「" + slot.name() + "」已" + (online ? "上线" : "下线"), slot.name());
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
                    slot.remain(), tone, cells, slot.online()));
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

    private void writeLog(String action, String detail) {
        AdminContext.CurrentAdmin admin = AdminContext.get();
        String name = admin == null ? UNKNOWN : admin.name();
        String role = admin == null ? UNKNOWN : admin.roleName();
        String group = admin == null ? UNKNOWN : admin.groupName();
        AuditLogEntry entry = AuditLogEntry.of(now(), name, role, group, action, detail, clientIp(), "成功");
        securityRepository.pushAuditLog(entry);
    }

    /** 真实来源 IP：优先取反向代理透传的 X-Forwarded-For 首段。 */
    private String clientIp() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return UNKNOWN;
        }
        HttpServletRequest request = attrs.getRequest();
        String forwarded = request.getHeader(FORWARDED_FOR);
        if (forwarded == null || forwarded.isBlank()) {
            return request.getRemoteAddr();
        }
        return forwarded.split(",")[0].trim();
    }

    private static String now() {
        return LocalDateTime.now().format(FORMATTER);
    }
}
