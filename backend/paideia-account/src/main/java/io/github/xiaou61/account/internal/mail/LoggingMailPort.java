package io.github.xiaou61.account.internal.mail;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 未配置 SMTP 时的实现：把验证码写进日志，不出网。
 *
 * <p>用途只有一个——本地开发与自我验证时不必先配一个邮箱账号。
 *
 * <p><b>它是一条真实的风险线</b>：如果有人把没有 SMTP 配置的实例放到他人可访问的环境，
 * 验证码就会一直落在日志里，任何能看到日志的人都能注册他人邮箱。因此这里用 WARN 级别
 * 并每次都写明原因，装配层另外在启动时告警一次。
 */
public final class LoggingMailPort implements MailPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingMailPort.class);

    @Override
    public void sendVerificationCode(String to, String code, Duration validFor) {
        log.warn("""
                未配置 SMTP（paideia.mail.host 为空），验证码改为写日志：收件人={}，验证码={}，有效期={} 分钟。
                这只适用于本地开发。任何他人可访问的环境都必须配置真实 SMTP，否则日志成了注册后门。""",
                to, code, validFor.toMinutes());
    }
}
