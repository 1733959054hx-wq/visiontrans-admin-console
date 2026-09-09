package com.gzu.adminconsole.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.gzu.adminconsole.entity.AdminUserEntity;
import com.gzu.adminconsole.entity.AuthSessionEntity;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 会话持久化的真库集成测试：验证「登出真的删掉了库里的令牌」以及「可全表清理」。
 *
 * <p>这是很重要的回归防线：早期实现把 {@code deleteSession} 写成删除一个从未写入的
 * 内存 Map，编译测试都绿，但登出后令牌实际仍然可用 —— 只有落库执行才能真正暴露。
 *
 * <p>标注 {@code @Transactional} 使所有写入在用例结束后回滚，不会污染演示数据。
 */
@SpringBootTest
@Transactional
class AuthRepositorySessionPersistenceTest {

    @Autowired
    private AuthRepository authRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void logoutReallyRemovesTokenFromDatabase() {
        AdminUserEntity user = authRepository.findByUsername("admin");
        assertNotNull(user, "演示数据里应存在 admin 账号");

        AuthSessionEntity session = authRepository.createSession(user);
        // findValidSession 走 em.find 查库，命中说明会话确实持久化了
        assertNotNull(authRepository.findValidSession(session.getToken()), "登录后会话应落库");

        authRepository.deleteSession(session.getToken());
        flushAndDetach();
        assertNull(authRepository.findValidSession(session.getToken()),
                "登出后按令牌必须查不到会话，否则旧令牌仍可登录");
    }

    @Test
    void purgeAllClearsEverySession() {
        AdminUserEntity user = authRepository.findByUsername("admin");
        assertNotNull(user, "演示数据里应存在 admin 账号");

        AuthSessionEntity session = authRepository.createSession(user);
        assertNotNull(authRepository.findValidSession(session.getToken()));

        assertTrue(authRepository.purgeAll() > 0, "应至少清理掉一条会话");
        flushAndDetach();
        assertNull(authRepository.findValidSession(session.getToken()),
                "purgeAll 后会话必须不可用（应用重启清空场景）");
    }

    /**
     * 把变更真正刷到数据库并清空一级缓存。
     *
     * <p>必须这么做：测试与被测方法共用同一个事务 {@code EntityManager}，若不清缓存，
     * {@code em.find} 会直接返回刚刚 persist 到缓存里的实体 —— 看起来像是"删除没生效"，
     * 实际是同事务缓存遮蔽了真实库状态，而生产环境每个请求都是全新的 EntityManager。
     */
    private void flushAndDetach() {
        entityManager.flush();
        entityManager.clear();
    }
}
