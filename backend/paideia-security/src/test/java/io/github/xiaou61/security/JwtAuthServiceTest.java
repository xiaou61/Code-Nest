package io.github.xiaou61.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.xiaou61.platform.BizException;
import io.github.xiaou61.platform.ErrorCode;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

class JwtAuthServiceTest {

    private static final String SECRET = "test-only-secret-value-for-hs256-0123456789";
    private static final AuthPort.Subject LEARNER = new AuthPort.Subject("learner-a", AuthPort.ROLE_LEARNER);

    private static JwtAuthService service(Duration ttl) {
        return new JwtAuthService(new AuthProperties(SECRET, "paideia-test", ttl));
    }

    @Test
    @DisplayName("签发后能用自己的解码器还原主体与角色")
    void roundTripsSubjectAndRole() {
        JwtAuthService auth = service(Duration.ofMinutes(5));

        AuthPort.Token token = auth.issue(LEARNER);

        assertThat(token.value()).isNotBlank();
        assertThat(auth.authenticate(token.value()).id()).isEqualTo("learner-a");
        assertThat(auth.authenticate(token.value()).role()).isEqualTo(AuthPort.ROLE_LEARNER);
    }

    @Test
    @DisplayName("管理员的角色也随令牌往返")
    void roundTripsAdminRole() {
        JwtAuthService auth = service(Duration.ofMinutes(5));

        String token = auth.issue(new AuthPort.Subject("admin-1", AuthPort.ROLE_ADMIN)).value();

        assertThat(auth.authenticate(token).role()).isEqualTo(AuthPort.ROLE_ADMIN);
    }

    @Test
    @DisplayName("缺少角色 claim 的令牌被拒（合法令牌必然带角色）")
    void rejectsTokenWithoutRole() {
        JwtAuthService auth = service(Duration.ofMinutes(5));
        // 用同一个密钥直接造一个没有 role 的令牌：模拟旧版本签发或伪造
        JwtEncoderParameters withoutRole = JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder()
                        .issuer("paideia-test")
                        .subject("learner-a")
                        .issuedAt(java.time.Instant.now())
                        .expiresAt(java.time.Instant.now().plusSeconds(300))
                        .build());

        String token = new org.springframework.security.oauth2.jwt.NimbusJwtEncoder(
                        new com.nimbusds.jose.jwk.source.ImmutableSecret<>(
                                new javax.crypto.spec.SecretKeySpec(
                                        SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256")))
                .encode(withoutRole)
                .getTokenValue();

        assertThatThrownBy(() -> auth.authenticate(token))
                .isInstanceOf(BizException.class)
                .extracting(exception -> ((BizException) exception).errorCode())
                .isEqualTo(ErrorCode.UNAUTHENTICATED);
    }

    @Test
    @DisplayName("角色不在已知集合里的令牌被拒")
    void rejectsUnknownRole() {
        JwtAuthService auth = service(Duration.ofMinutes(5));

        String token = auth.issue(new AuthPort.Subject("someone", "teacher")).value();

        assertThatThrownBy(() -> auth.authenticate(token)).isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("被篡改的令牌不被接受")
    void rejectsTamperedToken() {
        JwtAuthService auth = service(Duration.ofMinutes(5));
        String token = auth.issue(LEARNER).value();

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
                .issue(LEARNER)
                .value();

        assertThatThrownBy(() -> service(Duration.ofMinutes(5)).authenticate(foreignToken))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("过期令牌不被接受")
    void rejectsExpiredToken() {
        // 负的存活期被归一为默认值，所以这里直接构造一个已过期的令牌：签发后立即用极短存活期校验
        JwtAuthService issuer = service(Duration.ofMillis(1));
        String token = issuer.issue(LEARNER).value();

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
