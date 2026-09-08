package com.gzu.adminconsole.jingchen.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.gzu.adminconsole.jingchen.entity.MerchantDailyStatEntity;
import com.gzu.adminconsole.jingchen.repository.MerchantStatRepository;

/**
 * 商户经营日统计演示数据初始化（jingchen 模块）。
 *
 * <p>首次启动且 merchant_daily_stat 表为空时,灌入近 14 天经营数据(带趋势起伏),
 * 便于经营概览页直接看到折线/柱状效果;已有数据一律不覆盖。
 */
@Component
@Order(220)
public class MerchantStatDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MerchantStatDataInitializer.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 近 14 天曝光量基准(万次)与波动幅度,保证趋势有起伏不呆板。 */
    private static final long[] EXPOSURE_WAN = {62, 66, 64, 71, 74, 78, 86, 83, 88, 92, 89, 95, 99, 104};
    private static final double[] CLICK_WAN = {3.1, 3.4, 3.2, 3.8, 4.0, 4.2, 4.9, 4.6, 5.0, 5.4, 5.2, 5.7, 6.0, 6.4};

    private final MerchantStatRepository repository;

    public MerchantStatDataInitializer(MerchantStatRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        LocalDate base = LocalDate.now().minusDays(EXPOSURE_WAN.length - 1);
        for (int i = 0; i < EXPOSURE_WAN.length; i++) {
            long exposure = EXPOSURE_WAN[i] * 10_000;
            long click = Math.round(CLICK_WAN[i] * 10_000);
            BigDecimal consume = new BigDecimal(8_000 + EXPOSURE_WAN[i] * 1_200 + (i % 3) * 3_100);
            BigDecimal gmv = new BigDecimal(5_000 + EXPOSURE_WAN[i] * 780 + (i % 2) * 2_400);
            repository.save(new MerchantDailyStatEntity(
                    base.plusDays(i).format(FMT), exposure, click, consume, gmv));
        }
        log.info("[商户端初始化] merchant_daily_stat 已灌入近 {} 天经营数据", EXPOSURE_WAN.length);
    }
}
