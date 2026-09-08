package com.gzu.adminconsole.jingchen.repository;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.gzu.adminconsole.common.TokenResolver;
import com.gzu.adminconsole.config.AdminContext;
import com.gzu.adminconsole.jingchen.common.MerchantConstants;
import com.gzu.adminconsole.jingchen.entity.MerchantEntity;
import com.gzu.adminconsole.jingchen.entity.MerchantSessionEntity;

/**
 * 商户会话数据访问层（模块自有，只操作 merchant_session 表）。
 *
 * <p>实现 {@link TokenResolver}：在统一鉴权中作为商户令牌的解析器；
 * 主工程的 {@code AuthRepository} 解析失败（非管理员令牌）后由本实现接管。
 */
@Repository
@Transactional(readOnly = true)
public class MerchantSessionRepository implements TokenResolver {

    /** 商户会话有效期（小时）。 */
    public static final int TOKEN_HOURS = 8;

    @PersistenceContext
    private EntityManager em;

    /** 按令牌取未过期会话；令牌无效或已过期返回 null。 */
    public MerchantSessionEntity findValid(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        MerchantSessionEntity s = em.find(MerchantSessionEntity.class, token);
        if (s == null) {
            return null;
        }
        if (s.getExpireAt() == null || s.getExpireAt().isBefore(LocalDateTime.now())) {
            return null;
        }
        return s;
    }

    /** 创建新会话（先清掉该商户旧会话，保证单点）。 */
    @Transactional
    public MerchantSessionEntity create(MerchantEntity m) {
        em.createQuery("delete from MerchantSessionEntity s where s.merchantCode = :code")
                .setParameter("code", m.getCode())
                .executeUpdate();
        MerchantSessionEntity s = new MerchantSessionEntity(
                generateToken(), m.getCode(), m.getName(),
                MerchantConstants.ROLE_CODE, MerchantConstants.ROLE_NAME,
                LocalDateTime.now().plusHours(TOKEN_HOURS));
        em.persist(s);
        return s;
    }

    /** 注销会话。 */
    @Transactional
    public void delete(String token) {
        MerchantSessionEntity s = em.find(MerchantSessionEntity.class, token);
        if (s != null) {
            em.remove(s);
        }
    }

    /** 清理过期会话（如需定时任务可挂上，当前随登录自然覆盖）。 */
    @Transactional
    public void purgeExpired() {
        em.createQuery("delete from MerchantSessionEntity s where s.expireAt < :now")
                .setParameter("now", LocalDateTime.now())
                .executeUpdate();
    }

    private static String generateToken() {
        return UUID.randomUUID().toString().replace("-", "") + "-M";
    }

    @Override
    public boolean resolveAndBind(String token) {
        MerchantSessionEntity s = findValid(token);
        if (s == null) {
            return false;
        }
        AdminContext.set(new AdminContext.CurrentAdmin(
                s.getToken(), s.getMerchantCode(), s.getMerchantName(),
                s.getRoleCode(), s.getRoleName(), "商户"));
        return true;
    }
}
