package com.gzu.adminconsole.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 系统运行日志（应用日志 / 错误日志 / 模型推理）。
 */
@Entity
@Table(name = "system_log")
public class SystemLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "log_time", length = 32)
    private String time;

    /** 级别：INFO / WARN / ERROR。 */
    @Column(name = "log_level", length = 8)
    private String level;

    /** 分类：应用日志 / 错误日志 / 模型推理。 */
    @Column(name = "category", length = 16)
    private String category;

    @Column(name = "source", length = 64)
    private String source;

    @Column(name = "message", length = 255)
    private String message;

    @Column(name = "sort_order")
    private int sortOrder;

    protected SystemLogEntity() {
    }

    public SystemLogEntity(String time, String level, String category, String source, String message,
                           int sortOrder) {
        this.time = time;
        this.level = level;
        this.category = category;
        this.source = source;
        this.message = message;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
