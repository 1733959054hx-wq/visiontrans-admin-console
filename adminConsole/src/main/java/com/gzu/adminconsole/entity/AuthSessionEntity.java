package com.gzu.adminconsole.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 管理员登录会话（令牌有效期 8 小时，登出即删除）。
 */
@Entity
@Table(name = "auth_session")
public class AuthSessionEntity {

    @Id
    @Column(name = "token", length = 64)
    private String token;

    @Column(name = "username", length = 64)
    private String username;

    @Column(name = "user_name", length = 32)
    private String name;

    @Column(name = "role_code", length = 32)
    private String roleCode;

    @Column(name = "role_name", length = 32)
    private String roleName;

    @Column(name = "group_name", length = 32)
    private String groupName;

    @Column(name = "expire_at")
    private LocalDateTime expireAt;

    protected AuthSessionEntity() {
    }

    public AuthSessionEntity(String token, String username, String name, String roleCode, String roleName,
                             String groupName, LocalDateTime expireAt) {
        this.token = token;
        this.username = username;
        this.name = name;
        this.roleCode = roleCode;
        this.roleName = roleName;
        this.groupName = groupName;
        this.expireAt = expireAt;
    }

    public String getToken() { return token; }
    public String getUsername() { return username; }
    public String getName() { return name; }
    public String getRoleCode() { return roleCode; }
    public String getRoleName() { return roleName; }
    public String getGroupName() { return groupName; }
    public LocalDateTime getExpireAt() { return expireAt; }
    public boolean isExpired() { return expireAt == null || expireAt.isBefore(LocalDateTime.now()); }
}
