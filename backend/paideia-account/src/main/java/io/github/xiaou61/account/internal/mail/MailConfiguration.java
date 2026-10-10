package io.github.xiaou61.account.internal.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 按配置选择发信实现。 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MailProperties.class)
public class MailConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MailConfiguration.class);

    @Bean
    MailPort mailPort(MailProperties properties) {
        if (!properties.hasHost()) {
            if (!properties.allowLogFallback()) {
                // 失败而不是静默降级：日志里的验证码等于注册后门，误配到他人可访问的环境
                // 不该表现为"一切正常"。见 LoggingMailPort 的说明。
                throw new IllegalStateException("""
                        未配置 SMTP（paideia.mail.host 为空），且未显式开启日志降级。
                        日志降级会把验证码写进日志，任何能看到日志的人都能注册他人邮箱，因此不再自动启用。二选一：
                          · 配置真实 SMTP：设置 paideia.mail.host / username / password（多为授权码）
                          · 仅本地开发：显式设置 paideia.mail.allow-log-fallback=true
                        """);
            }
            log.warn("""
                    paideia.mail.allow-log-fallback=true 已显式开启，验证码将写进日志而不是真的发出。
                    这只适用于本地开发；部署到任何他人可访问的环境前必须关闭它并配置真实 SMTP。""");
            return new LoggingMailPort();
        }
        if (!properties.hasCredentials()) {
            log.warn("已配置 SMTP 主机但未提供账号或授权码，发信会被拒。"
                    + "多数邮箱（含 QQ 邮箱）只接受授权码，不接受邮箱登录密码。");
        }
        if (properties.isPlaintext()) {
            log.warn("""
                    已配置 SMTP 但既没开 ssl 也没开 start-tls，等于明文发信 —— 多数服务商会直接拒收。
                    465 端口用 ssl: true；587 端口用 start-tls: true。""");
        }
        log.info("邮件发送使用 SMTP：{}:{}（{}）", properties.host(), properties.port(),
                properties.ssl() ? "隐式 TLS" : properties.startTls() ? "STARTTLS" : "明文");
        return new SmtpMailPort(properties);
    }
}
