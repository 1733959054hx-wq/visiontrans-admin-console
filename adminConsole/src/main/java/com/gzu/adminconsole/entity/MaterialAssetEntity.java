package com.gzu.adminconsole.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * AR 广告素材机审记录。
 */
@Entity
@Table(name = "material_asset")
public class MaterialAssetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_name", length = 128)
    private String name;

    @Column(name = "confidence", length = 16)
    private String confidence;

    @Column(name = "verdict", length = 16)
    private String verdict;

    @Column(name = "sort_order")
    private int sortOrder;

    protected MaterialAssetEntity() {
    }

    public MaterialAssetEntity(String name, String confidence, String verdict, int sortOrder) {
        this.name = name;
        this.confidence = confidence;
        this.verdict = verdict;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }
    public String getVerdict() { return verdict; }
    public void setVerdict(String verdict) { this.verdict = verdict; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
