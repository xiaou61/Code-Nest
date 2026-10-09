package io.github.xiaou61.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.xiaou61.platform.BizException;
import io.github.xiaou61.platform.ErrorCode;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtAuthServiceTest {

    private static final String SECRET = "test-only-secret-value-for-hs256-0123456789";

    private static JwtAuthService service(Duration ttl) {
        return new JwtAuthService(new AuthProperties(SECRET, "paideia-test", ttl));
    }

    @Test
    @DisplayName("签发后能用自己的解码器还原主体")
    void roundTripsSubject() {
        JwtAuthService auth = service(Duration.ofMinutes(5));

        AuthPort.Token token = auth.issue("learner-a");

        assertThat(token.value()).isNotBlank();
        assertThat(auth.authenticate(token.value()).id()).isEqualTo("learner-a");
    }

    @Test
    @DisplayName("被篡改的令牌不被接受")
    void rejectsTamperedToken() {
        JwtAuthService auth = service(Duration.ofMinutes(5));
        String token = auth.issue("learner-a").value();

        // 注意不能改签名段的最后一个字符：base64url 的末位只承载 2 个有效位，
        // 改它可能解码出完全相同的字节，也就是根本没有篡改。改载荷段才是真的动了被签名的内容。
        String[] parts = token.split("\\.");
        String tamperedPayload = (parts[1].startsWith("A") ? "B" : "A") + parts[1].substring(1);
        String tampered = parts[0] + "." + tamperedPayload + "." + parts[2];

        assertThatThrownBy(() -> auth.authenticate(tampered))
                .isInstanceOf(BizException.class)
                .extracting(exception -> ((BizException) exception).errorCode())
                .isEqualTo(ErrorCode.UNAUTHENTICATED);
    }

    @Test
    @DisplayName("另一个密钥签发的令牌不被接受")
    void rejectsTokenFromAnotherKey() {
        String foreignToken = new JwtAuthService(new AuthProperties(
                "a-completely-different-secret-value-000000", "paideia-test", Duration.ofMinutes(5)))
                .issue("learner-a")
                .value();

        assertThatThrownBy(() -> service(Duration.ofMinutes(5)).authenticate(foreignToken))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("过期令牌不被接受")
    void rejectsExpiredToken() {
        // 负的存活期被归一为默认值，所以这里直接构造一个已过期的令牌：签发后立即用极短存活期校验
        JwtAuthService issuer = service(Duration.ofMillis(1));
        String token = issuer.issue("learner-a").value();

        assertThatThrownBy(() -> issuer.authenticate(token))
                .as("存活期已过，应判为未认证")
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("格式非法的令牌不被接受，且不抛出底层解析异常")
    void rejectsMalformedToken() {
        assertThatThrownBy(() -> service(Duration.ofMinutes(5)).authenticate("not-a-jwt"))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("配置了过短的密钥时在构造阶段就失败，而不是用弱密钥跑起来")
    void rejectsWeakSecret() {
        assertThatThrownBy(() -> new AuthProperties("too-short", "paideia", Duration.ofMinutes(5)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("长度不足");
    }

    @Test
    @DisplayName("未配置密钥时不抛异常，但要能被识别出来（由装配层补随机密钥并告警）")
    void allowsMissingSecretAndReportsIt() {
        AuthProperties properties = new AuthProperties(null, "paideia", Duration.ofMinutes(5));

        assertThat(properties.hasSecret()).isFalse();
        assertThat(properties.withSecret("x".repeat(40)).hasSecret()).isTrue();
    }

    @Test
    @DisplayName("未指定签发者与存活期时使用默认值")
    void appliesDefaults() {
        AuthProperties properties = new AuthProperties(SECRET, "  ", null);

        assertThat(properties.issuer()).isEqualTo("paideia");
        assertThat(properties.tokenTtl()).isEqualTo(Duration.ofHours(2));
    }
}
