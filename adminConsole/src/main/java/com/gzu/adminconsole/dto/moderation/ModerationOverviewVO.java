package com.gzu.adminconsole.dto.moderation;

import java.util.List;

import com.gzu.adminconsole.dto.common.KpiMetric;

/**
 * 语种术语库审核与 UGC 风控中台视图模型（对应页面 a8）。
 */
public record ModerationOverviewVO(List<KpiMetric> kpis,
                                   List<KanbanColumn> kanban,
                                   List<MaterialRow> assets,
                                   UgcCard ugc,
                                   RefundCard refund) {

    /** 审核看板列。 */
    public record KanbanColumn(String title, String countLabel, String tone, List<KanbanCard> cards) {
    }

    /**
     * 看板卡片（术语包任务）。
     *
     * @param id 主键，前端编辑 / 移动 / 删除时回传
     */
    public record KanbanCard(Long id, String title, String priority, String meta, String owner, String due) {
    }

    /**
     * AR 广告素材机审置信度行。
     *
     * @param id 主键，前端编辑 / 删除时回传
     */
    public record MaterialRow(Long id, String name, String confidence, String verdict, String tone) {
    }

    /** UGC 违规片段卡。 */
    public record UgcCard(String id,
                          String title,
                          String level,
                          String levelTone,
                          List<UgcSegment> segments,
                          List<UgcRule> rules,
                          String verdict) {
    }

    /** UGC 文本片段（用于原位高亮渲染）。 */
    public record UgcSegment(String text, String tone) {
    }

    /** 命中的风控规则。 */
    public record UgcRule(String name, double score) {
    }

    /** 退款仲裁工单。 */
    public record RefundCard(String orderNo,
                             String paid,
                             String suggestRefund,
                             String advice,
                             String sla,
                             String reasons,
                             String status) {
    }
}
