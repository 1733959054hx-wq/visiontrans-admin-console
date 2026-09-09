package com.gzu.adminconsole.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.gzu.adminconsole.common.BusinessException;
import com.gzu.adminconsole.common.TrendUtils;
import com.gzu.adminconsole.config.AdminContext;
import com.gzu.adminconsole.dto.common.KpiMetric;
import com.gzu.adminconsole.dto.finance.FinanceOverviewVO;
import com.gzu.adminconsole.dto.meta.ActionResultVO;
import com.gzu.adminconsole.model.AuditLogEntry;
import com.gzu.adminconsole.model.CustomerOrder;
import com.gzu.adminconsole.model.InvoiceApplication;
import com.gzu.adminconsole.model.SettlementRecord;
import com.gzu.adminconsole.repository.FinanceRepository;
import com.gzu.adminconsole.repository.SecurityRepository;

/**
 * 财务订单、商户结算与发票审核 ViewModel 层。
 */
@Service
public class FinanceService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /** 无法从会话中识别操作人时的占位值。 */
    private static final String UNKNOWN = "未知";
    /** 反向代理透传客户端 IP 的请求头。 */
    private static final String FORWARDED_FOR = "X-Forwarded-For";

    private final FinanceRepository repository;
    private final SecurityRepository securityRepository;

    public FinanceService(FinanceRepository repository, SecurityRepository securityRepository) {
        this.repository = repository;
        this.securityRepository = securityRepository;
    }

    /** 财务订单大盘视图模型。 */
    public FinanceOverviewVO overview() {
        List<CustomerOrder> orders = repository.findOrders();
        List<SettlementRecord> settlements = repository.findSettlements();
        List<InvoiceApplication> invoices = repository.findInvoices();

        // KPI 由台账实时推导：确认支付 / 退款 / 对账 / 结算后指标同步变化
        double paidAmount = orders.stream()
                .filter(o -> CustomerOrder.STATUS_PAID.equals(o.status()))
                .mapToDouble(o -> parseAmount(o.amount()))
                .sum();
        long abnormal = orders.stream()
                .filter(o -> CustomerOrder.STATUS_ABNORMAL.equals(o.status()))
                .count();
        double pendingSettle = settlements.stream()
                .filter(s -> SettlementRecord.STATUS_RECONCILED.equals(s.status()))
                .mapToDouble(s -> parseAmount(s.amount()))
                .sum();

        List<KpiMetric> kpis = List.of(
                new KpiMetric("本月交易额", formatYuan(paidAmount), null, "fa-sack-dollar", "#1E3A8A",
                        "#2563EB", null, null, null,
                        "已支付订单金额合计 · 共 " + orders.size() + " 笔订单",
                        trend(paidAmount)),
                new KpiMetric("订单总数", String.valueOf(orders.size()), null, "fa-receipt", "#0B1E4D",
                        "#1E3A8A", null, null, null,
                        "会员 / 课程 / 数字内容全类型",
                        trend(orders.size())),
                new KpiMetric("异常订单数", String.valueOf(abnormal), null, "fa-triangle-exclamation",
                        "#B91C1C", "#EF4444", null, null, null,
                        "支付回调超时 / 重复扣款待核查",
                        trend(abnormal)),
                new KpiMetric("待结算金额", formatYuan(pendingSettle), null, "fa-hand-holding-dollar",
                        "#B45309", "#F59E0B", null, null, null,
                        "已对账未结算合计 · 对账周期 T+1",
                        trend(pendingSettle)));

        return new FinanceOverviewVO(kpis, orderRows(orders), settlementRows(settlements), invoiceRows(invoices));
    }

    /* ------------------------------ 订单处理 ------------------------------ */

    /**
     * 处理 C 端订单：markPaid 确认支付（待支付/支付异常 → 已支付）、refund 退款（已支付/支付异常/退款中 → 已退款）、
     * close 关闭（未终态 → 已关闭）。
     */
    public ActionResultVO handleOrder(Long id, String action) {
        CustomerOrder order = repository.findOrder(id);
        if (order == null) {
            throw new BusinessException("未找到订单 #" + id);
        }
        String target = switch (action == null ? "" : action.toLowerCase()) {
            case "markpaid" -> CustomerOrder.STATUS_PAID;
            case "refund" -> CustomerOrder.STATUS_REFUNDED;
            case "close" -> CustomerOrder.STATUS_CLOSED;
            default -> throw new BusinessException("不支持的订单处理动作：" + action + "（可选 markPaid / refund / close）");
        };
        String status = order.status();
        if ("markpaid".equalsIgnoreCase(action)) {
            if (!CustomerOrder.STATUS_PENDING.equals(status)
                    && !CustomerOrder.STATUS_ABNORMAL.equals(status)) {
                throw new BusinessException("订单 " + order.orderNo() + " 当前状态不支持确认支付：" + status);
            }
        } else if ("refund".equalsIgnoreCase(action)) {
            if (!CustomerOrder.STATUS_PAID.equals(status)
                    && !CustomerOrder.STATUS_ABNORMAL.equals(status)
                    && !CustomerOrder.STATUS_REFUNDING.equals(status)) {
                throw new BusinessException("订单 " + order.orderNo() + " 当前状态不支持退款：" + status);
            }
        } else if (CustomerOrder.STATUS_REFUNDED.equals(status) || CustomerOrder.STATUS_CLOSED.equals(status)) {
            throw new BusinessException("订单 " + order.orderNo() + " 已是终态：" + status);
        }
        repository.updateOrder(new CustomerOrder(order.id(), order.orderNo(), order.customer(), order.type(),
                order.amount(), target, order.created(), order.note()));
        writeLog("订单处理", "订单 " + order.orderNo() + " 执行 " + action + "：" + status + " → " + target);
        return ActionResultVO.ok("订单 " + order.orderNo() + " 已" + actionLabel(action, target),
                order.orderNo());
    }

    /** 订单处理动作的提示文案。 */
    private String actionLabel(String action, String target) {
        return switch (action == null ? "" : action.toLowerCase()) {
            case "markpaid" -> "标记为已支付";
            case "refund" -> "标记为已退款";
            default -> "标记为" + target;
        };
    }

    /* ------------------------------ 商户结算 ------------------------------ */

    /** 对账：待对账 → 已对账。 */
    public ActionResultVO reconcile(Long id) {
        SettlementRecord record = findSettlement(id);
        requireStatus(record, SettlementRecord.STATUS_PENDING, "对账");
        repository.updateSettlement(withStatus(record, SettlementRecord.STATUS_RECONCILED));
        writeLog("商户结算对账", "结算单 " + record.merchant() + "（" + record.period() + "）已对账");
        return ActionResultVO.ok("结算单「" + record.merchant() + "」已对账", record.merchant());
    }

    /** 结算：已对账 → 已结算。 */
    public ActionResultVO settle(Long id) {
        SettlementRecord record = findSettlement(id);
        requireStatus(record, SettlementRecord.STATUS_RECONCILED, "结算");
        repository.updateSettlement(withStatus(record, SettlementRecord.STATUS_SETTLED));
        writeLog("商户结算打款", "结算单 " + record.merchant() + "（" + record.period() + "）已结算");
        return ActionResultVO.ok("结算单「" + record.merchant() + "」已结算", record.merchant());
    }

    /* ------------------------------ 发票审核 ------------------------------ */

    /** 发票审核：approved = true 开票，false 驳回（仅待审核可审）。 */
    public ActionResultVO reviewInvoice(Long id, boolean approved) {
        InvoiceApplication invoice = repository.findInvoice(id);
        if (invoice == null) {
            throw new BusinessException("未找到发票申请 #" + id);
        }
        if (!InvoiceApplication.STATUS_PENDING.equals(invoice.status())) {
            throw new BusinessException("发票申请 " + invoice.applyNo() + " 已处理：" + invoice.status());
        }
        String target = approved ? InvoiceApplication.STATUS_ISSUED : InvoiceApplication.STATUS_REJECTED;
        repository.updateInvoice(new InvoiceApplication(invoice.id(), invoice.applyNo(), invoice.applicant(),
                invoice.title(), invoice.taxNo(), invoice.amount(), target, invoice.applied()));
        writeLog("发票审核", "发票申请 " + invoice.applyNo() + " 审核结果：" + target);
        return ActionResultVO.ok("发票申请 " + invoice.applyNo() + " 已" + target, invoice.applyNo());
    }

    /* ------------------------------ 私有方法 ------------------------------ */

    private SettlementRecord findSettlement(Long id) {
        SettlementRecord record = repository.findSettlement(id);
        if (record == null) {
            throw new BusinessException("未找到结算单 #" + id);
        }
        return record;
    }

    /** 校验结算单处于期望状态，否则拒绝流转。 */
    private void requireStatus(SettlementRecord record, String expected, String action) {
        if (!expected.equals(record.status())) {
            throw new BusinessException("结算单「" + record.merchant() + "」当前状态不支持"
                    + action + "：" + record.status());
        }
    }

    private SettlementRecord withStatus(SettlementRecord record, String status) {
        return new SettlementRecord(record.id(), record.merchant(), record.period(), record.orders(),
                record.amount(), record.commission(), status);
    }

    private List<FinanceOverviewVO.OrderRow> orderRows(List<CustomerOrder> orders) {
        return orders.stream()
                .map(o -> new FinanceOverviewVO.OrderRow(o.id(), o.orderNo(), o.customer(), o.type(),
                        o.amount(), o.status(), orderTone(o.status()), o.created(), o.note()))
                .toList();
    }

    /** 订单状态配映射：待支付/退款中 amber、已支付 green、支付异常 red、已退款/已关闭 slate。 */
    private String orderTone(String status) {
        return switch (status) {
            case CustomerOrder.STATUS_PAID -> "green";
            case CustomerOrder.STATUS_ABNORMAL -> "red";
            case CustomerOrder.STATUS_PENDING, CustomerOrder.STATUS_REFUNDING -> "amber";
            default -> "slate";
        };
    }

    private List<FinanceOverviewVO.SettlementRow> settlementRows(List<SettlementRecord> records) {
        return records.stream()
                .map(s -> new FinanceOverviewVO.SettlementRow(s.id(), s.merchant(), s.period(), s.orders(),
                        s.amount(), s.commission(), s.status(), settlementTone(s.status())))
                .toList();
    }

    /** 结算状态配映射：待对账 amber、已对账 blue、已结算 green。 */
    private String settlementTone(String status) {
        return switch (status) {
            case SettlementRecord.STATUS_PENDING -> "amber";
            case SettlementRecord.STATUS_RECONCILED -> "blue";
            default -> "green";
        };
    }

    private List<FinanceOverviewVO.InvoiceRow> invoiceRows(List<InvoiceApplication> invoices) {
        return invoices.stream()
                .map(i -> new FinanceOverviewVO.InvoiceRow(i.id(), i.applyNo(), i.applicant(), i.title(),
                        i.taxNo(), i.amount(), i.status(), invoiceTone(i.status()), i.applied()))
                .toList();
    }

    /** 发票状态配映射：待审核 amber、已开票 green、已驳回 red。 */
    private String invoiceTone(String status) {
        return switch (status) {
            case InvoiceApplication.STATUS_PENDING -> "amber";
            case InvoiceApplication.STATUS_ISSUED -> "green";
            default -> "red";
        };
    }

    /** 金额文案 "¥ 1,286.00" → 数值，无法解析时按 0 计。 */
    private static double parseAmount(String text) {
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

    /** 数值 → 金额文案（与台账 "¥ 1,286.00" 口径一致）。 */
    private static String formatYuan(double value) {
        return String.format("¥ %,.2f", value);
    }

    /** 收敛趋势迷你图（统一走 {@link TrendUtils}）。 */
    private static List<Double> trend(double current) {
        return TrendUtils.converge(current);
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

    /** 生成审计存证哈希（与既有日志口径一致）。 */
    @SuppressWarnings("unused")
    private static String hash() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
