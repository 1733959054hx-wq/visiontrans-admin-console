package com.gzu.adminconsole.jingchen.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.springframework.stereotype.Service;

import com.gzu.adminconsole.jingchen.dto.MerchantVideoStatsVO;
import com.gzu.adminconsole.jingchen.entity.MerchantVideoDailyEntity;
import com.gzu.adminconsole.jingchen.entity.MerchantVideoEntity;
import com.gzu.adminconsole.jingchen.repository.MerchantVideoDailyRepository;
import com.gzu.adminconsole.jingchen.repository.MerchantVideoRepository;

/**
 * 视频播放统计业务逻辑（模块自有）：把「视频 × 日」明细聚合成
 * 趋势 / 地域分布 / 单视频对比三张视图,完播率一律按当日播放量加权。
 */
@Service
public class MerchantVideoStatsService {

    private final MerchantVideoDailyRepository dailyRepository;
    private final MerchantVideoRepository videoRepository;

    public MerchantVideoStatsService(MerchantVideoDailyRepository dailyRepository,
                                     MerchantVideoRepository videoRepository) {
        this.dailyRepository = dailyRepository;
        this.videoRepository = videoRepository;
    }

    public MerchantVideoStatsVO stats() {
        List<MerchantVideoDailyEntity> rows = dailyRepository.findAll();
        Map<Long, MerchantVideoEntity> videoMap = new LinkedHashMap<>();
        for (MerchantVideoEntity v : videoRepository.findAll()) {
            videoMap.put(v.getId(), v);
        }

        TreeSet<String> dates = new TreeSet<>();
        for (MerchantVideoDailyEntity d : rows) {
            dates.add(d.getStatDate());
        }
        List<String> range = new ArrayList<>(dates);

        // 聚合累加器：plays / 完播率加权和(Σrate×plays) / 平均观看时长和(Σwatch)
        Map<String, Agg> byDate = new LinkedHashMap<>();
        Map<Long, Agg> byVideo = new LinkedHashMap<>();
        Map<String, Agg> byRegion = new LinkedHashMap<>();
        Agg total = new Agg();

        for (MerchantVideoDailyEntity d : rows) {
            long plays = d.getPlays() == null ? 0 : d.getPlays();
            long watch = d.getWatchSec() == null ? 0 : d.getWatchSec();
            BigDecimal rate = d.getFinishRate();
            Agg day = byDate.computeIfAbsent(d.getStatDate(), k -> new Agg());
            day.add(plays, watch, rate);
            Agg vid = byVideo.computeIfAbsent(d.getVideoId(), k -> new Agg());
            vid.add(plays, watch, rate);
            Agg region = byRegion.computeIfAbsent(regionOf(d, videoMap), k -> new Agg());
            region.add(plays, watch, rate);
            total.add(plays, watch, rate);
        }

        List<MerchantVideoStatsVO.TrendPoint> trend = range.stream()
                .map(date -> new MerchantVideoStatsVO.TrendPoint(date,
                        byDate.get(date).plays, byDate.get(date).weightedRate()))
                .toList();

        List<MerchantVideoStatsVO.RegionSlice> regions = byRegion.entrySet().stream()
                .map(e -> new MerchantVideoStatsVO.RegionSlice(e.getKey(), e.getValue().plays,
                        total.plays == 0 ? 0 : (int) Math.round(e.getValue().plays * 100.0 / total.plays)))
                .sorted((a, b) -> Long.compare(b.plays(), a.plays()))
                .toList();

        List<MerchantVideoStatsVO.VideoRow> videos = byVideo.entrySet().stream()
                .map(e -> {
                    MerchantVideoEntity v = videoMap.get(e.getKey());
                    return new MerchantVideoStatsVO.VideoRow(e.getKey(),
                            v == null ? "未知视频" : v.getName(),
                            v == null ? "" : v.getStatus(),
                            v == null ? "" : v.getRegion(),
                            e.getValue().plays,
                            e.getValue().weightedRate(),
                            e.getValue().avgWatch());
                })
                .sorted((a, b) -> Long.compare(b.plays(), a.plays()))
                .toList();

        long daily = range.isEmpty() ? 0 : total.plays / range.size();
        return new MerchantVideoStatsVO(range.size(), range, total.plays, total.weightedRate(), daily,
                trend, regions, videos);
    }

    /** 聚合累加器：播放量、完播率加权和(Σrate×plays)、观看时长和。 */
    private static final class Agg {
        private long plays;
        private long watchSum;
        private BigDecimal rateWeightedSum = BigDecimal.ZERO;

        void add(long plays, long watch, BigDecimal rate) {
            this.plays += plays;
            this.watchSum += watch;
            if (rate != null && plays > 0) {
                rateWeightedSum = rateWeightedSum.add(rate.multiply(BigDecimal.valueOf(plays)));
            }
        }

        BigDecimal weightedRate() {
            return plays == 0 ? null
                    : rateWeightedSum.divide(BigDecimal.valueOf(plays), 2, RoundingMode.HALF_UP);
        }

        long avgWatch() {
            return plays == 0 ? 0 : watchSum / plays;
        }
    }

    private static String regionOf(MerchantVideoDailyEntity d, Map<Long, MerchantVideoEntity> videoMap) {
        if (d.getRegion() != null && !d.getRegion().isBlank()) {
            return d.getRegion();
        }
        MerchantVideoEntity v = videoMap.get(d.getVideoId());
        return v == null || v.getRegion() == null ? "未知" : v.getRegion();
    }
}
