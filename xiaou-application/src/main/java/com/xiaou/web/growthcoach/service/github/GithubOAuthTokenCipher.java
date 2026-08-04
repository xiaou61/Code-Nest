package com.xiaou.web.growthcoach.service.github;

import com.xiaou.common.exception.BusinessException;
import com.xiaou.web.growthcoach.config.GrowthCoachProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * GitHub OAuth access token 的服务端 AES-256-GCM 封装。
 *
 * <p>密文绑定到 Code Nest 用户 ID，跨用户复制数据库记录不能被正确解密。</p>
 */
@Component
@RequiredArgsConstructor
public class GithubOAuthTokenCipher {

    private static final String VERSION = "v1";
    private static final int KEY_BYTES = 32;
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final GrowthCoachProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public boolean isConfigured() {
        try {
            return keyBytes().length == KEY_BYTES;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public String encryptForUser(Long userId, String accessToken) {
        if (userId == null || userId <= 0 || !StringUtils.hasText(accessToken)) {
            throw new BusinessException("GitHub 授权信息无效");
        }
        byte[] iv = new byte[IV_BYTES];
        secureRandom.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes(), "AES"), new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(aad(userId));
            byte[] ciphertext = cipher.doFinal(accessToken.getBytes(StandardCharsets.UTF_8));
            Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
            return VERSION + "." + encoder.encodeToString(iv) + "." + encoder.encodeToString(ciphertext);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("GitHub OAuth 令牌加密不可用", exception);
        }
    }

    String decryptForUser(Long userId, String encryptedToken) {
        if (userId == null || userId <= 0 || !StringUtils.hasText(encryptedToken)) {
            throw new BusinessException("GitHub 连接凭据不可用");
        }
        String[] parts = encryptedToken.split("\\.", -1);
        if (parts.length != 3 || !VERSION.equals(parts[0])) {
            throw new BusinessException("GitHub 连接凭据格式无效");
        }
        try {
            Base64.Decoder decoder = Base64.getUrlDecoder();
            byte[] iv = decoder.decode(parts[1]);
            byte[] ciphertext = decoder.decode(parts[2]);
            if (iv.length != IV_BYTES || ciphertext.length == 0) {
                throw new BusinessException("GitHub 连接凭据格式无效");
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes(), "AES"), new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(aad(userId));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (BusinessException exception) {
            throw exception;
        } catch (IllegalArgumentException | GeneralSecurityException exception) {
            throw new BusinessException("GitHub 连接凭据不可用");
        }
    }

    private byte[] keyBytes() {
        GrowthCoachProperties.GithubOAuth config = properties.getGithubOAuth();
        String encoded = config == null ? null : config.getTokenEncryptionKey();
        if (!StringUtils.hasText(encoded)) {
            throw new IllegalStateException("GitHub OAuth 令牌加密密钥未配置");
        }
        byte[] key = decodeKey(encoded.trim());
        if (key.length != KEY_BYTES) {
            throw new IllegalStateException("GitHub OAuth 令牌加密密钥必须是 32 字节");
        }
        return key;
    }

    private byte[] decodeKey(String value) {
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException ignored) {
            return Base64.getUrlDecoder().decode(value);
        }
    }

    private byte[] aad(Long userId) {
        return ("growth-github-oauth:user:" + userId).getBytes(StandardCharsets.UTF_8);
    }
}
