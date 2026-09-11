package com.gzu.adminconsole.jingchen.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 商户视频字幕实体（jingchen 模块，业务表 merchant_subtitle）。
 *
 * <p>字幕从 merchant_video 的逗号文本字段独立成表,按视频逐语种维护:
 * 语言 / 字幕文件(SRT·VTT)/ 上传文件地址 / 格式与时间;
 * merchant_video.subtitle_langs 仅作冗余展示字段,由本表的增删自动回写。
 */
@Entity
@Table(name = "merchant_subtitle")
public class MerchantSubtitleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属视频 id(merchant_video.id)。 */
    @Column(name = "video_id", nullable = false)
    private Long videoId;

    /** 字幕语言,如 中 / 英 / 日。 */
    @Column(name = "lang", length = 32)
    private String lang;

    /** 字幕文件原始名,如 Tokyo_Transit_zh.srt。 */
    @Column(name = "file_name", length = 128)
    private String fileName;

    /** 落盘文件访问地址(/merchant/files/{uuid}.srt),未上传实体文件时为 null。 */
    @Column(name = "file_url", length = 255)
    private String fileUrl;

    /** 字幕格式:SRT / VTT。 */
    @Column(name = "sub_format", length = 8)
    private String format;

    /** 上传 / 更新时间(yyyy-MM-dd HH:mm)。 */
    @Column(name = "uploaded_at", length = 20)
    private String uploadedAt;

    protected MerchantSubtitleEntity() {
    }

    public MerchantSubtitleEntity(Long videoId, String lang, String fileName, String fileUrl,
                                  String format, String uploadedAt) {
        this.videoId = videoId;
        this.lang = lang;
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.format = format;
        this.uploadedAt = uploadedAt;
    }

    public Long getId() { return id; }
    public Long getVideoId() { return videoId; }
    public void setVideoId(Long videoId) { this.videoId = videoId; }
    public String getLang() { return lang; }
    public void setLang(String lang) { this.lang = lang; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public String getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(String uploadedAt) { this.uploadedAt = uploadedAt; }
}
