package com.gzu.adminconsole.jingchen.config;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.gzu.adminconsole.common.PasswordHasher;
import com.gzu.adminconsole.jingchen.common.MerchantConstants;
import com.gzu.adminconsole.jingchen.entity.MerchantEntity;
import com.gzu.adminconsole.jingchen.repository.MerchantRepository;

/**
 * 商户账号与档案初始化（jingchen 模块）。
 *
 * <p>商户账号、口令、档案<b>全部落在模块自有的 merchant_account 表</b>，
 * 不再写入主工程的 admin_user —— 因此商户不会出现在后台「管理员账号」列表中，
 * 两类用户完全分开（详见 README 隔离设计）。
 */
@Component
@Order(200)
public class MerchantAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MerchantAccountInitializer.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JingchenProperties properties;
    private final MerchantRepository merchantRepository;

    public MerchantAccountInitializer(JingchenProperties properties, MerchantRepository merchantRepository) {
        this.properties = properties;
        this.merchantRepository = merchantRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            log.info("商户模块已关闭（admin-console.jingchen.enabled=false），跳过初始化");
            return;
        }
        String username = properties.getUsername();
        String password = properties.getPassword();

        // 1) 模块自有：商户档案 + 凭据
        MerchantEntity existing = merchantRepository.findByCode(username);
        if (existing == null) {
            MerchantEntity entity = new MerchantEntity(username, "示例商户", "周敏", "138****0001",
                    "启用", LocalDateTime.now().format(FORMATTER));
            entity.setPasswordHash(PasswordHasher.hash(password));
            merchantRepository.save(entity);
            log.info("[初始化] 商户 {} 档案与凭据已创建", username);
        } else if (existing.getPasswordHash() == null || existing.getPasswordHash().isBlank()) {
            // 演示账号：仅在首次（凭据为空）写入，避免覆盖人工修改的口令
            merchantRepository.updatePasswordHash(username, PasswordHasher.hash(password));
            log.info("[初始化] 商户 {} 凭据缺失，已补写演示口令", username);
        }

        log.info("[初始化] 商户模块就绪：登录账号 {} / 口令 {}（角色 {} / {}）", username, password,
                MerchantConstants.ROLE_NAME, MerchantConstants.ROLE_CODE);
    }
}
