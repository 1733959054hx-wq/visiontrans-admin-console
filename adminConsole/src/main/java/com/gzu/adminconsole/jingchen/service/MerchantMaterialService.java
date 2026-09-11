package com.gzu.adminconsole.jingchen.service;

import java.util.List;
import java.util.Locale;

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
    private final MerchantFileService fileService;

    public MerchantMaterialService(MerchantMaterialRepository repository, MerchantAbConfigRepository abRepository,
                                   MerchantFileService fileService) {
        this.repository = repository;
        this.abRepository = abRepository;
        this.fileService = fileService;
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
                requireName(req.name()), typeOf(req), sizeKbOf(req),
                req.exposure() == null ? 0L : req.exposure(),
                req.ctr(),
                req.status() == null ? "测试中" : req.status(),
                req.fileName(), req.fileUrl());
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
        if (req.fileUrl() != null) {
            entity.setFileUrl(req.fileUrl());
            entity.setFileName(req.fileName());
            // 换文件后以落盘真实大小为准（KB）
            entity.setSizeKb((fileService.sizeOf(fileService.storedNameOf(req.fileUrl())) + 1023) / 1024);
            if (req.materialType() == null) entity.setMaterialType(typeOfExt(fileService.storedNameOf(req.fileUrl())));
        }
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

    /** 形态:显式指定优先;否则按上传扩展名识别(视频/图片),其余归 H5。 */
    private String typeOf(MerchantMaterialRequest req) {
        if (req.materialType() != null && !req.materialType().isBlank()) {
            return req.materialType();
        }
        String extType = typeOfExt(fileService.storedNameOf(req.fileUrl()));
        return extType == null ? "图片" : extType;
    }

    private String typeOfExt(String storedName) {
        if (storedName == null) {
            return null;
        }
        String ext = storedName.substring(storedName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (ext.matches("mp4|mov|m4v|webm|avi|mkv")) {
            return "视频";
        }
        if (ext.matches("jpg|jpeg|png|gif|webp|bmp")) {
            return "图片";
        }
        return "H5";
    }

    /** 文件大小:优先取落盘真实大小(KB);演示建档(无文件)时用前端报数或 0。 */
    private Long sizeKbOf(MerchantMaterialRequest req) {
        if (req.fileUrl() != null) {
            long bytes = fileService.sizeOf(fileService.storedNameOf(req.fileUrl()));
            if (bytes > 0) {
                return (bytes + 1023) / 1024;
            }
        }
        return req.sizeKb() == null ? 0L : req.sizeKb();
    }

    private String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(400, "素材名称必填");
        }
        return name.trim();
    }
}
