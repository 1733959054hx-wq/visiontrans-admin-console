package com.gzu.adminconsole.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.gzu.adminconsole.common.TokenResolver;
import com.gzu.adminconsole.config.AdminContext;
import com.gzu.adminconsole.entity.AdminUserEntity;
import com.gzu.adminconsole.entity.AuthSessionEntity;

/**
 * 登录会话数据访问层（主工程：后台管理员 / 运营 / 审计员）。
 *
 * <p>同时作为 {@link TokenResolver} 的默认实现参与统一鉴权：
 * 商户等模块的令牌由各自模块解析，互不干扰。
 *
 * <p>会话保存在<b>进程内存</b>（{@link ConcurrentHashMap}）而非数据库：
 * 后端重启即全部失效，所有在线用户被迫重新登录，避免长期挂起的令牌；
 * 持久化表 auth_session 仅保留历史兼容，不再读写。
 */
@Repository
@Transactional(readOnly = true)
public class AuthRepository implements TokenResolver {

    /** 令牌有效期（小时）。 */
    public static final int TOKEN_HOURS = 8;

    /** 内存会话表：token → 会话。 */
    private final Map<String, AuthSessionEntity> sessions = new ConcurrentHashMap<>();

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

    /**
     * {@inheritDoc}
     *
     * <p>主工程实现：识别后台管理员令牌并绑定身份；商户令牌返回 false，由商户模块的解析器接管。
     */
    @Override
    public boolean resolveAndBind(String token) {
        AuthSessionEntity session = findValidSession(token);
        if (session == null) {
            return false;
        }
        bind(session);
        return true;
    }

    /** 更新口令哈希（登录成功后的静默升级：历史弱哈希 → 当前算法）。 */
    @Transactional
    public void updatePasswordHash(String username, String hash) {
        em.createQuery("update AdminUserEntity u set u.passwordHash = :h where u.username = :n")
                .setParameter("h", hash)
                .setParameter("n", username)
                .executeUpdate();
    }

    /** 删除会话（登出）。 */
    @Transactional
    public void deleteSession(String token) {
        sessions.remove(token);
    }

    /** 清理过期会话。 */
    @Transactional
    public void purgeExpired() {
        sessions.values().removeIf(AuthSessionEntity::isExpired);
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
