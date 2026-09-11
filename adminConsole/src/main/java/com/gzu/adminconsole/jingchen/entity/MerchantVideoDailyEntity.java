package com.gzu.adminconsole.jingchen.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 商户视频日播放统计实体（jingchen 模块，业务表 merchant_video_daily）。
 *
 * <p>按「视频 × 日期」逐日记录播放量 / 平均观看时长 / 完播率,
 * 地域取录制当日视频的授权区域;供视频播放统计页做趋势 / 对比 / 地域分布分析。
 */
@Entity
@Table(name = "merchant_video_daily",
        uniqueConstraints = @UniqueConstraint(columnNames = {"video_id", "stat_date"}))
public class MerchantVideoDailyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属视频 id(merchant_video.id)。 */
    @Column(name = "video_id", nullable = false)
    private Long videoId;

    /** 统计日期(yyyy-MM-dd)。 */
    @Column(name = "stat_date", length = 10, nullable = false)
    private String statDate;

    /** 当日视频授权区域(如 亚太 / 全球),便于直接做地域分布。 */
    @Column(name = "region", length = 32)
    private String region;

    /** 当日播放量(次)。 */
    @Column(name = "plays")
    private Long plays;

    /** 当日平均观看时长(秒)。 */
    @Column(name = "watch_sec")
    private Long watchSec;

    /** 当日完播率(%),无播放时为 null。 */
    @Column(name = "finish_rate", precision = 5, scale = 2)
    private BigDecimal finishRate;

    protected MerchantVideoDailyEntity() {
    }

    public MerchantVideoDailyEntity(Long videoId, String statDate, String region,
                                    Long plays, Long watchSec, BigDecimal finishRate) {
        this.videoId = videoId;
        this.statDate = statDate;
        this.region = region;
        this.plays = plays;
        this.watchSec = watchSec;
        this.finishRate = finishRate;
    }

    public Long getId() { return id; }
    public Long getVideoId() { return videoId; }
    public String getStatDate() { return statDate; }
    public String getRegion() { return region; }
    public Long getPlays() { return plays; }
    public Long getWatchSec() { return watchSec; }
    public BigDecimal getFinishRate() { return finishRate; }
}
