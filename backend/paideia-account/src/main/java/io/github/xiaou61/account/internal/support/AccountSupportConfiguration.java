package io.github.xiaou61.account.internal.support;

import io.github.xiaou61.account.internal.credential.VerificationCode;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 进程内短期状态的装配。
 *
 * <p><b>三处都用进程内存储，这是单实例自托管的取舍</b>：图形验证码答案、邮箱验证码、
 * 限流计数。多实例部署时三处**都必须**换成共享存储（MySQL 带 TTL 的表或 Redis），
 * 否则会出现"验证码明明是对的却说过期"（请求落到了另一台）与"阈值变成 limit × 实例数"。
 *
 * <p>注意本类用 {@code proxyBeanMethods = false}：因此**不要在别的 @Bean 方法或
 * 定时任务里直接调用这里的方法**——那样拿到的是新实例，而不是容器里那一份。
 * 需要同一份状态就注入它（见 {@link ExpiredStatePurger}）。
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class AccountSupportConfiguration {

    /** 单个键的内存上限。超出说明正在被刷，此时拒绝比继续吃内存安全。 */
    private static final int CAPTCHA_CAPACITY = 10_000;
    private static final int EMAIL_CODE_CAPACITY = 10_000;
    private static final int RATE_LIMIT_KEYS = 50_000;

    @Bean
    ExpiringStore<String> captchaAnswerStore() {
        return new ExpiringStore<>(CAPTCHA_CAPACITY, Clock.systemUTC());
    }

    @Bean
    ExpiringStore<VerificationCode> emailCodeStore() {
        return new ExpiringStore<>(EMAIL_CODE_CAPACITY, Clock.systemUTC());
    }

    @Bean
    RateLimiter rateLimiter() {
        return new RateLimiter(RATE_LIMIT_KEYS, Clock.systemUTC());
    }
}
