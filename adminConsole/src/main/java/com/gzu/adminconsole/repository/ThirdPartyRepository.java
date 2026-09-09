package com.gzu.adminconsole.repository;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.gzu.adminconsole.entity.ThirdPartyServiceEntity;

/**
 * 第三方依赖服务数据访问层。
 */
@Repository
@Transactional(readOnly = true)
public class ThirdPartyRepository {

    @PersistenceContext
    private EntityManager em;

    /** 全部依赖服务（按展示顺序）。 */
    public List<ThirdPartyServiceEntity> findAll() {
        return em.createQuery("select t from ThirdPartyServiceEntity t order by t.sortOrder",
                ThirdPartyServiceEntity.class).getResultList();
    }

    public ThirdPartyServiceEntity findById(Long id) {
        return em.find(ThirdPartyServiceEntity.class, id);
    }

    /** 写入拨测结果。 */
    @Transactional
    public void saveProbeResult(ThirdPartyServiceEntity entity) {
        em.merge(entity);
    }

    @Transactional
    public void saveAll(List<ThirdPartyServiceEntity> services) {
        int order = 0;
        for (ThirdPartyServiceEntity service : services) {
            service.setSortOrder(order++);
            em.persist(service);
        }
    }

    public boolean isEmpty() {
        Long count = em.createQuery("select count(t) from ThirdPartyServiceEntity t", Long.class).getSingleResult();
        return count == null || count == 0L;
    }
}
