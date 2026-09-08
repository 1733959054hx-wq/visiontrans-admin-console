package com.gzu.adminconsole.dto.model;

import java.util.List;

import com.gzu.adminconsole.dto.common.KpiMetric;
import com.gzu.adminconsole.dto.common.ToggleItem;

/**
 * AI 模型生命周期与热更中心视图模型（对应页面 a7）。
 */
public record ModelOverviewVO(List<KpiMetric> kpis,
                              List<ModelRow> models,
                              List<TimelineRow> timeline,
                              GrayscaleCard grayscale,
                              List<ToggleItem> strategies) {

    /** 模型部署与灰度状态行。 */
    public record ModelRow(String name,
                           String type,
                           String precision,
                           String size,
                           String coverage,
                           int grayRatio,
                           String status,
                           String statusTone,
                           String icon,
                           String iconTone) {
    }

    /** 版本迭代与运维时间线。 */
    public record TimelineRow(String title, String time, String desc, String tone) {
    }

    /** 灰度下发策略卡。 */
    public record GrayscaleCard(int ratio, List<BatchRow> batches) {
    }

    /** 灰度批次。 */
    public record BatchRow(String name, String ratio, String status, String tone) {
    }
}
