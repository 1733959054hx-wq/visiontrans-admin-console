package com.gzu.adminconsole.repository;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.gzu.adminconsole.entity.GlossaryTaskEntity;
import com.gzu.adminconsole.entity.MaterialAssetEntity;
import com.gzu.adminconsole.entity.RefundRecordEntity;
import com.gzu.adminconsole.entity.UgcHitEmb;
import com.gzu.adminconsole.entity.UgcRecordEntity;
import com.gzu.adminconsole.entity.UgcSegmentEmb;
import com.gzu.adminconsole.model.GlossaryTask;
import com.gzu.adminconsole.model.MaterialAsset;
import com.gzu.adminconsole.model.RefundRecord;
import com.gzu.adminconsole.model.UgcRecord;

/**
 * 术语库审核与 UGC 风控数据访问层（JPA 实现）。
 */
@Repository
@Transactional(readOnly = true)
public class ModerationRepository {

    private static final String UGC_ID = "UGC-CURRENT";
    private static final String REFUND_ID = "REFUND-CURRENT";

    @PersistenceContext
    private EntityManager em;

    /** 全部术语包审核任务。 */
    public List<GlossaryTask> findTasks() {
        return em.createQuery("select t from GlossaryTaskEntity t order by t.sortOrder", GlossaryTaskEntity.class)
                .getResultList().stream()
                .map(this::toTaskModel).toList();
    }

    /** 按主键查找术语包任务。 */
    public GlossaryTask findTask(Long id) {
        GlossaryTaskEntity entity = em.find(GlossaryTaskEntity.class, id);
        return entity == null ? null : toTaskModel(entity);
    }

    /** 新增术语包任务。 */
    @Transactional
    public void insertTask(GlossaryTask task) {
        Integer max = em.createQuery("select max(t.sortOrder) from GlossaryTaskEntity t", Integer.class)
                .getSingleResult();
        em.persist(new GlossaryTaskEntity(task.title(), task.priority(), task.meta(), task.owner(), task.due(),
                task.column(), max == null ? 0 : max + 1));
    }

    /** 更新术语包任务。 */
    @Transactional
    public void updateTask(GlossaryTask task) {
        GlossaryTaskEntity entity = em.find(GlossaryTaskEntity.class, task.id());
        if (entity == null) {
            return;
        }
        entity.setTitle(task.title());
        entity.setPriority(task.priority());
        entity.setMeta(task.meta());
        entity.setOwner(task.owner());
        entity.setDue(task.due());
        entity.setColumnName(task.column());
        em.merge(entity);
    }

    /** 移动术语包任务到指定看板列。 */
    @Transactional
    public void moveTask(Long id, String column) {
        GlossaryTaskEntity entity = em.find(GlossaryTaskEntity.class, id);
        if (entity != null) {
            entity.setColumnName(column);
            em.merge(entity);
        }
    }

    /** 删除术语包任务。 */
    @Transactional
    public void deleteTask(Long id) {
        GlossaryTaskEntity entity = em.find(GlossaryTaskEntity.class, id);
        if (entity != null) {
            em.remove(entity);
        }
    }

    /** AR 广告素材机审记录。 */
    public List<MaterialAsset> findAssets() {
        return em.createQuery("select a from MaterialAssetEntity a order by a.sortOrder", MaterialAssetEntity.class)
                .getResultList().stream().map(this::toAssetModel).toList();
    }

    /** 按主键查找素材。 */
    public MaterialAsset findAsset(Long id) {
        MaterialAssetEntity entity = em.find(MaterialAssetEntity.class, id);
        return entity == null ? null : toAssetModel(entity);
    }

    @Transactional
    public void insertAsset(MaterialAsset asset) {
        Integer max = em.createQuery("select max(a.sortOrder) from MaterialAssetEntity a", Integer.class)
                .getSingleResult();
        em.persist(new MaterialAssetEntity(asset.name(), asset.confidence(), asset.verdict(),
                max == null ? 0 : max + 1));
    }

    @Transactional
    public void updateAsset(MaterialAsset asset) {
        MaterialAssetEntity entity = em.find(MaterialAssetEntity.class, asset.id());
        if (entity == null) {
            return;
        }
        entity.setName(asset.name());
        entity.setConfidence(asset.confidence());
        entity.setVerdict(asset.verdict());
        em.merge(entity);
    }

    @Transactional
    public void deleteAsset(Long id) {
        MaterialAssetEntity entity = em.find(MaterialAssetEntity.class, id);
        if (entity != null) {
            em.remove(entity);
        }
    }

    /** 当前 UGC 违规记录。 */
    public UgcRecord findUgc() {
        UgcRecordEntity entity = em.find(UgcRecordEntity.class, UGC_ID);
        if (entity == null) {
            return null;
        }
        List<UgcRecord.Segment> segments = new ArrayList<>();
        entity.getSegments().forEach(s -> segments.add(new UgcRecord.Segment(s.getText(), s.getTone())));
        List<UgcRecord.Hit> hits = new ArrayList<>();
        entity.getHits().forEach(h -> hits.add(new UgcRecord.Hit(h.getName(), h.getScore())));
        return new UgcRecord(entity.getId(), entity.getLevel(), segments, hits, entity.getVerdict());
    }

    /** 更新 UGC 违规记录。 */
    @Transactional
    public void saveUgc(UgcRecord record) {
        UgcRecordEntity found = em.find(UgcRecordEntity.class, UGC_ID);
        final UgcRecordEntity entity;
        if (found == null) {
            entity = new UgcRecordEntity(UGC_ID, record.level(), record.verdict());
            em.persist(entity);
        } else {
            entity = found;
        }
        entity.setLevel(record.level());
        entity.setVerdict(record.verdict());
        entity.getSegments().clear();
        record.segments().forEach(s -> entity.getSegments().add(new UgcSegmentEmb(s.text(), s.tone())));
        entity.getHits().clear();
        record.hits().forEach(h -> entity.getHits().add(new UgcHitEmb(h.name(), h.score())));
        em.merge(entity);
    }

    /** 当前退款仲裁工单。 */
    public RefundRecord findRefund() {
        RefundRecordEntity entity = em.find(RefundRecordEntity.class, REFUND_ID);
        return entity == null ? null
                : new RefundRecord(entity.getOrderNo(), entity.getPaid(), entity.getSuggestRefund(),
                        entity.getAdvice(), entity.getSla(), entity.getReasons(), entity.getStatus());
    }

    /** 更新退款仲裁工单。 */
    @Transactional
    public void saveRefund(RefundRecord record) {
        RefundRecordEntity entity = em.find(RefundRecordEntity.class, REFUND_ID);
        if (entity == null) {
            entity = new RefundRecordEntity(REFUND_ID, record.orderNo(), record.paid(), record.suggestRefund(),
                    record.advice(), record.sla(), record.reasons(), record.status());
            em.persist(entity);
            return;
        }
        entity.setOrderNo(record.orderNo());
        entity.setPaid(record.paid());
        entity.setSuggestRefund(record.suggestRefund());
        entity.setAdvice(record.advice());
        entity.setSla(record.sla());
        entity.setReasons(record.reasons());
        entity.setStatus(record.status());
        em.merge(entity);
    }

    /** 数据是否已初始化。 */
    public boolean isEmpty() {
        Long count = em.createQuery("select count(t) from GlossaryTaskEntity t", Long.class).getSingleResult();
        if (count != null && count > 0) {
            return false;
        }
        return em.find(UgcRecordEntity.class, UGC_ID) == null;
    }

    @Transactional
    public void saveTasks(List<GlossaryTask> tasks) {
        int order = 0;
        for (GlossaryTask task : tasks) {
            em.persist(new GlossaryTaskEntity(task.title(), task.priority(), task.meta(), task.owner(),
                    task.due(), task.column(), order++));
        }
    }

    @Transactional
    public void saveAssets(List<MaterialAsset> assets) {
        int order = 0;
        for (MaterialAsset asset : assets) {
            em.persist(new MaterialAssetEntity(asset.name(), asset.confidence(), asset.verdict(), order++));
        }
    }

    private GlossaryTask toTaskModel(GlossaryTaskEntity t) {
        return new GlossaryTask(t.getId(), t.getTitle(), t.getPriority(), t.getMeta(), t.getOwner(), t.getDue(),
                t.getColumnName());
    }

    private MaterialAsset toAssetModel(MaterialAssetEntity a) {
        return new MaterialAsset(a.getId(), a.getName(), a.getConfidence(), a.getVerdict());
    }
}
