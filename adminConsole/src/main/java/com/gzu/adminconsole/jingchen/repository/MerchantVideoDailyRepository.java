package com.gzu.adminconsole.jingchen.repository;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.gzu.adminconsole.jingchen.entity.MerchantVideoDailyEntity;

/**
 * 商户视频日播放统计数据访问层（模块自有，只操作 merchant_video_daily 表）。
 */
@Repository
@Transactional(readOnly = true)
public class MerchantVideoDailyRepository {

    @PersistenceContext
    private EntityManager em;

    public List<MerchantVideoDailyEntity> findAll() {
        return em.createQuery("select d from MerchantVideoDailyEntity d order by d.statDate, d.videoId",
                        MerchantVideoDailyEntity.class)
                .getResultList();
    }

    public long count() {
        Long count = em.createQuery("select count(d) from MerchantVideoDailyEntity d", Long.class)
                .getSingleResult();
        return count == null ? 0L : count;
    }

    @Transactional
    public MerchantVideoDailyEntity save(MerchantVideoDailyEntity entity) {
        return em.merge(entity);
    }
}
