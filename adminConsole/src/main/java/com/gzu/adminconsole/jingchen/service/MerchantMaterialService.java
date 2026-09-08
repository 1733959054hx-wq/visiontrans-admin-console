package com.gzu.adminconsole.jingchen.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.gzu.adminconsole.common.BusinessException;
import com.gzu.adminconsole.jingchen.dto.MerchantAbRequest;
import com.gzu.adminconsole.jingchen.dto.MerchantMaterialRequest;
import com.gzu.adminconsole.jingchen.entity.MerchantAbConfigEntity;
import com.gzu.adminconsole.jingchen.entity.MerchantMaterialEntity;
import com.gzu.adminconsole.jingchen.repository.MerchantAbConfigRepository;
import com.gzu.adminconsole.jingchen.repository.MerchantMaterialRepository;

/**
 * 商户素材管理业务逻辑（模块自有,含 A/B 测试配置）。
 */
@Service
public class MerchantMaterialService {

    private final MerchantMaterialRepository repository;
    private final MerchantAbConfigRepository abRepository;

    public MerchantMaterialService(MerchantMaterialRepository repository, MerchantAbConfigRepository abRepository) {
        this.repository = repository;
        this.abRepository = abRepository;
    }

    public List<MerchantMaterialEntity> list() {
        return repository.findAll();
    }

    public MerchantMaterialEntity get(Long id) {
        MerchantMaterialEntity entity = repository.findById(id);
        if (entity == null) {
            throw new BusinessException(404, "素材不存在: id=" + id);
        }
        return entity;
    }

    public MerchantMaterialEntity create(MerchantMaterialRequest req) {
        MerchantMaterialEntity entity = new MerchantMaterialEntity(
                requireName(req.name()), req.materialType(),
                req.sizeKb() == null ? 0L : req.sizeKb(),
                req.exposure() == null ? 0L : req.exposure(),
                req.ctr(),
                req.status() == null ? "测试中" : req.status());
        return repository.save(entity);
    }

    public MerchantMaterialEntity update(Long id, MerchantMaterialRequest req) {
        MerchantMaterialEntity entity = get(id);
        if (req.name() != null && !req.name().isBlank()) entity.setName(req.name().trim());
        if (req.materialType() != null) entity.setMaterialType(req.materialType());
        if (req.sizeKb() != null) entity.setSizeKb(req.sizeKb());
        if (req.exposure() != null) entity.setExposure(req.exposure());
        if (req.ctr() != null) entity.setCtr(req.ctr());
        if (req.status() != null) entity.setStatus(req.status());
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.delete(get(id));
    }

    /** 读取 A/B 配置;未初始化返回 null,前端显示默认占位。 */
    public MerchantAbConfigEntity ab() {
        return abRepository.findFirst();
    }

    /** 保存 A/B 配置:不存在则新建单行,已存在则就地覆盖。 */
    public MerchantAbConfigEntity saveAb(MerchantAbRequest req) {
        MerchantAbConfigEntity config = abRepository.findFirst();
        int ratio = req.ratioB() == null ? 50 : Math.min(90, Math.max(10, req.ratioB()));
        if (config == null) {
            config = new MerchantAbConfigEntity(req.materialA(), req.materialB(), ratio);
        } else {
            if (req.materialA() != null) config.setMaterialA(req.materialA());
            if (req.materialB() != null) config.setMaterialB(req.materialB());
            config.setRatioB(ratio);
        }
        return abRepository.save(config);
    }

    private String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(400, "素材名称必填");
        }
        return name.trim();
    }
}
