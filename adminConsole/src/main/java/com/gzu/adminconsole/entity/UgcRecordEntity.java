package com.gzu.adminconsole.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

/**
 * UGC 违规文本记录（片段与命中规则以集合表保存，保持顺序）。
 */
@Entity
@Table(name = "ugc_record")
public class UgcRecordEntity {

    @Id
    @Column(name = "record_id", length = 64)
    private String id;

    @Column(name = "level", length = 32)
    private String level;

    @Column(name = "verdict", length = 32)
    private String verdict;

    @ElementCollection
    @CollectionTable(name = "ugc_segment", joinColumns = @JoinColumn(name = "record_id"))
    @OrderColumn(name = "sort_order")
    private List<UgcSegmentEmb> segments = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "ugc_hit", joinColumns = @JoinColumn(name = "record_id"))
    @OrderColumn(name = "sort_order")
    private List<UgcHitEmb> hits = new ArrayList<>();

    protected UgcRecordEntity() {
    }

    public UgcRecordEntity(String id, String level, String verdict) {
        this.id = id;
        this.level = level;
        this.verdict = verdict;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getVerdict() { return verdict; }
    public void setVerdict(String verdict) { this.verdict = verdict; }
    public List<UgcSegmentEmb> getSegments() { return segments; }
    public void setSegments(List<UgcSegmentEmb> segments) { this.segments = segments; }
    public List<UgcHitEmb> getHits() { return hits; }
    public void setHits(List<UgcHitEmb> hits) { this.hits = hits; }
}
