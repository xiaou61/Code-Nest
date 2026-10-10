package io.github.xiaou61.account.internal.credential;

import io.github.xiaou61.account.internal.support.Digest;
import io.github.xiaou61.account.internal.support.ExpiringStore;
import io.github.xiaou61.account.internal.support.RateLimiter;
import io.github.xiaou61.platform.BizException;
import io.github.xiaou61.platform.ErrorCode;
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
 *
 * <p><b>本端点自身也要限流</b>：它是公开的，而每张图都要在答案库里占一条记录。不限流的话
 * 一个来源反复拉图就能把库占满，进而让所有正常用户的注册流程拿不到验证码。
 */
@Service
public class CaptchaService {

    private static final Duration TTL = Duration.ofMinutes(5);
    private static final int CODE_LENGTH = 4;
    /** 同一来源每小时的发图上限。单张图很便宜，这个值只需拦住"刷"，不该影响人手刷新。 */
    private static final int ISSUES_PER_SOURCE_PER_HOUR = 60;
    private static final Duration ONE_HOUR = Duration.ofHours(1);

    private final ExpiringStore<String> answers;
    private final CaptchaImageRenderer renderer;
    private final RateLimiter rateLimiter;

    CaptchaService(ExpiringStore<String> captchaAnswerStore,
                   CaptchaImageRenderer renderer,
                   RateLimiter rateLimiter) {
        this.answers = captchaAnswerStore;
        this.renderer = renderer;
        this.rateLimiter = rateLimiter;
    }

    /** 下发一张新图。答案只以 SHA-256 留在内存，明文只在图片里。 */
    public Challenge issue(String sourceAddress) {
        rateLimiter.requireUnderLimit(
                "captcha-source-hour:" + Digest.sha256Hex(sourceAddress == null ? "" : sourceAddress),
                ISSUES_PER_SOURCE_PER_HOUR, ONE_HOUR);

        String code = renderer.randomCode(CODE_LENGTH);
        String captchaId = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(TTL);
        if (!answers.put(captchaId, Digest.sha256Hex(normalize(code)), TTL)) {
            // 容量满说明正在被刷。这是"暂时不可用"而不是 500：给 429，
            // 调用方看到的才是"过一会儿再试"，而不是"服务端崩了"。
            throw new BizException(ErrorCode.TOO_MANY_REQUESTS, "系统繁忙，请稍后重试");
        }
        String image = Base64.getEncoder().encodeToString(renderer.renderPng(code));
        return new Challenge(captchaId, image, expiresAt);
    }

    /**
     * 校验并作废。
     *
     * @throws BizException 验证码不存在、已过期、或答案不正确
     */
    public void verifyAndConsume(String captchaId, String answer) {
        Optional<String> expected = captchaId == null ? Optional.empty() : answers.take(captchaId);
        if (expected.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "图形验证码已过期，请刷新后重试");
        }
        if (answer == null || !expected.get().equals(Digest.sha256Hex(normalize(answer)))) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "图形验证码不正确，请刷新后重试");
        }
    }

    /** 比对前统一大小写与首尾空白：用户看不到大小写差异，不该因此失败。 */
    private static String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    public record Challenge(String captchaId, String imageBase64, Instant expiresAt) {
    }
}
