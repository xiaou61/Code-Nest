package io.github.xiaou61;

import io.github.xiaou61.account.internal.mail.InMemoryMailPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * 测试 profile 的公共装配。
 *
 * <p><b>为什么不是 {@code @TestConfiguration}</b>：{@code @TestConfiguration} 刻意**不参与组件扫描**，
 * 只能由每个测试类自己 {@code @Import}。而 Spring 的上下文缓存按"配置"区分——只要有一个类多导
 * 一个配置，它就和其它测试**各起一个上下文**（每个上下文要起一次 Spring 并让 Flyway 连远程库，
 * 实测 15–25 秒）。用 {@code @Profile("test")} 被扫描到，所有集成测试就天然共用一个上下文。
 *
 * <p><b>新增测试类不要再自己 {@code @Import} 任何测试装配</b>；需要新的测试 bean 就加在本类里。
 * 这条纪律的代价写在这里：一旦有人加了 {@code @Import}，整套集成测试就多付一次上下文启动。
 */
@Configuration(proxyBeanMethods = false)
@Profile("test")
class TestProfileConfiguration {

    /**
     * 用内存信箱替换真实发信，注册链路的验证因此不依赖真实 SMTP。
     *
     * <p>{@code @Primary} 让注入 {@code MailPort} 的地方都拿到它；需要读验证码的测试按具体类型注入。
     * 实现类来自账号模块的 test-jar（与生产实现分目录，不进生产产物）。
     */
    @Bean
    @Primary
    InMemoryMailPort inMemoryMailPort() {
        return new InMemoryMailPort();
    }
}
