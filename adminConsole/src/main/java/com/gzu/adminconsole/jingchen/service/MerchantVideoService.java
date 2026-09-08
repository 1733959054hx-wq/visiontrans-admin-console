package com.gzu.adminconsole.jingchen.service;

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

    public MerchantVideoService(MerchantVideoRepository repository) {
        this.repository = repository;
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

    /** 新建(模拟上传:仅建档案,状态默认转码中,由编辑推进到已就绪)。 */
    public MerchantVideoEntity create(MerchantVideoRequest req) {
        MerchantVideoEntity entity = new MerchantVideoEntity(
                requireName(req.name()),
                req.durationSec() == null ? 0L : req.durationSec(),
                req.sizeGb(),
                req.lang() == null ? "多语" : req.lang(),
                req.status() == null ? "转码中" : req.status(),
                req.plays() == null ? 0L : req.plays(),
                req.finishRate(),
                req.region() == null ? "全球" : req.region(),
                req.subtitleLangs());
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
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.delete(get(id));
    }

    private String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(400, "视频名称必填");
        }
        return name.trim();
    }
}
