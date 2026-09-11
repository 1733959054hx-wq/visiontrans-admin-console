package com.gzu.adminconsole.jingchen.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;

import com.gzu.adminconsole.common.BusinessException;
import com.gzu.adminconsole.jingchen.dto.MerchantVideoRequest;
import com.gzu.adminconsole.jingchen.entity.MerchantVideoEntity;
import com.gzu.adminconsole.jingchen.repository.MerchantVideoRepository;

/**
 * 商户视频接入业务逻辑（模块自有）。
 */
@Service
public class MerchantVideoService {

    private final MerchantVideoRepository repository;
    private final MerchantFileService fileService;

    public MerchantVideoService(MerchantVideoRepository repository, MerchantFileService fileService) {
        this.repository = repository;
        this.fileService = fileService;
    }

    public List<MerchantVideoEntity> list() {
        return repository.findAll();
    }

    public MerchantVideoEntity get(Long id) {
        MerchantVideoEntity entity = repository.findById(id);
        if (entity == null) {
            throw new BusinessException(404, "视频不存在: id=" + id);
        }
        return entity;
    }

    /** 新建:真实上传时按落盘文件折算大小(GB,十进制),时长转码后识别;演示建档保持前端报数。 */
    public MerchantVideoEntity create(MerchantVideoRequest req) {
        MerchantVideoEntity entity = new MerchantVideoEntity(
                requireName(req.name()),
                req.durationSec() == null ? 0L : req.durationSec(),
                sizeGbOf(req),
                req.lang() == null ? "多语" : req.lang(),
                req.status() == null ? "转码中" : req.status(),
                req.plays() == null ? 0L : req.plays(),
                req.finishRate(),
                req.region() == null ? "全球" : req.region(),
                req.subtitleLangs(),
                req.fileName(),
                req.fileUrl());
        return repository.save(entity);
    }

    public MerchantVideoEntity update(Long id, MerchantVideoRequest req) {
        MerchantVideoEntity entity = get(id);
        if (req.name() != null && !req.name().isBlank()) entity.setName(req.name().trim());
        if (req.durationSec() != null) entity.setDurationSec(req.durationSec());
        if (req.sizeGb() != null) entity.setSizeGb(req.sizeGb());
        if (req.lang() != null) entity.setLang(req.lang());
        if (req.status() != null) entity.setStatus(req.status());
        if (req.plays() != null) entity.setPlays(req.plays());
        if (req.finishRate() != null) entity.setFinishRate(req.finishRate());
        if (req.region() != null) entity.setRegion(req.region());
        if (req.subtitleLangs() != null) entity.setSubtitleLangs(req.subtitleLangs());
        if (req.fileUrl() != null) {
            entity.setFileUrl(req.fileUrl());
            entity.setFileName(req.fileName());
            BigDecimal gb = sizeGbOf(req);
            if (gb != null) entity.setSizeGb(gb);
        }
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.delete(get(id));
    }

    /** 大小(GB):真实文件按字节数 / 1000^3 两位小数;无文件时用前端报数。 */
    private BigDecimal sizeGbOf(MerchantVideoRequest req) {
        if (req.fileUrl() != null) {
            long bytes = fileService.sizeOf(fileService.storedNameOf(req.fileUrl()));
            if (bytes > 0) {
                return BigDecimal.valueOf(bytes)
                        .divide(BigDecimal.valueOf(1_000_000_000L), 2, RoundingMode.HALF_UP);
            }
        }
        return req.sizeGb();
    }

    private String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(400, "视频名称必填");
        }
        return name.trim();
    }
}
