package com.gzu.adminconsole.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
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
 * <p>会话<b>持久化在 auth_session 表</b>：每个请求都按令牌查库校验，
 * 因此注销必须删除数据库记录，只清本地内存缓存是无效的（早期实现的踩坑点）。
 * 过期会话由 {@link #purgeExpired()} 定时清理；应用重启时由
 * {@code SessionStartupPurge} 按 {@code invalidate-on-startup} 配置决定是否全表清空。
 */
@Repository
@Transactional(readOnly = true)
public class AuthRepository implements TokenResolver {

    private static final Logger log = LoggerFactory.getLogger(AuthRepository.class);

    /** 令牌有效期（小时）。 */
    public static final int TOKEN_HOURS = 8;

    /** 免鉴权公开路径后缀（登录 / 验证码 / 公钥下发）。 */
    private static final List<String> PUBLIC_PATH_SUFFIXES =
            List.of("/auth/login", "/auth/captcha", "/auth/public-key");

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

    /**
     * {@inheritDoc}
     *
     * <p>主工程实现：只认后台登录相关路径为公开路径，其余一律需令牌。
     */
    @Override
    public boolean isPublicPath(String uri) {
        if (uri == null) {
            return false;
        }
        return PUBLIC_PATH_SUFFIXES.stream().anyMatch(uri::endsWith);
    }

    /** 更新口令哈希（登录成功后的静默升级：历史弱哈希 → 当前算法）。 */
    @Transactional
    public void updatePasswordHash(String username, String hash) {
        em.createQuery("update AdminUserEntity u set u.passwordHash = :h where u.username = :n")
                .setParameter("h", hash)
                .setParameter("n", username)
                .executeUpdate();
    }

    /**
     * 删除会话（登出）：会话持久化在 auth_session 表，必须删库记录才能真正失效。
     *
     * <p>早期实现只清理了一个从未写入的内存 Map，导致登出后令牌依然可用。
     */
    @Transactional
    public void deleteSession(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        em.createQuery("delete from AuthSessionEntity s where s.token = :t")
                .setParameter("t", token)
                .executeUpdate();
    }

    /**
     * 清理过期会话。
     *
     * <p>每次登录会顺带清一次；同时挂小时级定时任务兜底，避免长期无登录时表持续膨胀。
     */
    @Transactional
    @Scheduled(fixedDelayString = "${admin-console.auth.purge-interval-ms:3600000}",
            initialDelay = 60000)
    public void purgeExpired() {
        int removed = em.createQuery("delete from AuthSessionEntity s where s.expireAt < :now")
                .setParameter("now", LocalDateTime.now())
                .executeUpdate();
        if (removed > 0) {
            log.debug("[会话] 已清理过期会话 {} 条", removed);
        }
    }

    /**
     * 清理全部会话（对应用重启场景）。
     *
     * @return 被清理的会话条数
     */
    @Transactional
    public int purgeAll() {
        return em.createQuery("delete from AuthSessionEntity s").executeUpdate();
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
