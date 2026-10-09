package io.github.xiaou61.security;

import java.security.SecureRandom;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 安全装配：无状态资源服务。
 *
 * <p>不建会话、不用 Cookie，因此关闭 CSRF——浏览器会话认证在桌面壳里本就不可靠
 * （页面来源于自定义协议，{@code SameSite}/{@code Secure} 语义失效），统一走 Authorization 头。
 *
 * <p>放行规则：健康检查、错误页、以及仅开发 profile 打开的签发端点；
 * 其余（含 Actuator 的其他端点）一律要求已认证。
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfiguration.class);

    @Bean
    JwtAuthService authPort(AuthProperties properties) {
        if (!properties.hasSecret()) {
            log.warn("""
                    未配置签名密钥 paideia.auth.secret（环境变量 PAIDEIA_AUTH_SECRET），已生成本次进程的随机密钥。
                    后果：重启后此前签发的令牌全部失效，多实例之间也无法互相校验。
                    部署到任何他人可访问的环境之前，必须配置固定密钥。
                    """);
            return new JwtAuthService(properties.withSecret(randomSecret()));
        }
        return new JwtAuthService(properties);
    }

    private static String randomSecret() {
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 资源服务器复用同一个解码器：令牌的签发与校验规则只有一份，
     * 不会出现"签发用一套、校验用另一套"的错位。
     */
    @Bean
    JwtDecoder jwtDecoder(JwtAuthService authPort) {
        return authPort.decoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 必须显式接管 CORS：否则预检 OPTIONS 会先被下面的 anyRequest().authenticated() 挡成 401，
                // 表现为"浏览器直接调通、跨域调用全挂"。规则本身来自 paideia-web 的 WebMvcConfigurer。
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/token").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()));
        return http.build();
    }
}
