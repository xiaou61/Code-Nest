package io.github.xiaou61.account.internal.credential;

import io.github.xiaou61.account.internal.mail.MailPort;
import io.github.xiaou61.account.internal.support.Digest;
import io.github.xiaou61.account.internal.support.ExpiringStore;
import io.github.xiaou61.account.internal.support.RateLimiter;
import io.github.xiaou61.platform.BizException;
import io.github.xiaou61.platform.ErrorCode;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * 邮箱验证码：生成、发信、校验。发信前必须先通过图形验证码。
 *
 * <p><b>顺序是设计的一部分</b>：先校验图形验证码 → 再查限流 → 才发信。
 * 反过来先查限流的话，用户把图形验证码打错一个字符就会白白烧掉 60 秒的发信间隔，
 * 而图形验证码本身就是防机器人的那道闸，先过它没有损失。
 *
 * <p><b>三条限流缺一不可</b>：同邮箱最小间隔（防连点）、同邮箱每小时上限（防刷一个邮箱）、
 * 同来源 IP 每小时上限（防用一个 IP 刷很多邮箱）。没有它们，这个接口就是替你烧发信配额
 * 并把发信域名打进黑名单的工具。
 *
 * <p><b>来源 IP 取自直连地址</b>：部署在反向代理后面时需要改读 {@code X-Forwarded-For}，
 * 否则所有请求会被算成同一个来源而互相影响。当前是直连自托管，记下这个天花板。
 */
@Service
public class EmailCodeService {

    private static final Duration CODE_TTL = Duration.ofMinutes(10);
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration SEND_INTERVAL = Duration.ofSeconds(60);
    private static final int SENDS_PER_EMAIL_PER_HOUR = 5;
    private static final int SENDS_PER_SOURCE_PER_HOUR = 20;
    private static final Duration ONE_HOUR = Duration.ofHours(1);

    private final ExpiringStore<VerificationCode> codes;
    private final RateLimiter rateLimiter;
    private final CaptchaService captchaService;
    private final MailPort mailPort;
    private final SecureRandom random = new SecureRandom();

    EmailCodeService(ExpiringStore<VerificationCode> emailCodeStore,
                     RateLimiter rateLimiter,
                     CaptchaService captchaService,
                     MailPort mailPort) {
        this.codes = emailCodeStore;
        this.rateLimiter = rateLimiter;
        this.captchaService = captchaService;
        this.mailPort = mailPort;
    }

    /** 校验图形验证码 → 限流 → 生成并发送。 */
    public void send(String email, String captchaId, String captchaAnswer, String sourceAddress) {
        captchaService.verifyAndConsume(captchaId, captchaAnswer);

        String emailKey = hashOf(email);
        rateLimiter.requireInterval("email-send:" + emailKey, SEND_INTERVAL);
        rateLimiter.requireUnderLimit("email-send-hour:" + emailKey, SENDS_PER_EMAIL_PER_HOUR, ONE_HOUR);
        rateLimiter.requireUnderLimit(
                "source-send-hour:" + hashOf(sourceAddress), SENDS_PER_SOURCE_PER_HOUR, ONE_HOUR);

        String code = "%06d".formatted(100_000 + random.nextInt(900_000));
        boolean stored = codes.put(
                emailKey, new VerificationCode(emailKey, code, Instant.now().plus(CODE_TTL)), CODE_TTL);
        if (!stored) {
            throw new BizException(ErrorCode.TOO_MANY_REQUESTS, "系统繁忙，请稍后重试");
        }
        // 发信失败会让整个注册流程失败（异常向上抛），而不是留下一枚用户永远收不到的验证码
        mailPort.sendVerificationCode(email, code, CODE_TTL);
    }

    /**
     * 校验并作废。一次性：成功后该验证码立即不可再用。
     *
     * <p>计数、比对与消费都由 {@link VerificationCode#attempt} 在一次原子操作里完成，
     * 这里只负责按判定结果决定是否把记录从存储里摘掉。
     *
     * @throws BizException 验证码不存在、已过期、错误次数超限、或不正确
     */
    public void verifyAndConsume(String email, String code) {
        String emailKey = hashOf(email);
        Optional<VerificationCode> stored = codes.get(emailKey);
        if (stored.isEmpty()) {
            throw invalid("验证码不存在或已过期，请重新获取");
        }
        switch (stored.get().attempt(code, MAX_ATTEMPTS)) {
            case MATCHED -> codes.remove(emailKey);
            case EXHAUSTED -> {
                // 6 位数字只有 100 万种，不限次就是可爆破；超限即作废并要求重新获取
                codes.remove(emailKey);
                throw invalid("验证码错误次数过多，请重新获取");
            }
            // 并发重放与普通的码不对对外给同一句话，避免成为探测手段
            case REPLAYED, MISMATCH -> throw invalid("验证码不正确");
        }
    }

    static String hashOf(String value) {
        return Digest.sha256Hex(value.trim().toLowerCase(Locale.ROOT));
    }

    private static BizException invalid(String message) {
        return new BizException(ErrorCode.INVALID_ARGUMENT, message);
    }
}
