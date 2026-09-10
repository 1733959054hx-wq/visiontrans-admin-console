package com.gzu.adminconsole.jingchen.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 商户提现申请实体（jingchen 模块，业务表 merchant_withdraw）。
 *
 * <p>提现管理:发起申请即冻结余额(生成一条提现支出流水);
 * 状态:审核中 / 已到账 / 已驳回。演示环境驳回 / 到账由管理员侧推进。
 */
@Entity
@Table(name = "merchant_withdraw")
public class MerchantWithdrawEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 提现单号,如 WD-20260903-001。 */
    @Column(name = "withdraw_no", length = 32)
    private String withdrawNo;

    /** 提现金额(元)。 */
    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    /** 状态:审核中 / 已到账 / 已驳回。 */
    @Column(name = "w_status", length = 16)
    private String status;

    /** 收款账户摘要。 */
    @Column(name = "account", length = 64)
    private String account;

    /** 申请时间(yyyy-MM-dd HH:mm)。 */
    @Column(name = "applied_at", length = 32)
    private String appliedAt;

    protected MerchantWithdrawEntity() {
    }

    public MerchantWithdrawEntity(String withdrawNo, BigDecimal amount, String status,
                                  String appliedAt, String account) {
        this.withdrawNo = withdrawNo;
        this.amount = amount;
        this.status = status;
        this.appliedAt = appliedAt;
        this.account = account;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getWithdrawNo() { return withdrawNo; }
    public void setWithdrawNo(String withdrawNo) { this.withdrawNo = withdrawNo; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAppliedAt() { return appliedAt; }
    public void setAppliedAt(String appliedAt) { this.appliedAt = appliedAt; }
    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }
}
