package com.gzu.adminconsole.jingchen.config;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.gzu.adminconsole.jingchen.entity.MerchantSubtitleEntity;
import com.gzu.adminconsole.jingchen.repository.MerchantSubtitleRepository;
import com.gzu.adminconsole.jingchen.repository.MerchantVideoRepository;

/**
 * 商户字幕演示数据初始化（jingchen 模块）。
 *
 * <p>首次启动且 merchant_subtitle 表为空时,按 merchant_video 现有档案的
 * subtitle_langs 逐语种建立字幕档案(演示档案无实体文件,file_url 为 null);
 * 已有数据一律不覆盖。
 */
@Component
@Order(224)
public class MerchantSubtitleDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MerchantSubtitleDataInitializer.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final MerchantSubtitleRepository repository;
    private final MerchantVideoRepository videoRepository;

    public MerchantSubtitleDataInitializer(MerchantSubtitleRepository repository,
                                           MerchantVideoRepository videoRepository) {
        this.repository = repository;
        this.videoRepository = videoRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        String now = LocalDateTime.now().format(FMT);
        int rows = 0;
        for (var video : videoRepository.findAll()) {
            String langs = video.getSubtitleLangs();
            if (langs == null || langs.isBlank()) {
                continue;
            }
            for (String lang : langs.split(",")) {
                String l = lang.trim();
                if (l.isEmpty()) {
                    continue;
                }
                String base = stripExt(video.getName());
                repository.save(new MerchantSubtitleEntity(video.getId(), l,
                        base + "_" + langCode(l) + ".srt", null, "SRT", now));
                rows++;
            }
        }
        log.info("[商户端初始化] merchant_subtitle 已按视频档案灌入 {} 条字幕", rows);
    }

    private static String stripExt(String name) {
        int dot = name == null ? -1 : name.lastIndexOf('.');
        return dot < 0 ? String.valueOf(name) : name.substring(0, dot);
    }

    private static String langCode(String lang) {
        return switch (lang) {
            case "中" -> "zh";
            case "英" -> "en";
            case "日" -> "ja";
            case "韩" -> "ko";
            case "法" -> "fr";
            case "德" -> "de";
            case "阿" -> "ar";
            default -> "xx";
        };
    }
}
