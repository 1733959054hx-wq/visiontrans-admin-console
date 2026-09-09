package com.gzu.adminconsole.dto.ads;

import java.util.List;

import com.gzu.adminconsole.dto.common.KpiMetric;
import com.gzu.adminconsole.dto.common.ToggleItem;

/**
 * 全网广告位排期与调度引擎视图模型（对应页面 a9）。
 */
public record AdOverviewVO(List<KpiMetric> kpis,
                           List<String> days,
                           List<AdSlotRow> slots,
                           List<FrequencyCapRow> frequency,
                           List<ToggleItem> freqStrategies,
                           EcpmMatrix matrix,
                           AdviceCard advice) {

    /** 广告位库存甘特行。 */
    public record AdSlotRow(Long id, String name, String status, String color, String remain, String remainTone,
                            List<Boolean> cells, boolean online) {
    }

    /** 单用户频次限制项。 */
    public record FrequencyCapRow(String name, String display, int max, int value) {
    }

    /** 场景 × 语种 eCPM 策略矩阵。 */
    public record EcpmMatrix(List<String> scenes, List<String> langs, List<List<Integer>> values, int max) {
    }

    /** AI 调优建议。 */
    public record AdviceCard(String text, boolean adopted) {
    }
}
