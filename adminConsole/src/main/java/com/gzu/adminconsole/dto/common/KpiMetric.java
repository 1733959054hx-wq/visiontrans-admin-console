package com.gzu.adminconsole.dto.common;

import java.util.List;

/**
 * 通用 KPI 卡片视图模型（所有大盘页面的顶部指标卡复用）。
 *
 * @param label  指标名称
 * @param value  指标主数值（已格式化）
 * @param unit   数值单位后缀，可为 null
 * @param icon   FontAwesome 图标类名
 * @param c1     图标渐变起始色
 * @param c2     图标渐变结束色 / 迷你曲线颜色
 * @param delta  环比文案，可为 null（不展示角标）
 * @param up     环比是否上升（决定箭头方向），delta 为 null 时忽略
 * @param tone   角标配色：green / red / amber / blue / slate
 * @param note   辅助说明
 * @param spark  迷你曲线数据
 */
public record KpiMetric(String label,
                        String value,
                        String unit,
                        String icon,
                        String c1,
                        String c2,
                        String delta,
                        Boolean up,
                        String tone,
                        String note,
                        List<Double> spark) {
}
