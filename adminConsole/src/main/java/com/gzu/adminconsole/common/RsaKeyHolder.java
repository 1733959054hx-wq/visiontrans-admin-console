package com.gzu.adminconsole.common;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;

import org.springframework.stereotype.Component;

/**
 * RSA 密钥持有者：应用启动时生成 2048 位密钥对。
 *
 * <p>公钥经 {@code GET /auth/public-key} 下发给前端，前端用 jsencrypt 加登录口令后再提交，
 * 后端用私钥解密，口令明文不出现在传输层。
 */
@Component
public class RsaKeyHolder {

    private static final String ALGORITHM = "RSA";
    private static final String CIPHER_TRANSFORMATION = "RSA/ECB/PKCS1Padding";

    private final KeyPair keyPair;

    public RsaKeyHolder() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(ALGORITHM);
            generator.initialize(2048);
            this.keyPair = generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 JRE 不支持 RSA", e);
        }
    }

    /** X.509 编码的公钥（Base64），可直接交给 jsencrypt 的 setPublicKey。 */
    public String publicKeyBase64() {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    /** 解密前端 jsencrypt 加密的口令（Base64 密文 → 明文）。 */
    public String decrypt(String base64Cipher) {
        try {
            PrivateKey privateKey = keyPair.getPrivate();
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] plain = cipher.doFinal(Base64.getDecoder().decode(base64Cipher));
            return new String(plain, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BusinessException("口令解密失败，请刷新页面后重试");
        }
    }
}
