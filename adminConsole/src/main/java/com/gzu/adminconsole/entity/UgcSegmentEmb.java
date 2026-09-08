package com.gzu.adminconsole.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * UGC 文本片段（原位高亮渲染，tone 为 null / rose / amber）。
 */
@Embeddable
public class UgcSegmentEmb {

    @Column(name = "seg_text", length = 512)
    private String text;

    @Column(name = "tone", length = 16)
    private String tone;

    protected UgcSegmentEmb() {
    }

    public UgcSegmentEmb(String text, String tone) {
        this.text = text;
        this.tone = tone;
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getTone() { return tone; }
    public void setTone(String tone) { this.tone = tone; }
}
