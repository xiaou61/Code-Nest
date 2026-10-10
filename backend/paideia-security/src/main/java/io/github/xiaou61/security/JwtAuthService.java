package io.github.xiaou61.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** 自签 JWT 的 {@link AuthPort} 实现。 */
public class JwtAuthService implements AuthPort {

    /** 角色 claim 的名字。前端 `session.ts` 与 Spring Security 的转换器都按这个名字读。 */
    public static final String ROLE_CLAIM = "role";

    private static final Set<String> KNOWN_ROLES = Set.of(ROLE_ADMIN, ROLE_LEARNER);

    private final AuthProperties properties;
    private final NimbusJwtEncoder encoder;
    private final JwtDecoder decoder;

    public JwtAuthService(AuthProperties properties) {
        this.properties = properties;
        SecretKey key = secretKey(properties.secret());
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        this.decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    /** 供资源服务器的过滤器使用同一个解码器：令牌的校验规则只有一份。 */
    public JwtDecoder decoder() {
        return decoder;
    }

    @Override
    public Token issue(Subject subject) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.tokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(subject.id())
                .claim(ROLE_CLAIM, subject.role())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();

        String value = encoder
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new Token(value, expiresAt);
    }

    @Override
    public Subject authenticate(String token) {
        Jwt jwt;
        try {
            jwt = decoder.decode(token);
        } catch (RuntimeException exception) {
            // 签名不符、过期、格式错误都归为未认证：对外不区分原因，避免成为探测手段
            throw AuthPort.unauthenticated("令牌无效或已过期");
        }
        String subject = jwt.getSubject();
        String role = jwt.getClaimAsString(ROLE_CLAIM);
        if (subject == null || role == null || !KNOWN_ROLES.contains(role)) {
            // 合法令牌必然带可识别的角色；缺失或认不出即拒绝（失败即拒绝）
            throw AuthPort.unauthenticated("令牌无效或已过期");
        }
        return new Subject(subject, role);
    }

    private static SecretKey secretKey(String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
