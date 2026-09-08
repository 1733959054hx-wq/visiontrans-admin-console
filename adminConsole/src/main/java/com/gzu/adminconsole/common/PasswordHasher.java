package com.gzu.adminconsole.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * 口令哈希工具。
 *
 * <p>当前格式：{@code pbkdf2$<迭代次数>$<Base64 盐>$<Base64 派生密钥>}，使用 JDK 内置的
 * PBKDF2WithHmacSHA256，每个口令独立随机盐，迭代次数遵循 OWASP 建议（600,000）。
 *
 * <p>历史格式（SHA-256 + 固定盐）仍可被 {@link #matches(String, String)} 校验通过，
 * 但会被 {@link #needsRehash(String)} 标记为需升级；{@code AuthService} 在登录成功时
 * 用当时拿到的明文静默重写为新格式，实现无感知迁移。</p>
 */
public final class PasswordHasher {

    /** 口令派生算法（JDK 内置，无需引入 spring-security-crypto）。 */
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    /** 迭代次数：OWASP Password Storage Cheat Sheet 对 PBKDF2-HMAC-SHA256 的建议值。 */
    private static final int ITERATIONS = 600_000;
    /** 派生密钥长度（位）。 */
    private static final int KEY_LENGTH = 256;
    /** 随机盐长度（字节）。 */
    private static final int SALT_LENGTH = 16;
    /** 新版密文前缀。 */
    private static final String PREFIX = "pbkdf2$";
    /** 密文分段数：pbkdf2 / 迭代次数 / 盐 / 密钥。 */
    private static final int SEGMENTS = 4;
    /** 历史格式使用的固定盐，仅用于兼容迁移，不再用于新密文。 */
    private static final String LEGACY_SALT = "visiontrans-admin";

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    /** 计算口令哈希（每次生成独立随机盐）。 */
    public static String hash(String rawPassword) {
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        byte[] derived = pbkdf2(rawPassword, salt, ITERATIONS);
        return PREFIX + ITERATIONS + "$" + encode(salt) + "$" + encode(derived);
    }

    /** 校验口令，兼容历史 SHA-256 密文。 */
    public static boolean matches(String rawPassword, String hash) {
        if (rawPassword == null || hash == null || hash.isBlank()) {
            return false;
        }
        if (!hash.startsWith(PREFIX)) {
            // 历史密文：SHA-256(SALT + 口令)，校验通过后在登录时静默升级
            return hash.equalsIgnoreCase(legacySha256(rawPassword));
        }
        String[] parts = hash.split("\\$");
        if (parts.length != SEGMENTS) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            return MessageDigest.isEqual(expected, pbkdf2(rawPassword, salt, iterations));
        } catch (IllegalArgumentException ex) {
            // 密文被篡改或编码非法，一律按校验失败处理
            return false;
        }
    }

    /**
     * 该密文是否需要重新哈希（历史格式，或迭代次数低于当前配置）。
     *
     * <p>登录成功后调用，为真则用当次明文重写密文，完成无感知升级。</p>
     */
    public static boolean needsRehash(String hash) {
        if (hash == null || !hash.startsWith(PREFIX)) {
            return true;
        }
        String[] parts = hash.split("\\$");
        if (parts.length != SEGMENTS) {
            return true;
        }
        try {
            return Integer.parseInt(parts[1]) < ITERATIONS;
        } catch (NumberFormatException ex) {
            return true;
        }
    }

    private static byte[] pbkdf2(String rawPassword, byte[] salt, int iterations) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            PBEKeySpec spec = new PBEKeySpec(rawPassword.toCharArray(), salt, iterations, KEY_LENGTH);
            try {
                return factory.generateSecret(spec).getEncoded();
            } finally {
                spec.clearPassword();
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new IllegalStateException("PBKDF2WithHmacSHA256 不可用", ex);
        }
    }

    private static String legacySha256(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((LEGACY_SALT + rawPassword).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 不可用", ex);
        }
    }

    private static String encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
}
