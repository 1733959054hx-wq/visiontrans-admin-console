package com.gzu.adminconsole.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 口令哈希工具。
 *
 * <p>演示项目使用 SHA-256 + 固定盐；生产环境建议替换为 BCrypt
 * （引入 spring-security-crypto 后使用 BCryptPasswordEncoder）。</p>
 */
public final class PasswordHasher {

    private static final String SALT = "visiontrans-admin";

    private PasswordHasher() {
    }

    /** 计算口令哈希。 */
    public static String hash(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((SALT + rawPassword).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 不可用", ex);
        }
    }

    /** 校验口令。 */
    public static boolean matches(String rawPassword, String hash) {
        return hash != null && hash.equalsIgnoreCase(hash(rawPassword));
    }
}
