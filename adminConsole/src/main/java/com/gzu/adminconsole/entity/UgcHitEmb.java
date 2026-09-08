package com.gzu.adminconsole.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * UGC 命中的风控规则。
 */
@Embeddable
public class UgcHitEmb {

    @Column(name = "rule_name", length = 64)
    private String name;

    @Column(name = "score")
    private double score;

    protected UgcHitEmb() {
    }

    public UgcHitEmb(String name, double score) {
        this.name = name;
        this.score = score;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
}
