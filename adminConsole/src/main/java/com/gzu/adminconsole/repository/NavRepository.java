package com.gzu.adminconsole.repository;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.gzu.adminconsole.entity.NavMenuEntity;
import com.gzu.adminconsole.model.NavMenu;

/**
 * 侧边导航菜单数据访问层（JPA 实现）。
 *
 * <p>菜单携带 groupName（一级页面分组）与 roles（可见角色），
 * 下发时按当前登录管理员的角色过滤，实现「菜单权限配置」真实生效。
 */
@Repository
@Transactional(readOnly = true)
public class NavRepository {

    @PersistenceContext
    private EntityManager em;

    /** 全部菜单项（不区分角色，用于后台菜单权限配置面板）。 */
    public List<NavMenu> findAll() {
        return em.createQuery("select n from NavMenuEntity n order by n.sortOrder", NavMenuEntity.class)
                .getResultList().stream()
                .map(NavRepository::toModel)
                .toList();
    }

    /** 指定角色可见的菜单项（roles 为空表示对所有角色开放）。 */
    public List<NavMenu> findByRole(String roleCode) {
        return findAll().stream()
                .filter(menu -> menu.visibleTo(roleCode))
                .toList();
    }

    /** 按菜单 ID 查找。 */
    public NavMenu findById(String id) {
        NavMenuEntity entity = em.find(NavMenuEntity.class, id);
        return entity == null ? null : toModel(entity);
    }

    /** 更新菜单可见角色（逗号分隔的编码串）。 */
    @Transactional
    public void updateMenuRoles(String id, String roles) {
        NavMenuEntity entity = em.find(NavMenuEntity.class, id);
        if (entity == null) {
            return;
        }
        entity.setRoles(roles);
        em.merge(entity);
    }

    /**
     * 幂等升级单个菜单的展示信息（分组 / 文案 / 图标 / 排序），用于老库随版本对齐。
     *
     * <p>刻意不覆盖 {@code roles}：那是运营在「菜单权限配置」里调整过的数据，
     * 升级只补齐为空的默认值（见 {@link #backfillGroupAndRoles()}），不做重置。
     */
    @Transactional
    public boolean upgradeMenu(NavMenu menu, int sortOrder) {
        NavMenuEntity entity = em.find(NavMenuEntity.class, menu.id());
        if (entity == null) {
            return false;
        }
        entity.setIcon(menu.icon());
        entity.setText(menu.text());
        entity.setSub(menu.sub());
        entity.setTitle(menu.title());
        entity.setDesc(menu.desc());
        entity.setGroupName(menu.groupName() == null || menu.groupName().isBlank()
                ? NavMenu.DEFAULT_GROUP : menu.groupName());
        entity.setGroupSub(menu.groupSub());
        entity.setSortOrder(sortOrder);
        em.merge(entity);
        return true;
    }

    /** 数据是否已初始化。 */
    public boolean isEmpty() {
        Long count = em.createQuery("select count(n) from NavMenuEntity n", Long.class).getSingleResult();
        return count == null || count == 0L;
    }

    /** 按菜单 ID 判断是否存在（用于幂等补齐单个菜单）。 */
    public boolean existsById(String id) {
        return em.find(NavMenuEntity.class, id) != null;
    }

    /** 幂等补齐：老库缺少分组 / 角色字段时按默认值更新。 */
    @Transactional
    public void backfillGroupAndRoles() {
        em.createQuery("select n from NavMenuEntity n", NavMenuEntity.class).getResultList().forEach(entity -> {
            boolean dirty = false;
            if (entity.getGroupName() == null || entity.getGroupName().isBlank()) {
                entity.setGroupName(NavMenu.DEFAULT_GROUP);
                dirty = true;
            }
            if (entity.getRoles() == null || entity.getRoles().isBlank()) {
                entity.setRoles(NavMenu.ALL_ROLES);
                dirty = true;
            }
            if (dirty) {
                em.merge(entity);
            }
        });
    }

    /** 追加单个菜单项（指定排序号，用于老数据幂等补齐）。 */
    @Transactional
    public void insertOne(NavMenu menu, int sortOrder) {
        em.persist(toEntity(menu, sortOrder));
    }

    @Transactional
    public void saveAll(List<NavMenu> menus) {
        int order = 0;
        for (NavMenu menu : menus) {
            em.persist(toEntity(menu, order++));
        }
    }

    private static NavMenu toModel(NavMenuEntity n) {
        return new NavMenu(n.getId(), n.getIcon(), n.getText(), n.getSub(), n.getTitle(), n.getDesc(),
                n.getGroupName(), n.getGroupSub(), n.getRoles());
    }

    private static NavMenuEntity toEntity(NavMenu menu, int sortOrder) {
        return new NavMenuEntity(menu.id(), menu.icon(), menu.text(), menu.sub(), menu.title(), menu.desc(),
                menu.groupName() == null || menu.groupName().isBlank() ? NavMenu.DEFAULT_GROUP : menu.groupName(),
                menu.groupSub(), menu.roles(), sortOrder);
    }
}
