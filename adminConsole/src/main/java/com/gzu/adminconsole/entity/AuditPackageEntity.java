package com.gzu.adminconsole.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 语种包 / 课程知识包审核记录（审核中心扩展看板）。
 */
@Entity
@Table(name = "audit_package")
public class AuditPackageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 包类型：语种包 / 课程知识包。 */
    @Column(name = "package_type", length = 16)
    private String type;

    @Column(name = "package_name", length = 128)
    private String name;

    /** 来源：端侧团队 / 众包平台等。 */
    @Column(name = "source", length = 64)
    private String source;

    @Column(name = "meta", length = 128)
    private String meta;

    /** 状态：待审核 / 已发布 / 已驳回。 */
    @Column(name = "package_status", length = 16)
    private String status;

    @Column(name = "sort_order")
    private int sortOrder;

    protected AuditPackageEntity() {
    }

    public AuditPackageEntity(String type, String name, String source, String meta, String status,
                              int sortOrder) {
        this.type = type;
        this.name = name;
        this.source = source;
        this.meta = meta;
        this.status = status;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getMeta() { return meta; }
    public void setMeta(String meta) { this.meta = meta; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
