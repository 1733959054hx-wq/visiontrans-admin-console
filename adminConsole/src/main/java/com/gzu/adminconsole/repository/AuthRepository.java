package com.gzu.adminconsole.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.gzu.adminconsole.config.AdminContext;
import com.gzu.adminconsole.entity.AdminUserEntity;
import com.gzu.adminconsole.entity.AuthSessionEntity;

/**
 * 登录会话数据访问层。
 */
@Repository
@Transactional(readOnly = true)
public class AuthRepository {

    /** 令牌有效期（小时）。 */
    public static final int TOKEN_HOURS = 8;

    @PersistenceContext
    private EntityManager em;

    /** 按登录账号查找管理员。 */
    public AdminUserEntity findByUsername(String username) {
        List<AdminUserEntity> list = em.createQuery(
                        "select u from AdminUserEntity u where u.username = :name", AdminUserEntity.class)
                .setParameter("name", username)
                .setMaxResults(1)
                .getResultList();
        return list.isEmpty() ? null : list.get(0);
    }

    /** 按令牌查找会话（自动忽略已过期会话）。 */
    public AuthSessionEntity findValidSession(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        AuthSessionEntity session = em.find(AuthSessionEntity.class, token);
        if (session == null || session.isExpired()) {
            return null;
        }
        return session;
    }

    /** 创建会话。 */
    @Transactional
    public AuthSessionEntity createSession(AdminUserEntity user) {
        purgeExpired();
        String token = UUID.randomUUID().toString().replace("-", "");
        AuthSessionEntity session = new AuthSessionEntity(
                token,
                user.getUsername(),
                user.getName(),
                roleCodeOf(user.getRole()),
                user.getRole(),
                user.getGroup(),
                LocalDateTime.now().plusHours(TOKEN_HOURS));
        em.persist(session);
        return session;
    }

    /** 删除会话（登出）。 */
    @Transactional
    public void deleteSession(String token) {
        AuthSessionEntity session = em.find(AuthSessionEntity.class, token);
        if (session != null) {
            em.remove(session);
        }
    }

    /** 清理过期会话。 */
    @Transactional
    public void purgeExpired() {
        em.createQuery("delete from AuthSessionEntity s where s.expireAt < :now")
                .setParameter("now", LocalDateTime.now())
                .executeUpdate();
    }

    /** 角色中文名 → 角色编码（与 RBAC 角色域一致）。 */
    public static String roleCodeOf(String roleName) {
        if (roleName == null) {
            return "";
        }
        return switch (roleName) {
            case "超级管理员" -> "SUPER_ADMIN";
            case "只读审计员" -> "AUDITOR";
            case "运营管理员" -> "OPERATIONS";
            default -> roleName;
        };
    }

    /** 把会话写入当前线程上下文。 */
    public static void bind(AuthSessionEntity session) {
        AdminContext.set(new AdminContext.CurrentAdmin(
                session.getToken(),
                session.getUsername(),
                session.getName(),
                session.getRoleCode(),
                session.getRoleName(),
                session.getGroupName()));
    }
}
