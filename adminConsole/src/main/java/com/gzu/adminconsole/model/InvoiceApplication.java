package com.gzu.adminconsole.model;

/**
 * B 端发票申请。
 *
 * @param id        主键，新增时为 null
 * @param applyNo   申请单号
 * @param applicant 申请人 / 公司名
 * @param title     发票抬头
 * @param taxNo     纳税人识别号
 * @param amount    开票金额文案
 * @param status    状态：待审核 / 已开票 / 已驳回
 * @param applied   申请时间
 */
public record InvoiceApplication(Long id,
                                 String applyNo,
                                 String applicant,
                                 String title,
                                 String taxNo,
                                 String amount,
                                 String status,
                                 String applied) {

    /** 状态：待审核。 */
    public static final String STATUS_PENDING = "待审核";
    /** 状态：已开票。 */
    public static final String STATUS_ISSUED = "已开票";
    /** 状态：已驳回。 */
    public static final String STATUS_REJECTED = "已驳回";
}
