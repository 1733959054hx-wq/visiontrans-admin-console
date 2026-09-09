package com.gzu.adminconsole.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * C 端用户登录会话（对应需求「移除指定设备登录态」的作用对象）。
 *
 * <p>后台管理员会话在 {@code auth_session}，两者互不影响。
 */
@Entity
@Table(name = "app_session",
        indexes = {@Index(name = "idx_app_session_fp", columnList = "fingerprint")})
public class AppSessionEntity {

    @Id
    @Column(name = "token", length = 64)
    private String token;

    @Column(name = "account", length = 64)
    private String account;

    @Column(name = "fingerprint", length = 64)
    private String fingerprint;

    @Column(name = "login_at")
    private LocalDateTime loginAt;

    @Column(name = "expire_at")
    private LocalDateTime expireAt;

    protected AppSessionEntity() {
    }

    public AppSessionEntity(String token, String account, String fingerprint, LocalDateTime loginAt,
                            LocalDateTime expireAt) {
        this.token = token;
        this.account = account;
        this.fingerprint = fingerprint;
        this.loginAt = loginAt;
        this.expireAt = expireAt;
    }

    public String getToken() { return token; }
    public String getAccount() { return account; }
    public String getFingerprint() { return fingerprint; }
    public LocalDateTime getLoginAt() { return loginAt; }
    public LocalDateTime getExpireAt() { return expireAt; }
    public boolean isExpired() { return expireAt == null || expireAt.isBefore(LocalDateTime.now()); }
}
