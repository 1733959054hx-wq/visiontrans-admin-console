package com.gzu.adminconsole.dto.finance;

import java.util.List;

import com.gzu.adminconsole.dto.common.KpiMetric;

/**
 * 财务订单视图模型（对应页面 a11）。
 */
public record FinanceOverviewVO(List<KpiMetric> kpis,
                                List<OrderRow> orders,
                                List<SettlementRow> settlements,
                                List<InvoiceRow> invoices,
                                List<OnboardingRow> onboardings) {

    /** 商户入驻申请行（资质提交 / 合同签署的后台审核）。 */
    public record OnboardingRow(Long id,
                                String applyNo,
                                String merchantName,
                                String licenseNo,
                                String contact,
                                String phone,
                                String qualification,
                                String contractStatus,
                                String contractTone,
                                String status,
                                String statusTone,
                                String submitted,
                                String reviewer,
                                String remark) {
    }

    /** C 端订单行。 */
    public record OrderRow(Long id,
                           String orderNo,
                           String customer,
                           String type,
                           String amount,
                           String status,
                           String statusTone,
                           String created,
                           String note) {
    }

    /** 商户结算行。 */
    public record SettlementRow(Long id,
                                String merchant,
                                String period,
                                long orders,
                                String amount,
                                String commission,
                                String status,
                                String statusTone) {
    }

    /** 发票申请行。 */
    public record InvoiceRow(Long id,
                             String applyNo,
                             String applicant,
                             String title,
                             String taxNo,
                             String amount,
                             String status,
                             String statusTone,
                             String applied) {
    }
}
