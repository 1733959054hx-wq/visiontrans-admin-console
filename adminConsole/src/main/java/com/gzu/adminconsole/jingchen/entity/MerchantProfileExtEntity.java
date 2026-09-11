package com.gzu.adminconsole.jingchen.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 商户资料扩展表（jingchen 模块，业务表 merchant_profile_ext）。
 *
 * <p>账户管理-基本信息维护:存放结算账户等商户扩展资料,
 * 主字段(展示名 / 联系人 / 电话)仍落在主工程 merchant_account 表。
 */
@Entity
@Table(name = "merchant_profile_ext")
public class MerchantProfileExtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 商户编码(= merchant_account.username,唯一)。 */
    @Column(name = "merchant_code", length = 64)
    private String merchantCode;

    /** 结算账户摘要,如 工行(****8821)。 */
    @Column(name = "settle_account", length = 64)
    private String settleAccount;

    protected MerchantProfileExtEntity() {
    }

    public MerchantProfileExtEntity(String merchantCode, String settleAccount) {
        this.merchantCode = merchantCode;
        this.settleAccount = settleAccount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMerchantCode() { return merchantCode; }
    public void setMerchantCode(String merchantCode) { this.merchantCode = merchantCode; }
    public String getSettleAccount() { return settleAccount; }
    public void setSettleAccount(String settleAccount) { this.settleAccount = settleAccount; }
}
