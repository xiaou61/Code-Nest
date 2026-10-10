package io.github.xiaou61.account.internal.user;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 密码哈希。用 BCrypt——Spring Security 自带，零新增依赖，且刻意慢。
 *
 * <p><b>为什么必须慢</b>：口令是低熵的，快哈希（SHA-256）配上一次拖库就等于明文。
 * BCrypt 的单次成本让离线爆破变得昂贵，代价是每次登录多花几十毫秒 CPU。
 *
 * <p>{@link #wasteTimeLikeAMatch()} 是为**时序侧信道**准备的：如果账号不存在时直接返回，
 * 响应会明显快于"账号存在但密码错"，攻击者据此就能枚举出哪些账号存在。所以账号不存在时
 * 也对着一个假的摘要跑一次同样的校验。
 */
@Component
public class PasswordHasher {

    /**
     * 用于对齐耗时的假摘要。它不是任何真实口令的摘要，只是让 BCrypt 有活干。
     * 在构造时生成一次，避免每次调用多付一次哈希成本。
     */
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final String decoyHash = encoder.encode("decoy-value-never-used-as-a-real-password");

    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null) {
            return false;
        }
        return encoder.matches(rawPassword, storedHash);
    }

    /** 对着假摘要做一次校验，使"账号不存在"与"密码错误"两条路径耗时接近。 */
    public void wasteTimeLikeAMatch() {
        encoder.matches("decoy-value-never-used-as-a-real-password", decoyHash);
    }
}
