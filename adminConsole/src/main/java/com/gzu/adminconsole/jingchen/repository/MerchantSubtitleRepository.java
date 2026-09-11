package com.gzu.adminconsole.jingchen.repository;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.gzu.adminconsole.jingchen.entity.MerchantSubtitleEntity;

/**
 * 商户视频字幕数据访问层（模块自有，只操作 merchant_subtitle 表）。
 */
@Repository
@Transactional(readOnly = true)
public class MerchantSubtitleRepository {

    @PersistenceContext
    private EntityManager em;

    public List<MerchantSubtitleEntity> findByVideoId(Long videoId) {
        return em.createQuery("select s from MerchantSubtitleEntity s where s.videoId = :vid order by s.id",
                        MerchantSubtitleEntity.class)
                .setParameter("vid", videoId)
                .getResultList();
    }

    public MerchantSubtitleEntity findById(Long id) {
        return em.find(MerchantSubtitleEntity.class, id);
    }

    /** 某视频已绑定字幕语种(按 id 序),用于回写 merchant_video.subtitle_langs。 */
    public List<String> langsOf(Long videoId) {
        return em.createQuery("select s.lang from MerchantSubtitleEntity s where s.videoId = :vid order by s.id",
                        String.class)
                .setParameter("vid", videoId)
                .getResultList();
    }

    public long count() {
        Long count = em.createQuery("select count(s) from MerchantSubtitleEntity s", Long.class).getSingleResult();
        return count == null ? 0L : count;
    }

    @Transactional
    public MerchantSubtitleEntity save(MerchantSubtitleEntity entity) {
        return em.merge(entity);
    }

    @Transactional
    public void delete(MerchantSubtitleEntity entity) {
        em.remove(em.contains(entity) ? entity : em.merge(entity));
    }
}
