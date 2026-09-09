package com.gzu.adminconsole.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.gzu.adminconsole.common.BusinessException;
import com.gzu.adminconsole.common.DateRange;
import com.gzu.adminconsole.common.TrendUtils;
import com.gzu.adminconsole.config.AdminContext;
import com.gzu.adminconsole.dto.common.KpiMetric;
import com.gzu.adminconsole.dto.meta.ActionResultVO;
import com.gzu.adminconsole.dto.moderation.ModerationOverviewVO;
import com.gzu.adminconsole.model.AuditLogEntry;
import com.gzu.adminconsole.model.AuditPackage;
import com.gzu.adminconsole.model.GlossaryTask;
import com.gzu.adminconsole.model.MaterialAsset;
import com.gzu.adminconsole.model.RefundRecord;
import com.gzu.adminconsole.model.UgcRecord;
import com.gzu.adminconsole.repository.ModerationRepository;
import com.gzu.adminconsole.repository.SecurityRepository;

/**
 * 语种术语库审核与 UGC 风控中台 ViewModel 层。
 */
@Service
public class ModerationService {

    /** 看板列固定顺序（与前端 COLUMNS 保持一致）。 */
    public static final List<String> KANBAN_COLUMNS = List.of("待初审", "术语复核", "已发布");

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /** 无法从会话中识别操作人时的占位值。 */
    private static final String UNKNOWN = "未知";
    /** 反向代理透传客户端 IP 的请求头。 */
    private static final String FORWARDED_FOR = "X-Forwarded-For";

    private final ModerationRepository repository;
    private final SecurityRepository securityRepository;

    public ModerationService(ModerationRepository repository, SecurityRepository securityRepository) {
        this.repository = repository;
        this.securityRepository = securityRepository;
    }

    /** 审核与风控中台视图模型（可按任务截止时间范围过滤，yyyy-MM-dd；无截止时间的任务始终保留）。 */
    public ModerationOverviewVO overview(String start, String end) {
        // KPI 由看板任务与素材台账实时推导：新增 / 移动 / 删除记录后指标同步变化
        List<GlossaryTask> tasks = repository.findTasks().stream()
                .filter(t -> DateRange.inRange(t.due(), start, end))
                .toList();
        List<MaterialAsset> assets = repository.findAssets();
        long pending = tasks.stream()
                .filter(t -> t.column() != null && t.column().startsWith("待"))
                .count();
        long inReview = tasks.stream()
                .filter(t -> t.column() != null && !t.column().startsWith("待") && !"已发布".equals(t.column()))
                .count();
        double accuracy = assets.stream()
                .mapToDouble(a -> parsePercent(a.confidence()))
                .average()
                .orElse(0.0);
        UgcRecord ugc = repository.findUgc();
        int ugcHits = ugc == null || ugc.hits() == null ? 0 : ugc.hits().size();

        List<KpiMetric> kpis = List.of(
                new KpiMetric("在库术语总量", String.valueOf(tasks.size()), null, "fa-language", "#1E3A8A",
                        "#2563EB", null, null, null,
                        "看板在库术语包 " + tasks.size() + " 个 · 覆盖多语种",
                        trend(tasks.size())),
                new KpiMetric("今日待审词条", String.valueOf(pending), null, "fa-hourglass-half", "#B45309",
                        "#F59E0B", null, null, null,
                        "待处理 " + pending + " · 流转中 " + inReview,
                        trend(pending)),
                new KpiMetric("素材机审准确率", String.format("%.2f", accuracy), " %", "fa-robot", "#0B1E4D",
                        "#1E3A8A", null, null, null,
                        "取 " + assets.size() + " 条素材的置信度均值",
                        trend(accuracy)),
                new KpiMetric("UGC 违规拦截", String.valueOf(ugcHits), null, "fa-shield-halved", "#B91C1C",
                        "#EF4444", null, null, null,
                        "当前样本命中风控规则数",
                        trend(ugcHits)));

        return new ModerationOverviewVO(kpis, kanban(tasks), assets(), packages(), ugcCard(), refundCard());
    }

    /** 置信度文案 "91.2%" → 数值，无法解析时按 0 计。 */
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

    /** 对当前 UGC 记录执行处置。 */
    public ActionResultVO decideUgc(String action) {
        UgcRecord current = repository.findUgc();
        String verdict = switch (action == null ? "" : action.toLowerCase()) {
            case "ban" -> "已封禁";
            case "pass" -> "已放行";
            case "review" -> "复核中";
            default -> throw new BusinessException("不支持的处置动作：" + action + "（可选 ban / pass / review）");
        };
        repository.saveUgc(new UgcRecord(current.id(), current.level(), current.segments(), current.hits(),
                verdict));
        if ("已放行".equals(verdict)) {
            RefundRecord refund = repository.findRefund();
            repository.saveRefund(new RefundRecord(refund.orderNo(), refund.paid(), refund.suggestRefund(),
                    refund.advice(), refund.sla(), refund.reasons(), "已退款"));
        }
        return ActionResultVO.ok("UGC " + current.id() + " 处置完成：" + verdict, current.id());
    }

    /* ------------------------------ 术语包任务 ------------------------------ */

    /** 新增术语包任务。 */
    public ActionResultVO createTask(GlossaryTask task) {
        if (task == null || task.title() == null || task.title().isBlank()) {
            throw new BusinessException("术语包名称不能为空");
        }
        requireColumn(task.column());
        repository.insertTask(task);
        return ActionResultVO.ok("术语包「" + task.title() + "」已加入" + task.column(), task.title());
    }

    /** 更新术语包任务。 */
    public ActionResultVO updateTask(GlossaryTask task) {
        if (task.id() == null) {
            throw new BusinessException("缺少术语包主键，无法更新");
        }
        requireColumn(task.column());
        repository.updateTask(task);
        return ActionResultVO.ok("术语包「" + task.title() + "」已更新", task.title());
    }

    /** 校验看板列，避免写入非法列后卡片在界面上"消失"。 */
    private void requireColumn(String column) {
        if (column == null || !KANBAN_COLUMNS.contains(column)) {
            throw new BusinessException("看板列不合法：" + column + "，可选值 " + KANBAN_COLUMNS);
        }
    }

    /** 移动术语包任务到上一列 / 下一列。 */
    public ActionResultVO moveTask(Long id, String direction) {
        GlossaryTask current = findTask(id);
        List<String> columns = KANBAN_COLUMNS;
        int index = columns.indexOf(current.column());
        int target = "prev".equalsIgnoreCase(direction) ? index - 1 : index + 1;
        if (target < 0 || target >= columns.size()) {
            throw new BusinessException("已在" + ("prev".equalsIgnoreCase(direction) ? "第一列" : "最后一列"));
        }
        repository.moveTask(id, columns.get(target));
        return ActionResultVO.ok("「" + current.title() + "」已移动到" + columns.get(target), current.title());
    }

    /** 删除术语包任务。 */
    public ActionResultVO deleteTask(Long id) {
        GlossaryTask current = findTask(id);
        repository.deleteTask(id);
        return ActionResultVO.ok("术语包「" + current.title() + "」已删除", current.title());
    }

    /* ------------------------------ 素材机审 ------------------------------ */

    /** 新增素材。 */
    public ActionResultVO createAsset(MaterialAsset asset) {
        if (asset == null || asset.name() == null || asset.name().isBlank()) {
            throw new BusinessException("素材名称不能为空");
        }
        repository.insertAsset(asset);
        return ActionResultVO.ok("素材「" + asset.name() + "」已加入机审队列", asset.name());
    }

    /** 更新素材。 */
    public ActionResultVO updateAsset(MaterialAsset asset) {
        if (asset.id() == null) {
            throw new BusinessException("缺少素材主键，无法更新");
        }
        repository.updateAsset(asset);
        return ActionResultVO.ok("素材「" + asset.name() + "」已更新", asset.name());
    }

    /** 删除素材。 */
    public ActionResultVO deleteAsset(Long id) {
        repository.deleteAsset(id);
        return ActionResultVO.ok("素材 #" + id + " 已删除", String.valueOf(id));
    }

    /* ------------------------------ 退款仲裁 ------------------------------ */

    /** 按平台建议执行退款。 */
    public ActionResultVO processRefund() {
        RefundRecord refund = repository.findRefund();
        if (refund == null) {
            throw new BusinessException("当前没有待仲裁的退款工单");
        }
        if (!"待仲裁".equals(refund.status())) {
            throw new BusinessException("工单 " + refund.orderNo() + " 已处理：" + refund.status());
        }
        repository.saveRefund(new RefundRecord(refund.orderNo(), refund.paid(), refund.suggestRefund(),
                refund.advice(), refund.sla(), refund.reasons(), "已退款"));
        return ActionResultVO.ok("订单 " + refund.orderNo() + " 已按建议退款 " + refund.suggestRefund(),
                refund.orderNo());
    }

    private GlossaryTask findTask(Long id) {
        List<GlossaryTask> tasks = repository.findTasks();
        return tasks.stream().filter(t -> id.equals(t.id())).findFirst()
                .orElseThrow(() -> new BusinessException("未找到术语包任务：" + id));
    }

    private List<ModerationOverviewVO.KanbanColumn> kanban(List<GlossaryTask> tasks) {
        return List.of(
                column("待初审", "18", "amber", tasks),
                column("术语复核", "7", "blue", tasks),
                column("已发布", "1,286", "green", tasks));
    }

    private ModerationOverviewVO.KanbanColumn column(String title, String countLabel, String tone,
                                                     List<GlossaryTask> tasks) {
        List<ModerationOverviewVO.KanbanCard> cards = tasks.stream()
                .filter(t -> t.column().equals(title))
                .map(t -> new ModerationOverviewVO.KanbanCard(t.id(), t.title(), t.priority(), t.meta(),
                        t.owner(), t.due()))
                .toList();
        return new ModerationOverviewVO.KanbanColumn(title, countLabel, tone, cards);
    }

    private List<ModerationOverviewVO.MaterialRow> assets() {
        List<ModerationOverviewVO.MaterialRow> rows = new ArrayList<>();
        for (MaterialAsset a : repository.findAssets()) {
            String tone = switch (a.verdict()) {
                case "通过" -> "green";
                case "人工复审" -> "amber";
                default -> "red";
            };
            rows.add(new ModerationOverviewVO.MaterialRow(a.id(), a.name(), a.confidence(), a.verdict(), tone));
        }
        return rows;
    }

    /* ------------------------------ 语种包 / 课程知识包 ------------------------------ */

    /** 新增审核包（未指定状态时进入待审核队列）。 */
    public ActionResultVO createPackage(AuditPackage pkg) {
        if (pkg == null || pkg.name() == null || pkg.name().isBlank()) {
            throw new BusinessException("审核包名称不能为空");
        }
        String status = pkg.status() == null || pkg.status().isBlank()
                ? AuditPackage.STATUS_PENDING : pkg.status();
        repository.insertPackage(new AuditPackage(null, pkg.type(), pkg.name(), pkg.source(), pkg.meta(), status));
        writeLog("审核包维护", "新增" + pkg.type() + "「" + pkg.name() + "」（" + status + "）");
        return ActionResultVO.ok("审核包「" + pkg.name() + "」已加入" + status + "队列", pkg.name());
    }

    /** 更新审核包。 */
    public ActionResultVO updatePackage(AuditPackage pkg) {
        if (pkg == null || pkg.id() == null) {
            throw new BusinessException("缺少审核包主键，无法更新");
        }
        AuditPackage current = repository.findPackage(pkg.id());
        if (current == null) {
            throw new BusinessException("未找到审核包 #" + pkg.id());
        }
        repository.updatePackage(pkg);
        writeLog("审核包维护", "更新审核包「" + pkg.name() + "」（" + pkg.status() + "）");
        return ActionResultVO.ok("审核包「" + pkg.name() + "」已更新", pkg.name());
    }

    /** 删除审核包。 */
    public ActionResultVO deletePackage(Long id) {
        AuditPackage current = repository.findPackage(id);
        if (current == null) {
            throw new BusinessException("未找到审核包 #" + id);
        }
        repository.deletePackage(id);
        writeLog("审核包维护", "删除审核包「" + current.name() + "」");
        return ActionResultVO.ok("审核包「" + current.name() + "」已删除", current.name());
    }

    /** 审核包复核：decision = pass 发布 / reject 驳回（仅待审核可审）。 */
    public ActionResultVO reviewPackage(Long id, String decision) {
        AuditPackage current = repository.findPackage(id);
        if (current == null) {
            throw new BusinessException("未找到审核包 #" + id);
        }
        if (!AuditPackage.STATUS_PENDING.equals(current.status())) {
            throw new BusinessException("审核包「" + current.name() + "」已处理：" + current.status());
        }
        String target = "pass".equalsIgnoreCase(decision)
                ? AuditPackage.STATUS_PUBLISHED : AuditPackage.STATUS_REJECTED;
        repository.updatePackage(new AuditPackage(current.id(), current.type(), current.name(), current.source(),
                current.meta(), target));
        writeLog("审核包复核", "审核包「" + current.name() + "」复核结果：" + target);
        return ActionResultVO.ok("审核包「" + current.name() + "」已" + target, current.name());
    }

    /** 素材人工复审：decision = pass 通过 / reject 驳回（仅人工复审可审）。 */
    public ActionResultVO reviewAsset(Long id, String decision) {
        MaterialAsset current = repository.findAsset(id);
        if (current == null) {
            throw new BusinessException("未找到素材 #" + id);
        }
        if (!"人工复审".equals(current.verdict())) {
            throw new BusinessException("素材「" + current.name() + "」不在人工复审队列：" + current.verdict());
        }
        String target = "pass".equalsIgnoreCase(decision) ? "通过" : "驳回";
        repository.updateAsset(new MaterialAsset(current.id(), current.name(), current.confidence(), target));
        writeLog("素材机审复核", "素材「" + current.name() + "」复核结果：" + target);
        return ActionResultVO.ok("素材「" + current.name() + "」复核完成：" + target, current.name());
    }

    /** 语种包 / 课程知识包审核行（状态配映射：待审核 amber、已发布 green、已驳回 red）。 */
    private List<ModerationOverviewVO.PackageRow> packages() {
        return repository.findPackages().stream()
                .map(p -> new ModerationOverviewVO.PackageRow(p.id(), p.type(), p.name(), p.source(), p.meta(),
                        p.status(), packageTone(p.status())))
                .toList();
    }

    /** 审核包状态配映射。 */
    private String packageTone(String status) {
        return switch (status) {
            case AuditPackage.STATUS_PENDING -> "amber";
            case AuditPackage.STATUS_PUBLISHED -> "green";
            default -> "red";
        };
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

    private ModerationOverviewVO.UgcCard ugcCard() {
        UgcRecord record = repository.findUgc();
        List<ModerationOverviewVO.UgcSegment> segments = record.segments().stream()
                .map(s -> new ModerationOverviewVO.UgcSegment(s.text(), s.tone()))
                .toList();
        List<ModerationOverviewVO.UgcRule> rules = record.hits().stream()
                .map(h -> new ModerationOverviewVO.UgcRule(h.name(), h.score()))
                .toList();
        return new ModerationOverviewVO.UgcCard(record.id(), "违规片段原位高亮", record.level(), "red",
                segments, rules, record.verdict());
    }

    private ModerationOverviewVO.RefundCard refundCard() {
        RefundRecord r = repository.findRefund();
        return new ModerationOverviewVO.RefundCard(r.orderNo(), r.paid(), r.suggestRefund(), r.advice(),
                r.sla(), r.reasons(), r.status());
    }
}
