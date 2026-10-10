package io.github.xiaou61.account.internal.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 邮件配置。
 *
 * <p>{@code host} 为空时**不会自动**降级成"把验证码写进日志"：必须显式设置
 * {@code paideia.mail.allow-log-fallback=true}，否则装配层直接让应用启动失败。
 * 凭据只从环境变量或被忽略的本地配置文件来，仓库里只有占位说明。
 *
 * <p><b>两种加密方式都支持，因为各家服务商默认端口不同</b>：
 * <ul>
 *   <li>{@code ssl=true} —— 隐式 TLS，通常是 465 端口（QQ 邮箱、企业邮箱的常用默认）。</li>
 *   <li>{@code start-tls=true} —— 先明文握手再升级，通常是 587 端口。</li>
 * </ul>
 * 两者都不开就是明文 SMTP，多数服务商会直接拒收；装配层会就此告警。
 *
 * <p>{@code fromAddress} 缺省时回退为账号名：多数服务商要求发信人与认证账号一致，
 * 不一致会被拒（QQ 邮箱尤其如此）。
 */
@ConfigurationProperties(prefix = "paideia.mail")
public record MailProperties(
        String host,
        int port,
        String username,
        String password,
        String fromAddress,
        String fromName,
        boolean startTls,
        boolean ssl,
        boolean allowLogFallback) {

    public MailProperties {
        port = port <= 0 ? 587 : port;
        fromName = fromName == null || fromName.isBlank() ? "Paideia" : fromName;
        fromAddress = fromAddress == null || fromAddress.isBlank() ? username : fromAddress;
    }

    public boolean hasHost() {
        return host != null && !host.isBlank();
    }

    public boolean hasCredentials() {
        return username != null && !username.isBlank() && password != null && !password.isBlank();
    }

    /** 既没开 SSL 也没开 STARTTLS，等于明文发信。 */
    public boolean isPlaintext() {
        return !ssl && !startTls;
    }

    /** record 自动生成的 toString 会带出 password；配置排障时整个对象被打印很常见，所以必须自己写。 */
    @Override
    public String toString() {
        return ("MailProperties[host=%s, port=%d, username=%s, password=%s, fromAddress=%s, fromName=%s,"
                + " startTls=%s, ssl=%s, allowLogFallback=%s]")
                .formatted(host, port, username, password == null || password.isEmpty() ? null : "***",
                        fromAddress, fromName, startTls, ssl, allowLogFallback);
    }
}
