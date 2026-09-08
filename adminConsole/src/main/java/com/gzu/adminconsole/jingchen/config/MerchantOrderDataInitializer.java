package com.gzu.adminconsole.jingchen.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.gzu.adminconsole.jingchen.entity.MerchantOrderEntity;
import com.gzu.adminconsole.jingchen.repository.MerchantOrderRepository;

/**
 * 商户订单演示数据初始化（jingchen 模块）。
 *
 * <p>首次启动且 merchant_order 表为空时灌入 8 条订单(覆盖三种结算状态),
 * 便于订单与结算页直接演示「待结算 → 已结算」流转;已有数据一律不覆盖。
 */
@Component
@Order(221)
public class MerchantOrderDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MerchantOrderDataInitializer.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final MerchantOrderRepository repository;

    public MerchantOrderDataInitializer(MerchantOrderRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        repository.save(order("ORD-20260903-001", "出境医疗急救术语包", "抖音内容号", "168.00", "42.00", "待结算", now.minusHours(1)));
        repository.save(order("ORD-20260903-002", "日本交通标志实景微课", "小红书达人", "128.00", "32.00", "待结算", now.minusHours(3)));
        repository.save(order("ORD-20260903-003", "东京机场口岸 AR 导览", "自有门店", "88.00", "0.00", "已结算", now.minusHours(6)));
        repository.save(order("ORD-20260902-012", "中东商务礼仪文化包", "旅行社直客", "88.00", "22.00", "已结算", now.minusDays(1).withHour(16)));
        repository.save(order("ORD-20260902-008", "跨境电商标题优化课", "抖音内容号", "45.00", "11.25", "退款中", now.minusDays(1).withHour(10)));
        repository.save(order("ORD-20260901-006", "欧盟通关申报术语库", "跨境社群", "158.00", "39.50", "已结算", now.minusDays(2).withHour(15)));
        repository.save(order("ORD-20260901-003", "东南亚旅行实用会话包", "小红书达人", "38.00", "9.50", "已结算", now.minusDays(2).withHour(9)));
        repository.save(order("ORD-20260831-009", "璃月景区导览文化包", "旅行社直客", "25.00", "6.25", "已结算", now.minusDays(3).withHour(11)));
        log.info("[商户端初始化] merchant_order 已灌入 8 条演示订单");
    }

    private MerchantOrderEntity order(String no, String goods, String channel, String amount,
                                      String commission, String status, LocalDateTime at) {
        return new MerchantOrderEntity(no, goods, channel,
                new BigDecimal(amount), new BigDecimal(commission), status, at.format(FMT));
    }
}
