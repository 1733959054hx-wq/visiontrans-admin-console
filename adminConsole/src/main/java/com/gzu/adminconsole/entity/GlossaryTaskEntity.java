package com.gzu.adminconsole.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 多语种术语包审核任务（看板卡片）。
 */
@Entity
@Table(name = "glossary_task")
public class GlossaryTaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", length = 128)
    private String title;

    @Column(name = "priority", length = 8)
    private String priority;

    @Column(name = "meta", length = 128)
    private String meta;

    @Column(name = "owner", length = 32)
    private String owner;

    @Column(name = "due", length = 32)
    private String due;

    @Column(name = "column_name", length = 32)
    private String columnName;

    @Column(name = "sort_order")
    private int sortOrder;

    protected GlossaryTaskEntity() {
    }

    public GlossaryTaskEntity(String title, String priority, String meta, String owner, String due,
                              String columnName, int sortOrder) {
        this.title = title;
        this.priority = priority;
        this.meta = meta;
        this.owner = owner;
        this.due = due;
        this.columnName = columnName;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getMeta() { return meta; }
    public void setMeta(String meta) { this.meta = meta; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getDue() { return due; }
    public void setDue(String due) { this.due = due; }
    public String getColumnName() { return columnName; }
    public void setColumnName(String columnName) { this.columnName = columnName; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
