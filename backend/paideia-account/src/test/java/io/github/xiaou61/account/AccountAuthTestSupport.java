package io.github.xiaou61.account;

import io.github.xiaou61.account.internal.mail.InMemoryMailPort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 测试期的邮件装配：用内存信箱替换真实发信。
 *
 * <p>{@code @Primary} 让注入 {@code MailPort} 的地方都拿到内存信箱，而生产装配里那个
 * 日志实现仍会被创建（无害）。测试要读验证码时按具体类型注入 {@link InMemoryMailPort}。
 */
@TestConfiguration(proxyBeanMethods = false)
public class AccountAuthTestSupport {

    @Bean
    @Primary
    InMemoryMailPort inMemoryMailPort() {
        return new InMemoryMailPort();
    }
}
