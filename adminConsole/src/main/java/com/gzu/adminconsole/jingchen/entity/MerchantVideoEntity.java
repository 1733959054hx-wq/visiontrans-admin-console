package com.gzu.adminconsole.jingchen.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 商户视频接入实体（jingchen 模块，业务表 merchant_video）。
 *
 * <p>视频管理:上传档案、字幕语言、播放数据与授权信息统一在此表维护。
 * 演示数据由 {@code MerchantVideoDataInitializer} 灌入。
 */
@Entity
@Table(name = "merchant_video")
public class MerchantVideoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 视频文件名,如 JP_Tokyo_Transit_4K_Master.mp4。 */
    @Column(name = "video_name", length = 160)
    private String name;

    /** 时长(秒)。 */
    @Column(name = "duration_sec")
    private Long durationSec;

    /** 文件大小(GB,两位小数)。 */
    @Column(name = "size_gb", precision = 6, scale = 2)
    private BigDecimal sizeGb;

    /** 语种,如 JA·JP。 */
    @Column(name = "video_lang", length = 16)
    private String lang;

    /** 状态:转码中 / 已就绪 / 已上架。 */
    @Column(name = "video_status", length = 16)
    private String status;

    /** 播放量(次)。 */
    @Column(name = "plays")
    private Long plays;

    /** 完播率(%),可为空。 */
    @Column(name = "finish_rate", precision = 5, scale = 2)
    private BigDecimal finishRate;

    /** 授权区域,如 亚太 / 全球。 */
    @Column(name = "region", length = 32)
    private String region;

    /** 已绑定字幕语言,逗号分隔,如 中,英,日。 */
    @Column(name = "subtitle_langs", length = 64)
    private String subtitleLangs;

    /** 上传文件原始名。 */
    @Column(name = "file_name", length = 128)
    private String fileName;

    /** 落盘文件访问地址(/merchant/files/{uuid}.ext),演示档案无实体文件时为 null。 */
    @Column(name = "file_url", length = 255)
    private String fileUrl;

    protected MerchantVideoEntity() {
    }

    public MerchantVideoEntity(String name, Long durationSec, BigDecimal sizeGb, String lang, String status,
                               Long plays, BigDecimal finishRate, String region, String subtitleLangs) {
        this(name, durationSec, sizeGb, lang, status, plays, finishRate, region, subtitleLangs, null, null);
    }

    public MerchantVideoEntity(String name, Long durationSec, BigDecimal sizeGb, String lang, String status,
                               Long plays, BigDecimal finishRate, String region, String subtitleLangs,
                               String fileName, String fileUrl) {
        this.name = name;
        this.durationSec = durationSec;
        this.sizeGb = sizeGb;
        this.lang = lang;
        this.status = status;
        this.plays = plays;
        this.finishRate = finishRate;
        this.region = region;
        this.subtitleLangs = subtitleLangs;
        this.fileName = fileName;
        this.fileUrl = fileUrl;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getDurationSec() { return durationSec; }
    public void setDurationSec(Long durationSec) { this.durationSec = durationSec; }
    public BigDecimal getSizeGb() { return sizeGb; }
    public void setSizeGb(BigDecimal sizeGb) { this.sizeGb = sizeGb; }
    public String getLang() { return lang; }
    public void setLang(String lang) { this.lang = lang; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getPlays() { return plays; }
    public void setPlays(Long plays) { this.plays = plays; }
    public BigDecimal getFinishRate() { return finishRate; }
    public void setFinishRate(BigDecimal finishRate) { this.finishRate = finishRate; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getSubtitleLangs() { return subtitleLangs; }
    public void setSubtitleLangs(String subtitleLangs) { this.subtitleLangs = subtitleLangs; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
}
