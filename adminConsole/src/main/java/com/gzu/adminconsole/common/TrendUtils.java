package com.gzu.adminconsole.common;

import java.util.ArrayList;
import java.util.List;

/**
 * KPI 趋势计算工具：收敛迷你图、环比百分比与涨跌方向。
 *
 * <p>原先在 Cluster / Ad / Model / Moderation / Security 五个 Service 中各写了一份完全相同的
 * 私有实现，抽到此处统一口径，避免"同一页面不同卡片趋势算法不一致"。</p>
 */
public final class TrendUtils {

    /** 收敛曲线比例序列：由 58% 平滑收敛到 100%。 */
    private static final double[] RATIOS = {0.58, 0.64, 0.70, 0.75, 0.80, 0.85, 0.89, 0.92, 0.95, 0.97, 0.99, 1.0};

    private TrendUtils() {
    }

    /** 由当前值生成一条收敛到该值的趋势迷你图，用于无时间维度数据（如台账计数）的兜底。 */
    public static List<Double> converge(double current) {
        List<Double> out = new ArrayList<>();
        for (double ratio : RATIOS) {
            out.add(Math.round(current * ratio * 100.0) / 100.0);
        }
        return out;
    }

    /** 由趋势序列首尾计算环比百分比文案。 */
    public static String deltaOf(List<Double> series) {
        if (series.size() < 2) {
            return "0.0%";
        }
        double first = series.get(0);
        double last = series.get(series.size() - 1);
        if (first <= 0) {
            return "0.0%";
        }
        return String.format("%.1f%%", (last - first) * 100.0 / first);
    }

    /** 趋势方向：末尾值不低于起点即视为上升。 */
    public static boolean rising(List<Double> series) {
        return series.size() < 2 || series.get(series.size() - 1) >= series.get(0);
    }
}
