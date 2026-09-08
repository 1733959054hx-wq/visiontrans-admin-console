package com.gzu.adminconsole.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 管理员操作日志（追加写入 + 哈希链存证，不可篡改；id 越大越新）。
 */
@Entity
@Table(name = "audit_log")
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "log_time", length = 32)
    private String time;

    @Column(name = "operator", length = 32)
    private String operator;

    @Column(name = "role_name", length = 32)
    private String role;

    @Column(name = "group_name", length = 32)
    private String group;

    @Column(name = "action", length = 64)
    private String action;

    @Column(name = "detail", length = 512)
    private String detail;

    @Column(name = "source", length = 64)
    private String source;

    @Column(name = "result", length = 16)
    private String result;

    @Column(name = "hash", length = 32)
    private String hash;

    protected AuditLogEntity() {
    }

    public AuditLogEntity(String time, String operator, String role, String group, String action,
                          String detail, String source, String result, String hash) {
        this.time = time;
        this.operator = operator;
        this.role = role;
        this.group = group;
        this.action = action;
        this.detail = detail;
        this.source = source;
        this.result = result;
        this.hash = hash;
    }

    public Long getId() { return id; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }
}
