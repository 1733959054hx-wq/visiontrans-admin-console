package com.gzu.adminconsole.jingchen.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gzu.adminconsole.common.BusinessException;
import com.gzu.adminconsole.jingchen.dto.MerchantSubtitleRequest;
import com.gzu.adminconsole.jingchen.entity.MerchantSubtitleEntity;
import com.gzu.adminconsole.jingchen.entity.MerchantVideoEntity;
import com.gzu.adminconsole.jingchen.repository.MerchantSubtitleRepository;
import com.gzu.adminconsole.jingchen.repository.MerchantVideoRepository;

/**
 * 商户视频字幕业务逻辑（模块自有）：按视频逐语种维护字幕档案,
 * 增删改后自动回写 merchant_video.subtitle_langs 冗余字段保持老页面兼容。
 */
@Service
public class MerchantSubtitleService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final MerchantSubtitleRepository repository;
    private final MerchantVideoRepository videoRepository;
    private final MerchantFileService fileService;

    public MerchantSubtitleService(MerchantSubtitleRepository repository, MerchantVideoRepository videoRepository,
                                   MerchantFileService fileService) {
        this.repository = repository;
        this.videoRepository = videoRepository;
        this.fileService = fileService;
    }

    public List<MerchantSubtitleEntity> list(Long videoId) {
        video(videoId);
        return repository.findByVideoId(videoId);
    }

    /** 新增字幕:视频必须存在,同视频同语种唯一;文件地址合法并回写格式与时间。 */
    @Transactional
    public MerchantSubtitleEntity create(Long videoId, MerchantSubtitleRequest req) {
        video(videoId);
        String lang = requireLang(req.lang());
        if (repository.findByVideoId(videoId).stream().anyMatch(s -> s.getLang().equals(lang))) {
            throw new BusinessException(400, "该视频已有「" + lang + "」字幕，请直接编辑或更换语种");
        }
        MerchantSubtitleEntity entity = new MerchantSubtitleEntity(videoId, lang,
                fileNameOf(req), req.fileUrl(), formatOf(req.fileUrl()),
                LocalDateTime.now().format(FMT));
        MerchantSubtitleEntity saved = repository.save(entity);
        syncVideoLangs(videoId);
        return saved;
    }

    /** 编辑字幕（换语种 / 补传或替换字幕文件）。 */
    @Transactional
    public MerchantSubtitleEntity update(Long videoId, Long id, MerchantSubtitleRequest req) {
        video(videoId);
        MerchantSubtitleEntity entity = repository.findById(id);
        if (entity == null || !entity.getVideoId().equals(videoId)) {
            throw new BusinessException(404, "字幕不存在: id=" + id);
        }
        if (req.lang() != null && !req.lang().isBlank()) {
            String lang = req.lang().trim();
            if (!lang.equals(entity.getLang()) && repository.findByVideoId(videoId).stream()
                    .anyMatch(s -> s.getLang().equals(lang))) {
                throw new BusinessException(400, "该视频已有「" + lang + "」字幕");
            }
            entity.setLang(lang);
        }
        if (req.fileUrl() != null && !req.fileUrl().isBlank()) {
            entity.setFileUrl(req.fileUrl());
            entity.setFileName(fileNameOf(req));
            entity.setFormat(formatOf(req.fileUrl()));
            entity.setUploadedAt(LocalDateTime.now().format(FMT));
        }
        MerchantSubtitleEntity saved = repository.save(entity);
        syncVideoLangs(videoId);
        return saved;
    }

    @Transactional
    public void delete(Long videoId, Long id) {
        video(videoId);
        MerchantSubtitleEntity entity = repository.findById(id);
        if (entity == null || !entity.getVideoId().equals(videoId)) {
            throw new BusinessException(404, "字幕不存在: id=" + id);
        }
        repository.delete(entity);
        syncVideoLangs(videoId);
    }

    /** 回写 merchant_video.subtitle_langs（冗余展示字段,逗号分隔按 id 序）。 */
    private void syncVideoLangs(Long videoId) {
        MerchantVideoEntity video = videoRepository.findById(videoId);
        if (video == null) {
            return;
        }
        List<String> langs = repository.langsOf(videoId);
        video.setSubtitleLangs(langs.isEmpty() ? "" : String.join(",", langs));
        videoRepository.save(video);
    }

    private MerchantVideoEntity video(Long videoId) {
        MerchantVideoEntity video = videoRepository.findById(videoId);
        if (video == null) {
            throw new BusinessException(404, "视频不存在: id=" + videoId);
        }
        return video;
    }

    private String requireLang(String lang) {
        if (lang == null || lang.isBlank()) {
            throw new BusinessException(400, "字幕语言必填");
        }
        return lang.trim();
    }

    /** 文件名:有落盘文件时取服务端校验过的原始名,否则沿用前端报名。 */
    private String fileNameOf(MerchantSubtitleRequest req) {
        if (req.fileUrl() != null && !req.fileUrl().isBlank() && req.fileName() != null) {
            return req.fileName().trim();
        }
        return req.fileName() == null ? "" : req.fileName().trim();
    }

    /** 字幕格式:按上传扩展名识别,识别不出按原文件名推断,缺省 SRT。 */
    private String formatOf(String fileUrl) {
        String name = fileUrl == null ? "" : fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "";
        return switch (ext.toLowerCase(Locale.ROOT)) {
            case "vtt" -> "VTT";
            case "srt" -> "SRT";
            default -> "SRT";
        };
    }
}
