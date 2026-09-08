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
 */
@Repository
@Transactional(readOnly = true)
public class NavRepository {

    /** 菜单分组标题。 */
    public static final String GROUP = "平台治理";

    @PersistenceContext
    private EntityManager em;

    /** 全部菜单项。 */
    public List<NavMenu> findAll() {
        return em.createQuery("select n from NavMenuEntity n order by n.sortOrder", NavMenuEntity.class)
                .getResultList().stream()
                .map(n -> new NavMenu(n.getId(), n.getIcon(), n.getText(), n.getSub(), n.getTitle(), n.getDesc()))
                .toList();
    }

    /** 数据是否已初始化。 */
    public boolean isEmpty() {
        Long count = em.createQuery("select count(n) from NavMenuEntity n", Long.class).getSingleResult();
        return count == null || count == 0L;
    }

    @Transactional
    public void saveAll(List<NavMenu> menus) {
        int order = 0;
        for (NavMenu menu : menus) {
            em.persist(new NavMenuEntity(menu.id(), menu.icon(), menu.text(), menu.sub(), menu.title(),
                    menu.desc(), order++));
        }
    }
}
