package com.gzu.adminconsole.jingchen.config;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.gzu.adminconsole.jingchen.entity.MerchantVideoDailyEntity;
import com.gzu.adminconsole.jingchen.entity.MerchantVideoEntity;
import com.gzu.adminconsole.jingchen.repository.MerchantVideoDailyRepository;
import com.gzu.adminconsole.jingchen.repository.MerchantVideoRepository;

/**
 * 商户视频日播放统计演示数据初始化（jingchen 模块）。
 *
 * <p>首次启动且 merchant_video_daily 表为空时,按现有视频档案近 14 天
 * 拆分播放量(带自然起伏),观看时长由时长×完播率推算;已有数据一律不覆盖。
 */
@Component
@Order(225)
public class MerchantVideoDailyDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MerchantVideoDailyDataInitializer.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int DAYS = 14;

    /** 14 天流量权重(周末略高),保证趋势有起伏不呆板。 */
    private static final double[] WEIGHTS = {0.72, 0.81, 0.68, 0.95, 1.04, 1.18, 1.12,
            0.76, 0.88, 0.83, 1.02, 1.15, 1.22, 1.16};

    private final MerchantVideoDailyRepository repository;
    private final MerchantVideoRepository videoRepository;

    public MerchantVideoDailyDataInitializer(MerchantVideoDailyRepository repository,
                                             MerchantVideoRepository videoRepository) {
        this.repository = repository;
        this.videoRepository = videoRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        LocalDate base = LocalDate.now().minusDays(DAYS - 1);
        double weightSum = 0;
        for (double w : WEIGHTS) {
            weightSum += w;
        }
        int rows = 0;
        for (MerchantVideoEntity v : videoRepository.findAll()) {
            long lifetime = v.getPlays() == null ? 0 : v.getPlays();
            // 近 14 天约占累计播放的 45%,逐日按权重拆分
            long recent = Math.round(lifetime * 0.45);
            BigDecimal rate = v.getFinishRate();
            for (int i = 0; i < DAYS; i++) {
                long plays = Math.round(recent * WEIGHTS[i] / weightSum);
                if (plays <= 0) {
                    continue;
                }
                BigDecimal dailyRate = null;
                Long watchSec = null;
                if (rate != null && v.getDurationSec() != null && v.getDurationSec() > 0) {
                    // 完播率逐日小幅波动(±2 个百分点),观看时长=时长×完播率
                    double jitter = ((i * 7) % 5 - 2) * 0.6;
                    double r = Math.max(5, Math.min(95, rate.doubleValue() + jitter));
                    dailyRate = BigDecimal.valueOf(r).setScale(2, RoundingMode.HALF_UP);
                    watchSec = Math.round(v.getDurationSec() * r / 100.0);
                }
                repository.save(new MerchantVideoDailyEntity(v.getId(),
                        base.plusDays(i).format(FMT), v.getRegion(), plays, watchSec, dailyRate));
                rows++;
            }
        }
        log.info("[商户端初始化] merchant_video_daily 已灌入 {} 条视频日播放数据(近 {} 天)", rows, DAYS);
    }
}
