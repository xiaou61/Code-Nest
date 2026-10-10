package io.github.xiaou61.account.internal.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 摘要工具。
 *
 * <p>两类用途：① 一次性凭据（refresh 令牌、邮箱验证码、图形验证码答案）入库/入内存前先摘要，
 * 使记录泄露也无法直接冒用；② 限流键用邮箱摘要而不是邮箱本身，避免把个人标识散落在内存里。
 *
 * <p>这里**故意不加盐也不做慢哈希**：这些值本身就是高熵或短寿命的，加盐没有意义；
 * 慢哈希会让每次校验都付出无谓的 CPU。密码是另一回事——它必须用 BCrypt，见
 * {@code PasswordHasher}。
 */
public final class Digest {

    private Digest() {
    }

    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            // SHA-256 是 JDK 必须实现的算法，走到这里说明运行环境不完整
            throw new IllegalStateException("运行环境缺少 SHA-256", exception);
        }
    }
}
