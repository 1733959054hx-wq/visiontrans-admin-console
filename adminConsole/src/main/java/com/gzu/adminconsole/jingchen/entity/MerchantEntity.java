package com.gzu.adminconsole.jingchen.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 商户档案（模块自有表 {@code merchant_account}）。
 *
 * <p>只存放商户自身的业务档案与登录凭据，不复用后台管理的 admin_user：
 * 登录身份（令牌 / 会话）由本模块的 {@code merchant_session} 表维护，本表通过 code 与之关联。
 */
@Entity
@Table(name = "merchant_account")
public class MerchantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 商户编码，与登录账号一致。 */
    @Column(nullable = false, unique = true, length = 64)
    private String code;

    /** 商户名称。 */
    @Column(length = 64)
    private String name;

    /** 联系人。 */
    @Column(length = 64)
    private String contact;

    /** 联系电话（脱敏展示）。 */
    @Column(length = 32)
    private String phone;

    /** 状态：启用 / 停用。 */
    @Column(length = 16)
    private String status;

    /** 入驻时间。 */
    @Column(length = 32)
    private String createdAt;

    /** 登录口令哈希（模块自有，不复用 admin_user 的凭据）。 */
    @Column(name = "password_hash", length = 128)
    private String passwordHash;

    /** 最近登录时间。 */
    @Column(name = "last_login", length = 32)
    private String lastLogin;

    public MerchantEntity() {
    }

    public MerchantEntity(String code, String name, String contact, String phone, String status, String createdAt) {
        this.code = code;
        this.name = name;
        this.contact = contact;
        this.phone = phone;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(String lastLogin) {
        this.lastLogin = lastLogin;
    }
}
