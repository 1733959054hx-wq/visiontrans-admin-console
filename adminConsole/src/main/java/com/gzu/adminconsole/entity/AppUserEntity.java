package com.gzu.adminconsole.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * C 端用户账号（注册来源 / 会员状态 / 活跃度）。
 */
@Entity
@Table(name = "app_user")
public class AppUserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account", length = 64)
    private String account;

    /** 注册来源：手机号 / 微信 / QQ / Apple。 */
    @Column(name = "reg_source", length = 16)
    private String regSource;

    /** 会员状态：免费体验 / 会员月卡 / 会员年卡 / 会员过期。 */
    @Column(name = "membership", length = 16)
    private String membership;

    @Column(name = "registered", length = 32)
    private String registered;

    @Column(name = "last_active", length = 32)
    private String lastActive;

    /** 状态：正常 / 停用。 */
    @Column(name = "user_status", length = 16)
    private String status;

    protected AppUserEntity() {
    }

    public AppUserEntity(String account, String regSource, String membership, String registered,
                         String lastActive, String status) {
        this.account = account;
        this.regSource = regSource;
        this.membership = membership;
        this.registered = registered;
        this.lastActive = lastActive;
        this.status = status;
    }

    public Long getId() { return id; }
    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }
    public String getRegSource() { return regSource; }
    public void setRegSource(String regSource) { this.regSource = regSource; }
    public String getMembership() { return membership; }
    public void setMembership(String membership) { this.membership = membership; }
    public String getRegistered() { return registered; }
    public void setRegistered(String registered) { this.registered = registered; }
    public String getLastActive() { return lastActive; }
    public void setLastActive(String lastActive) { this.lastActive = lastActive; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
