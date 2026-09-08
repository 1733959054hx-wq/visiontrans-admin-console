package com.gzu.adminconsole.jingchen.config;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.gzu.adminconsole.jingchen.entity.MerchantChannelEntity;
import com.gzu.adminconsole.jingchen.repository.MerchantChannelRepository;

/**
 * 商户推广渠道演示数据初始化（jingchen 模块）。
 * 表空时灌入 4 个渠道;已有数据一律不覆盖。
 */
@Component
@Order(224)
public class MerchantPromoDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MerchantPromoDataInitializer.class);

    private final MerchantChannelRepository repository;

    public MerchantPromoDataInitializer(MerchantChannelRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        repository.save(ch("抖音内容号", "DOUYIN-20875", 25, 3_842L, 612L, "9142.00"));
        repository.save(ch("小红书达人", "XHS-10243", 20, 1_286L, 198L, "3260.00"));
        repository.save(ch("旅行社直客", "TRAVEL-8861", 15, 1_298L, 146L, "2210.00"));
        repository.save(ch("高校社团", "CAMPUS-3305", 12, 432L, 52L, "486.00"));
        log.info("[商户端初始化] merchant_channel 已灌入 4 个推广渠道");
    }

    private MerchantChannelEntity ch(String name, String code, int ratio, long click, long deal, String comm) {
        return new MerchantChannelEntity(name, code, ratio, click, deal, new BigDecimal(comm));
    }
}
