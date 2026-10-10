package io.github.xiaou61.account.internal.credential;

import io.github.xiaou61.account.internal.support.Digest;
import io.github.xiaou61.account.internal.support.ExpiringStore;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * 图形验证码。
 *
 * <p><b>为什么是 captchaId 而不是 session</b>：图形验证码天生需要"服务端记得答案"，
 * 传统做法靠 session + Cookie 关联。本项目是无状态资源服务、禁用 Cookie，桌面壳的页面来源
 * 也不是 http(s) 域（Cookie 的 {@code SameSite}/{@code Secure} 语义失效），
 * 所以只能由后端发一个不透明 ID、客户端原样带回来。这不是备选之一，是与既有决定自洽的唯一解。
 *
 * <p><b>一次性</b>：任何一次校验（无论对错）都会作废该 captchaId。代价是用户打错一个字符
 * 就得刷新图片重来；换来的是同一个验证码不能被反复试探。因为图形验证码只是提高自动化成本，
 * 真正防刷的是限流，所以这里选择更严格的一侧。
 */
@Service
public class CaptchaService {

    private static final Duration TTL = Duration.ofMinutes(5);
    private static final int CODE_LENGTH = 4;

    private final ExpiringStore<String> answers;
    private final CaptchaImageRenderer renderer;

    CaptchaService(ExpiringStore<String> captchaAnswerStore, CaptchaImageRenderer renderer) {
        this.answers = captchaAnswerStore;
        this.renderer = renderer;
    }

    /** 下发一张新图。答案只以 SHA-256 留在内存，明文只在图片里。 */
    public Challenge issue() {
        String code = renderer.randomCode(CODE_LENGTH);
        String captchaId = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(TTL);
        if (!answers.put(captchaId, Digest.sha256Hex(normalize(code)), TTL)) {
            // 容量满说明正在被刷；拒绝而不是覆盖别人的记录
            throw new IllegalStateException("图形验证码存储已满，请稍后重试");
        }
        String image = Base64.getEncoder().encodeToString(renderer.renderPng(code));
        return new Challenge(captchaId, image, expiresAt);
    }

    /**
     * 校验并作废。
     *
     * @throws io.github.xiaou61.platform.BizException 验证码不存在、已过期、或答案不正确
     */
    public void verifyAndConsume(String captchaId, String answer) {
        Optional<String> expected = captchaId == null ? Optional.empty() : answers.take(captchaId);
        if (expected.isEmpty()) {
            throw new io.github.xiaou61.platform.BizException(
                    io.github.xiaou61.platform.ErrorCode.INVALID_ARGUMENT, "图形验证码已过期，请刷新后重试");
        }
        if (answer == null || !expected.get().equals(Digest.sha256Hex(normalize(answer)))) {
            throw new io.github.xiaou61.platform.BizException(
                    io.github.xiaou61.platform.ErrorCode.INVALID_ARGUMENT, "图形验证码不正确，请刷新后重试");
        }
    }

    /** 比对前统一大小写与首尾空白：用户看不到大小写差异，不该因此失败。 */
    private static String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    public record Challenge(String captchaId, String imageBase64, Instant expiresAt) {
    }
}
