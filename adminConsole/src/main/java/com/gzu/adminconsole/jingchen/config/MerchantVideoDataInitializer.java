package com.gzu.adminconsole.jingchen.config;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.gzu.adminconsole.jingchen.entity.MerchantVideoEntity;
import com.gzu.adminconsole.jingchen.repository.MerchantVideoRepository;

/**
 * 商户视频演示数据初始化（jingchen 模块）。
 * 表空时灌入 6 条视频档案;已有数据一律不覆盖。
 */
@Component
@Order(223)
public class MerchantVideoDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MerchantVideoDataInitializer.class);

    private final MerchantVideoRepository repository;

    public MerchantVideoDataInitializer(MerchantVideoRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        repository.save(video("JP_Tokyo_Transit_4K_Master.mp4", 2538L, "12.40", "JA·JP", "已上架",
                1_286_420L, "68.40", "亚太", "中,英,日"));
        repository.save(video("DE_Munich_Expo_Day1.mp4", 4565L, "21.86", "DE·DE", "已就绪",
                862_940L, "61.20", "欧洲", "中,德"));
        repository.save(video("FR_Paris_Art_Tour_EP03.mp4", 1727L, "6.14", "FR·FR", "已就绪",
                604_182L, "65.80", "全球", "中,法"));
        repository.save(video("EN_Medical_Onboarding_S01E02.mp4", 2120L, "7.92", "EN·US", "转码中",
                0L, null, "全球", "中,英"));
        repository.save(video("KR_Seoul_StreetFood_4K.mp4", 3156L, "15.30", "KO·KR", "已上架",
                728_510L, "59.40", "亚太", "中,韩"));
        repository.save(video("AR_Dubai_Business_Etiquette.mp4", 1148L, "4.05", "AR·AE", "已上架",
                196_835L, "54.10", "全球", "中,阿"));
        log.info("[商户端初始化] merchant_video 已灌入 6 条视频档案");
    }

    private MerchantVideoEntity video(String name, long dur, String size, String lang, String status,
                                      long plays, String rate, String region, String langs) {
        return new MerchantVideoEntity(name, dur, new BigDecimal(size), lang, status, plays,
                rate == null ? null : new BigDecimal(rate), region, langs);
    }
}
