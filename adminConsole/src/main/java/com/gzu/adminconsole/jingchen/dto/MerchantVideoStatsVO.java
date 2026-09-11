package com.gzu.adminconsole.jingchen.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 视频播放统计视图（jingchen 模块）：近 14 天播放趋势 / 地域分布 / 单视频对比。
 */
public record MerchantVideoStatsVO(
        /** 统计天数（如 14）。 */
        int days,
        /** 统计日期序列（升序）。 */
        List<String> range,
        /** 区间总播放量。 */
        long totalPlays,
        /** 按播放量加权的平均完播率(%)。 */
        BigDecimal avgFinishRate,
        /** 日均播放量。 */
        long dailyPlays,
        /** 播放趋势。 */
        List<TrendPoint> trend,
        /** 地域分布（播放量降序）。 */
        List<RegionSlice> regions,
        /** 单视频播放对比（播放量降序）。 */
        List<VideoRow> videos) {

    /** 单日趋势点（finishRate 为播放量加权值,当日无播放为 null）。 */
    public record TrendPoint(String date, long plays, BigDecimal finishRate) {
    }

    /** 地域分布切片（share 为占总播放百分比,四舍五入整数）。 */
    public record RegionSlice(String region, long plays, int share) {
    }

    /** 单视频汇总行。 */
    public record VideoRow(Long videoId, String name, String status, String region,
                           long plays, BigDecimal finishRate, long watchSec) {
    }
}
