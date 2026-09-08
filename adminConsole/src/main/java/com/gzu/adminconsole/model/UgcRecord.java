package com.gzu.adminconsole.model;

import java.util.List;

/**
 * UGC 违规文本记录（原位高亮拦截）。
 *
 * @param id       记录 ID
 * @param level    风险标签，如"高危导流"
 * @param segments 文本片段（携带高亮色调）
 * @param hits     命中的风控规则
 * @param verdict  处置结果：待处置 / 已放行 / 已封禁 / 复核中
 */
public record UgcRecord(String id, String level, List<Segment> segments, List<Hit> hits, String verdict) {

    /** 文本片段，tone 为 red / amber 时高亮，null 表示普通文本。 */
    public record Segment(String text, String tone) {
    }

    /** 命中的风控规则。 */
    public record Hit(String name, double score) {
    }
}
