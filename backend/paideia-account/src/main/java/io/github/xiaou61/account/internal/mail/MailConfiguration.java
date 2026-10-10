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
            log.warn("""
                    未配置 SMTP（paideia.mail.host 为空），验证码将写进日志而不是真的发出去。
                    这只适用于本地开发；任何他人可访问的环境都必须配置真实 SMTP。""");
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
