package com.gzu.adminconsole.jingchen.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
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
 * 商户会话数据访问层（模块自有）。
 *
 * <p>实现 {@link TokenResolver}：在统一鉴权中作为商户令牌的解析器；
 * 主工程的 {@code AuthRepository} 解析失败（非管理员令牌）后由本实现接管。
 *
 * <p>会话<b>落库</b>（merchant_session 表）：后端重启不再注销商户，
 * 移动端 App 跨进程、跨重启保持登录；同一商户允许多端同时在线
 * （web / android / ios 各持一个 token 互不干扰），上限
 * {@link #MAX_SESSIONS_PER_MERCHANT} 台，溢出时淘汰最早过期的会话。
 */
@Repository
@Transactional(readOnly = true)
public class MerchantSessionRepository implements TokenResolver {

    /** 商户会话有效期（小时）。 */
    public static final int TOKEN_HOURS = 8;

    /** 每商户最多同时在线设备数（web / 移动端并存的上限）。 */
    public static final int MAX_SESSIONS_PER_MERCHANT = 5;

    /** 免鉴权公开路径：商户登录 + 上传文件的取回（能力地址，名称不可猜测）。 */
    private static final List<String> PUBLIC_PATH_SUFFIXES = List.of("/merchant/login");

    /** 上传文件取回的公开地址段：/api/merchant/files/{uuid}.ext。 */
    private static final String FILES_PUBLIC_SEGMENT = MerchantConstants.API_PREFIX + "/files/";

    @PersistenceContext
    private EntityManager em;

    /** 按令牌取未过期会话（落库，重启不失效）；无效或已过期返回 null。 */
    public MerchantSessionEntity findValid(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        List<MerchantSessionEntity> hits = em.createQuery(
                        "select s from MerchantSessionEntity s where s.token = :t and s.expireAt > :now",
                        MerchantSessionEntity.class)
                .setParameter("t", token)
                .setParameter("now", LocalDateTime.now())
                .setMaxResults(1)
                .getResultList();
        return hits.isEmpty() ? null : hits.get(0);
    }

    /** 某商户当前有效的会话（按过期时间升序,最早的在前）。 */
    public List<MerchantSessionEntity> findValidByMerchant(String merchantCode) {
        return em.createQuery("select s from MerchantSessionEntity s where s.merchantCode = :c and s.expireAt > :now"
                                + " order by s.expireAt",
                        MerchantSessionEntity.class)
                .setParameter("c", merchantCode)
                .setParameter("now", LocalDateTime.now())
                .getResultList();
    }

    /** 创建新会话（多端并存:不互踢,仅在该商户有效会话达到上限时淘汰最早过期的）。 */
    @Transactional
    public MerchantSessionEntity create(MerchantEntity m, String device) {
        purgeExpired();
        List<MerchantSessionEntity> valid = findValidByMerchant(m.getCode());
        if (valid.size() >= MAX_SESSIONS_PER_MERCHANT) {
            for (MerchantSessionEntity oldest : valid.subList(0, valid.size() - (MAX_SESSIONS_PER_MERCHANT - 1))) {
                em.remove(em.contains(oldest) ? oldest : em.merge(oldest));
            }
        }
        MerchantSessionEntity s = new MerchantSessionEntity(
                generateToken(), m.getCode(), m.getName(),
                MerchantConstants.ROLE_CODE, MerchantConstants.ROLE_NAME,
                LocalDateTime.now().plusHours(TOKEN_HOURS),
                normalizeDevice(device));
        em.persist(s);
        return s;
    }

    /** 注销会话（登出）。 */
    @Transactional
    public void delete(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        MerchantSessionEntity s = em.find(MerchantSessionEntity.class, token);
        if (s != null) {
            em.remove(s);
        }
    }

    /** 清理过期会话（登录时顺手执行，也可挂定时任务）。 */
    @Transactional
    public void purgeExpired() {
        em.createQuery("delete from MerchantSessionEntity s where s.expireAt is null or s.expireAt < :now")
                .setParameter("now", LocalDateTime.now())
                .executeUpdate();
    }

    private static String normalizeDevice(String device) {
        if (device == null || device.isBlank()) {
            return "web";
        }
        String d = device.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        return d.isEmpty() ? "web" : d.substring(0, Math.min(d.length(), 32));
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
                s.getRoleCode(), s.getRoleName(), s.getDevice() == null ? "商户" : s.getDevice()));
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>商户实现：登录接口免鉴权（改路由前缀时改这里即可，无需动主工程拦截器）；
     * 上传文件的取回 {@code GET /merchant/files/{uuid}.ext} 同样公开——
     * 地址含不可猜测的 32 位 UUID,移动端用户可凭链接展示商户发布的图片 / 视频内容;
     * 文件<b>上传</b>(POST /merchant/files)不含该地址段,仍需商户令牌。
     */
    @Override
    public boolean isPublicPath(String uri) {
        if (uri == null) {
            return false;
        }
        return PUBLIC_PATH_SUFFIXES.stream().anyMatch(uri::endsWith)
                || uri.contains(FILES_PUBLIC_SEGMENT);
    }
}
