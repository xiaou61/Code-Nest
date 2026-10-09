package io.github.xiaou61.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** 自签 JWT 的 {@link AuthPort} 实现。 */
public class JwtAuthService implements AuthPort {

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
    public Token issue(String subject) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.tokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(subject)
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
        try {
            return new Subject(decoder.decode(token).getSubject());
        } catch (RuntimeException exception) {
            // 签名不符、过期、格式错误都归为未认证：对外不区分原因，避免成为探测手段
            throw AuthPort.unauthenticated("令牌无效或已过期");
        }
    }

    private static SecretKey secretKey(String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
