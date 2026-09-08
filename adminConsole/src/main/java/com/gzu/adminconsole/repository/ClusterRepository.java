package com.gzu.adminconsole.repository;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.gzu.adminconsole.entity.AlarmEventEntity;
import com.gzu.adminconsole.entity.ClusterNodeEntity;
import com.gzu.adminconsole.model.AlarmEvent;
import com.gzu.adminconsole.model.ClusterNode;

/**
 * 集群数据访问层（JPA 实现）。
 *
 * <p>对外暴露 {@code model} 包下的领域记录，Service 层不感知持久化技术。</p>
 */
@Repository
@Transactional(readOnly = true)
public class ClusterRepository {

    @PersistenceContext
    private EntityManager em;

    /* ------------------------------ 容器节点 ------------------------------ */

    /** 全部容器节点。 */
    public List<ClusterNode> findNodes() {
        return em.createQuery("select n from ClusterNodeEntity n order by n.id", ClusterNodeEntity.class)
                .getResultList().stream().map(this::toModel).toList();
    }

    /** 按节点 ID 查找。 */
    public ClusterNode findNode(String id) {
        ClusterNodeEntity entity = em.find(ClusterNodeEntity.class, id);
        return entity == null ? null : toModel(entity);
    }

    /** 新增节点。 */
    @Transactional
    public void insertNode(ClusterNode node) {
        em.persist(new ClusterNodeEntity(node.id(), node.zone(), node.role(), node.containers(), node.cpu(),
                node.gpu(), node.latencyMs(), node.status()));
    }

    /** 更新节点（按 ID 全量覆盖）。 */
    @Transactional
    public void updateNode(ClusterNode node) {
        ClusterNodeEntity entity = em.find(ClusterNodeEntity.class, node.id());
        if (entity == null) {
            return;
        }
        entity.setZone(node.zone());
        entity.setRole(node.role());
        entity.setContainers(node.containers());
        entity.setCpu(node.cpu());
        entity.setGpu(node.gpu());
        entity.setLatencyMs(node.latencyMs());
        entity.setStatus(node.status());
        em.merge(entity);
    }

    /** 删除节点。 */
    @Transactional
    public void deleteNode(String id) {
        ClusterNodeEntity entity = em.find(ClusterNodeEntity.class, id);
        if (entity != null) {
            em.remove(entity);
        }
    }

    /* ------------------------------ 告警事件 ------------------------------ */

    /** 告警条数（用于判断是否需要补灌演示告警）。 */
    public long countAlarms() {
        Long count = em.createQuery("select count(a) from AlarmEventEntity a", Long.class).getSingleResult();
        return count == null ? 0L : count;
    }

    /** 全部告警与自愈事件（按发生顺序）。 */
    public List<AlarmEvent> findAlarms() {
        return em.createQuery("select a from AlarmEventEntity a order by a.id", AlarmEventEntity.class)
                .getResultList().stream().map(this::toAlarmModel).toList();
    }

    /** 按主键查找告警。 */
    public AlarmEvent findAlarm(Long id) {
        AlarmEventEntity entity = em.find(AlarmEventEntity.class, id);
        return entity == null ? null : toAlarmModel(entity);
    }

    /** 新增告警。 */
    @Transactional
    public void insertAlarm(AlarmEvent alarm) {
        em.persist(new AlarmEventEntity(alarm.time(), alarm.level(), alarm.message(), alarm.result()));
    }

    /** 更新告警。 */
    @Transactional
    public void updateAlarm(AlarmEvent alarm) {
        AlarmEventEntity entity = em.find(AlarmEventEntity.class, alarm.id());
        if (entity == null) {
            return;
        }
        entity.setTime(alarm.time());
        entity.setLevel(alarm.level());
        entity.setMessage(alarm.message());
        entity.setResult(alarm.result());
        em.merge(entity);
    }

    /** 删除告警。 */
    @Transactional
    public void deleteAlarm(Long id) {
        AlarmEventEntity entity = em.find(AlarmEventEntity.class, id);
        if (entity != null) {
            em.remove(entity);
        }
    }

    /** 数据是否已初始化。 */
    public boolean isEmpty() {
        Long count = em.createQuery("select count(n) from ClusterNodeEntity n", Long.class).getSingleResult();
        return count == null || count == 0L;
    }

    @Transactional
    public void saveNodes(List<ClusterNode> nodes) {
        nodes.forEach(this::insertNode);
    }

    @Transactional
    public void saveAlarms(List<AlarmEvent> alarms) {
        alarms.forEach(this::insertAlarm);
    }

    private ClusterNode toModel(ClusterNodeEntity e) {
        return new ClusterNode(e.getId(), e.getZone(), e.getRole(), e.getContainers(), e.getCpu(),
                e.getGpu(), e.getLatencyMs(), e.getStatus());
    }

    private AlarmEvent toAlarmModel(AlarmEventEntity e) {
        return new AlarmEvent(e.getId(), e.getTime(), e.getLevel(), e.getMessage(), e.getResult());
    }
}
