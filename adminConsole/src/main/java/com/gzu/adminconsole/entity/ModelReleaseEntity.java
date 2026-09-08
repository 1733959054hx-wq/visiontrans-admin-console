package com.gzu.adminconsole.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * AI 模型发布版本（端侧量化模型 / 云端大模型统一纳管）。
 */
@Entity
@Table(name = "model_release")
public class ModelReleaseEntity {

    @Id
    @Column(name = "model_name", length = 128)
    private String name;

    @Column(name = "model_type", length = 64)
    private String type;

    @Column(name = "precision_type", length = 16)
    private String precision;

    @Column(name = "package_size", length = 16)
    private String size;

    @Column(name = "coverage", length = 16)
    private String coverage;

    @Column(name = "gray_ratio")
    private int grayRatio;

    @Column(name = "release_status", length = 32)
    private String status;

    protected ModelReleaseEntity() {
    }

    public ModelReleaseEntity(String name, String type, String precision, String size, String coverage,
                              int grayRatio, String status) {
        this.name = name;
        this.type = type;
        this.precision = precision;
        this.size = size;
        this.coverage = coverage;
        this.grayRatio = grayRatio;
        this.status = status;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getPrecision() { return precision; }
    public void setPrecision(String precision) { this.precision = precision; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public String getCoverage() { return coverage; }
    public void setCoverage(String coverage) { this.coverage = coverage; }
    public int getGrayRatio() { return grayRatio; }
    public void setGrayRatio(int grayRatio) { this.grayRatio = grayRatio; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
