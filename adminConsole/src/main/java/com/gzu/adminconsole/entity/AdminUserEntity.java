package com.gzu.adminconsole.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 后台管理员账号（含登录凭据）。
 *
 * <p>passwordHash 永不对外输出：AdminUser 记录中不包含该字段。</p>
 */
@Entity
@Table(name = "admin_user")
public class AdminUserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_name", length = 32)
    private String name;

    @Column(name = "role_name", length = 32)
    private String role;

    @Column(name = "group_name", length = 32)
    private String group;

    @Column(name = "phone", length = 32)
    private String phone;

    @Column(name = "user_status", length = 16)
    private String status;

    @Column(name = "last_login", length = 32)
    private String lastLogin;

    @Column(name = "username", length = 64, unique = true)
    private String username;

    @Column(name = "password_hash", length = 128)
    private String passwordHash;

    protected AdminUserEntity() {
    }

    public AdminUserEntity(String name, String role, String group, String phone, String status,
                           String lastLogin, String username, String passwordHash) {
        this.name = name;
        this.role = role;
        this.group = group;
        this.phone = phone;
        this.status = status;
        this.lastLogin = lastLogin;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getLastLogin() { return lastLogin; }
    public void setLastLogin(String lastLogin) { this.lastLogin = lastLogin; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}
