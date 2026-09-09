package com.gzu.adminconsole.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * B 端发票申请记录。
 */
@Entity
@Table(name = "invoice_apply")
public class InvoiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "apply_no", length = 32)
    private String applyNo;

    @Column(name = "applicant", length = 64)
    private String applicant;

    /** 发票抬头。 */
    @Column(name = "invoice_title", length = 128)
    private String title;

    @Column(name = "tax_no", length = 32)
    private String taxNo;

    @Column(name = "amount", length = 32)
    private String amount;

    /** 状态：待审核 / 已开票 / 已驳回。 */
    @Column(name = "invoice_status", length = 16)
    private String status;

    @Column(name = "applied", length = 32)
    private String applied;

    protected InvoiceEntity() {
    }

    public InvoiceEntity(String applyNo, String applicant, String title, String taxNo,
                         String amount, String status, String applied) {
        this.applyNo = applyNo;
        this.applicant = applicant;
        this.title = title;
        this.taxNo = taxNo;
        this.amount = amount;
        this.status = status;
        this.applied = applied;
    }

    public Long getId() { return id; }
    public String getApplyNo() { return applyNo; }
    public void setApplyNo(String applyNo) { this.applyNo = applyNo; }
    public String getApplicant() { return applicant; }
    public void setApplicant(String applicant) { this.applicant = applicant; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getTaxNo() { return taxNo; }
    public void setTaxNo(String taxNo) { this.taxNo = taxNo; }
    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getApplied() { return applied; }
    public void setApplied(String applied) { this.applied = applied; }
}
