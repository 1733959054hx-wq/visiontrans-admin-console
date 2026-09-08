package com.gzu.adminconsole.jingchen.config;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.gzu.adminconsole.jingchen.entity.MerchantAbConfigEntity;
import com.gzu.adminconsole.jingchen.entity.MerchantMaterialEntity;
import com.gzu.adminconsole.jingchen.repository.MerchantAbConfigRepository;
import com.gzu.adminconsole.jingchen.repository.MerchantMaterialRepository;

/**
 * 商户素材与 A/B 配置演示数据初始化（jingchen 模块）。
 * 表空时灌入 4 条素材与一条默认 A/B 配置;已有数据一律不覆盖。
 */
@Component
@Order(222)
public class MerchantMaterialDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MerchantMaterialDataInitializer.class);

    private final MerchantMaterialRepository repository;
    private final MerchantAbConfigRepository abRepository;

    public MerchantMaterialDataInitializer(MerchantMaterialRepository repository,
                                           MerchantAbConfigRepository abRepository) {
        this.repository = repository;
        this.abRepository = abRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() == 0) {
            repository.save(mat("东京机场导览_15s.mp4", "视频", 12800L, 1_864_120L, "6.82", "使用中"));
            repository.save(mat("东京机场导览_B版_15s.mp4", "视频", 12400L, 1_610_900L, "5.47", "测试中"));
            repository.save(mat("免税购物_kv_main.png", "图片", 2400L, 986_400L, "4.28", "使用中"));
            repository.save(mat("医疗术语包_h5.html", "H5", 180L, 742_600L, "5.03", "已停用"));
            log.info("[商户端初始化] merchant_material 已灌入 4 条素材");
        }
        if (abRepository.count() == 0) {
            abRepository.save(new MerchantAbConfigEntity("东京机场导览_15s.mp4", "东京机场导览_B版_15s.mp4", 50));
            log.info("[商户端初始化] merchant_ab_config 默认配置已写入(A/B 各 50%)");
        }
    }

    private MerchantMaterialEntity mat(String name, String type, long kb, long expo, String ctr, String status) {
        return new MerchantMaterialEntity(name, type, kb, expo, new BigDecimal(ctr), status);
    }
}
