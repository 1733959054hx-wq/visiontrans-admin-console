package com.gzu.adminconsole.jingchen.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 商户登录会话（模块自有表 {@code merchant_session}，落库持久化）。
 *
 * <p>与主工程的 {@code auth_session} 完全独立：商户令牌由
 * {@code MerchantSessionRepository} 解析，不进主工程的会话表，
 * 因此商户账号不会出现在后台的「管理员账号」列表中（参见 README 隔离设计）。
 *
 * <p>会话<b>落库</b>而非内存：后端重启不再注销商户，移动端 App 可保持登录；
 * 同一商户允许多端（web / android / ios）同时在线，各端一个 token 互不干扰。
 */
@Entity
@Table(name = "merchant_session")
public class MerchantSessionEntity {

    /** 会话令牌（与 admin 令牌同属 X-Auth-Token 头，由解析顺序区分）。 */
    @Id
    @Column(length = 64)
    private String token;

    private String merchantCode;
    private String merchantName;
    private String roleCode;
    private String roleName;

    /** 登录设备（web / android / ios / ...），多端会话互不干扰的标识。 */
    @Column(name = "device", length = 32)
    private String device;

    private LocalDateTime expireAt;

    public MerchantSessionEntity() {
    }

    public MerchantSessionEntity(String token, String merchantCode, String merchantName, String roleCode,
                                String roleName, LocalDateTime expireAt) {
        this(token, merchantCode, merchantName, roleCode, roleName, expireAt, "web");
    }

    public MerchantSessionEntity(String token, String merchantCode, String merchantName, String roleCode,
                                String roleName, LocalDateTime expireAt, String device) {
        this.token = token;
        this.merchantCode = merchantCode;
        this.merchantName = merchantName;
        this.roleCode = roleCode;
        this.roleName = roleName;
        this.expireAt = expireAt;
        this.device = device;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getMerchantCode() {
        return merchantCode;
    }

    public void setMerchantCode(String merchantCode) {
        this.merchantCode = merchantCode;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public LocalDateTime getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(LocalDateTime expireAt) {
        this.expireAt = expireAt;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }
}
