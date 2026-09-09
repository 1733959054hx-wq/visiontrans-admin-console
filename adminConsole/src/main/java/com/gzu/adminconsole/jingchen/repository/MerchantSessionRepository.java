package com.gzu.adminconsole.jingchen.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.gzu.adminconsole.common.TokenResolver;
import com.gzu.adminconsole.config.AdminContext;
import com.gzu.adminconsole.jingchen.common.MerchantConstants;
import com.gzu.adminconsole.jingchen.entity.MerchantEntity;
import com.gzu.adminconsole.jingchen.entity.MerchantSessionEntity;

/**
 * 商户会话数据访问层（模块自有）。
 *
 * <p>实现 {@link TokenResolver}：在统一鉴权中作为商户令牌的解析器；
 * 主工程的 {@code AuthRepository} 解析失败（非管理员令牌）后由本实现接管。
 *
 * <p>会话保存在<b>进程内存</b>（{@link ConcurrentHashMap}）：后端重启即全部失效，
 * 商户需重新登录；持久化表 merchant_session 仅保留历史兼容，不再读写。
 */
@Repository
@Transactional(readOnly = true)
public class MerchantSessionRepository implements TokenResolver {

    /** 商户会话有效期（小时）。 */
    public static final int TOKEN_HOURS = 8;

    /** 内存会话表：token → 会话。 */
    private final Map<String, MerchantSessionEntity> sessions = new ConcurrentHashMap<>();

    /** 免鉴权公开路径（商户登录）。 */
    private static final List<String> PUBLIC_PATH_SUFFIXES = List.of("/merchant/login");

    /** 按令牌取未过期会话；令牌无效或已过期返回 null。 */
    public MerchantSessionEntity findValid(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        MerchantSessionEntity s = sessions.get(token);
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
        sessions.values().removeIf(s -> s.getMerchantCode().equals(m.getCode()));
        MerchantSessionEntity s = new MerchantSessionEntity(
                generateToken(), m.getCode(), m.getName(),
                MerchantConstants.ROLE_CODE, MerchantConstants.ROLE_NAME,
                LocalDateTime.now().plusHours(TOKEN_HOURS));
        sessions.put(s.getToken(), s);
        return s;
    }

    /** 注销会话。 */
    @Transactional
    public void delete(String token) {
        sessions.remove(token);
    }

    /** 清理过期会话（如需定时任务可挂上，当前随登录自然覆盖）。 */
    @Transactional
    public void purgeExpired() {
        LocalDateTime now = LocalDateTime.now();
        sessions.values().removeIf(s -> s.getExpireAt() == null || s.getExpireAt().isBefore(now));
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

    /**
     * {@inheritDoc}
     *
     * <p>商户实现：登录接口免鉴权（改路由前缀时改这里即可，无需动主工程拦截器）。
     */
    @Override
    public boolean isPublicPath(String uri) {
        return uri != null && PUBLIC_PATH_SUFFIXES.stream().anyMatch(uri::endsWith);
    }
}
