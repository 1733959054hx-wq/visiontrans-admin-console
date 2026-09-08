package com.gzu.adminconsole.jingchen.config;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.gzu.adminconsole.jingchen.entity.MerchantPlanEntity;
import com.gzu.adminconsole.jingchen.repository.MerchantPlanRepository;

/**
 * 商户投放计划演示数据初始化（jingchen 模块）。
 *
 * <p>在 {@link MerchantAccountInitializer} 之后执行(@Order 210):
 * 首次启动且 ad_plan 表为空时灌入 4 条演示计划,便于前端直接看到列表效果;
 * 已存在的计划一律不覆盖(与主工程 DataInitializer 同策略)。
 */
@Component
@Order(210)
public class MerchantPlanDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MerchantPlanDataInitializer.class);

    private final MerchantPlanRepository repository;

    public MerchantPlanDataInitializer(MerchantPlanRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        repository.save(plan("PLAN-2026-1031", "东京机场口岸 AR 实景导览", "AR 街景锁定", "机场口岸 · 中→日",
                "320000", "248600", "投放中", "6.82"));
        repository.save(plan("PLAN-2026-1028", "出境医疗术语包联合推广", "Banner 信息流", "医疗就诊 · 中→英",
                "180000", "152300", "投放中", "5.47"));
        repository.save(plan("PLAN-2026-1044", "免税购物导购专场", "Banner 信息流", "免税购物 · 中→韩",
                "120000", "118800", "预算预警", "4.28"));
        repository.save(plan("PLAN-2026-1063", "跨境电商商品出海计划", "AR 街景锁定", "电商仓库 · 中→英",
                "210000", "0", "待审核", null));
        log.info("[商户端初始化] ad_plan 已灌入 4 条演示投放计划");
    }

    private MerchantPlanEntity plan(String no, String name, String form, String scene,
                                    String budget, String used, String status, String ctr) {
        return new MerchantPlanEntity(no, name, form, scene,
                new BigDecimal(budget), new BigDecimal(used), status,
                "周敏", ctr == null ? null : new BigDecimal(ctr), false);
    }
}
