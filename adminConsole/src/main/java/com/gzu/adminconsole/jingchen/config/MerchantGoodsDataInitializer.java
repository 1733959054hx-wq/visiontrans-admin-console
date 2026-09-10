package com.gzu.adminconsole.jingchen.config;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.gzu.adminconsole.jingchen.entity.MerchantGoodsEntity;
import com.gzu.adminconsole.jingchen.repository.MerchantGoodsRepository;

/**
 * 商户商品演示数据初始化（jingchen 模块）。
 * 表空时灌入 6 个商品(与订单表 goodsName 呼应);已有数据一律不覆盖。
 */
@Component
@Order(226)
public class MerchantGoodsDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MerchantGoodsDataInitializer.class);

    private final MerchantGoodsRepository repository;

    public MerchantGoodsDataInitializer(MerchantGoodsRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        repository.save(goods("出境医疗急救术语包", "专业词典", "医患场景高频急救术语,EN→ZH 双向对照", "68", 100, 18432L, "在售"));
        repository.save(goods("日本交通标志实景微课", "视频课程", "12 讲实景微课,涵盖 200+ 交通标志", "128", 80, 15908L, "在售"));
        repository.save(goods("中东商务礼仪文化包", "文化包", "清真商务礼仪 / 阿语敬语全覆盖", "88", 85, 9631L, "在售"));
        repository.save(goods("跨境电商标题优化课", "视频课程", "商品标题多语种优化实战", "45", 80, 7286L, "在售"));
        repository.save(goods("东南亚旅行实用会话包", "文化包", "泰 / 越 / 马来三语场景会话", "38", 80, 6905L, "在售"));
        repository.save(goods("国际学术会议同传术语包", "专业词典", "IEEE / 医学 / 法律三大领域术语", "268", 100, 3142L, "已下架"));
        log.info("[商户端初始化] merchant_goods 已灌入 6 个商品");
    }

    private MerchantGoodsEntity goods(String name, String type, String desc, String price,
                                      int discount, long sales, String status) {
        BigDecimal priceValue = new BigDecimal(price);
        BigDecimal sale = priceValue.multiply(BigDecimal.valueOf(discount))
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        return new MerchantGoodsEntity(name, type, desc, priceValue, discount, sale, status, sales);
    }
}
