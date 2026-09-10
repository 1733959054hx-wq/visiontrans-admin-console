package com.gzu.adminconsole.jingchen.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gzu.adminconsole.common.BusinessException;
import com.gzu.adminconsole.config.AdminContext;
import com.gzu.adminconsole.entity.MerchantOnboardingEntity;
import com.gzu.adminconsole.jingchen.dto.MerchantOnboardRequest;
import com.gzu.adminconsole.jingchen.repository.MerchantOnboardRepository;

/**
 * 商户入驻业务逻辑（jingchen 模块,商户侧）。
 *
 * <p>流程对应功能表「入驻页」:① 资质提交(创建申请单,待审核) →
 * ② 合同签署(本单置「已签署」) → ③ 等待平台审核(通过 / 驳回)。
 * 已驳回的申请可重新提交(生成新的申请单)。审核动作由后台管理端负责。
 */
@Service
public class MerchantOnboardService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final MerchantOnboardRepository repository;

    public MerchantOnboardService(MerchantOnboardRepository repository) {
        this.repository = repository;
    }

    /** 当前商户的入驻申请(最新一条,可能为 null = 从未提交)。 */
    @Transactional(readOnly = true)
    public MerchantOnboardingEntity mine() {
        String merchantName = AdminContext.get().name();
        List<MerchantOnboardingEntity> list = repository.findByName(merchantName);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 资质提交:新商户创建申请;已驳回的重新提交生成新申请单。
     *  商户名一律取当前会话身份(不信任前端传入),保证 mine() 能按商户名找回申请单。 */
    @Transactional
    public MerchantOnboardingEntity submit(MerchantOnboardRequest req) {
        if (req.licenseNo() == null || req.licenseNo().isBlank()) {
            throw new BusinessException(400, "营业执照号必填");
        }
        String merchantName = AdminContext.get().name(); // 身份从会话取,与查询键一致
        MerchantOnboardingEntity entity = new MerchantOnboardingEntity(
                "APP-" + (System.currentTimeMillis() % 1_000_000),
                merchantName,
                req.licenseNo().trim(),
                req.contact() == null ? "" : req.contact().trim(),
                req.phone() == null ? "" : req.phone().trim(),
                req.qualification() == null ? "" : req.qualification().trim(),
                "待签署",
                "待审核",
                LocalDateTime.now().format(FMT),
                "",
                "");
        return repository.save(entity);
    }

    /** 签署合作协议:仅本商户最新申请单,且不能重复签署。 */
    @Transactional
    public MerchantOnboardingEntity signContract() {
        MerchantOnboardingEntity entity = mine();
        if (entity == null) {
            throw new BusinessException(400, "请先提交入驻资质");
        }
        if ("已签署".equals(entity.getContractStatus())) {
            throw new BusinessException(400, "合同已签署,请勿重复操作");
        }
        entity.setContractStatus("已签署");
        return repository.save(entity);
    }
}
