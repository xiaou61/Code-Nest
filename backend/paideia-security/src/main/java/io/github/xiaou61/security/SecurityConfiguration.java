package io.github.xiaou61.security;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 安全装配：无状态资源服务。
 *
 * <p>不建会话、不用 Cookie，因此关闭 CSRF——浏览器会话认证在桌面壳里本就不可靠
 * （页面来源于自定义协议，{@code SameSite}/{@code Secure} 语义失效），统一走 Authorization 头。
 *
 * <p>放行规则：健康检查、错误页、以及 <b>{@code /api/v1/auth/**}</b> 下的凭据交换端点；
 * 其余（含 Actuator 的其他端点）一律要求已认证。
 *
 * <p><b>前缀纪律</b>：{@code /api/v1/auth/**} 整体放行 —— 该前缀下只允许放"用凭据换令牌"
 * 或"令牌自身操作"的端点（验证码、发信、注册、登录、刷新、登出）。**不得**把任何读取
 * 业务数据的端点挂到这个前缀下，否则它会默默变成公开接口。这条靠约定而不是靠配置拦住，
 * 所以写在这里显式提醒。
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfiguration.class);

    /** 角色 claim 对应的权限前缀，与 Spring Security 的 {@code hasRole} 约定一致。 */
    private static final String ROLE_AUTHORITY_PREFIX = "ROLE_";

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

    /**
     * 把角色 claim 映射为权限。
     *
     * <p>必须显式提供：Spring 默认只从 {@code scope}/{@code scp} 读权限，而我们的角色在
     * {@code role} claim 里 —— 不映射的话令牌通过校验却没有任何权限，表现为"已登录但什么都不能做"。
     *
     * <p>角色值是小写的（与数据库和前端一致），权限名统一大写为 {@code ROLE_ADMIN} /
     * {@code ROLE_LEARNER}，与 {@code hasRole('ADMIN')} 的约定对齐。**认不出的角色映射为
     * 空权限**（认证通过但无权），不做任何降级猜测。
     */
    @Bean
    Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return jwt -> {
            String role = jwt.getClaimAsString(JwtAuthService.ROLE_CLAIM);
            List<GrantedAuthority> authorities = role == null || role.isBlank()
                    ? List.of()
                    : List.of(new SimpleGrantedAuthority(
                            ROLE_AUTHORITY_PREFIX + role.toUpperCase(Locale.ROOT)));
            return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter)
            throws Exception {
        http
                // 必须显式接管 CORS：否则预检 OPTIONS 会先被下面的 anyRequest().authenticated() 挡成 401，
                // 表现为"浏览器直接调通、跨域调用全挂"。规则本身来自 paideia-web 的 WebMvcConfigurer。
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        // 见类注释的前缀纪律：这个前缀下只放凭据交换类端点
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        // 知识库的管理端写接口。**必须排在下面的 anyRequest() 之前**：
                        // Spring Security 按声明顺序取第一个匹配的规则，顺序写反这条永不生效，
                        // 且不会有任何报错——只会在某天表现为"任何已登录的学习者都能改知识库"。
                        .requestMatchers("/api/v1/knowledge/admin/**").hasRole("ADMIN")
                        // 附件读取**免鉴权**（用户裁决）：<img> 不携带 Authorization 头，
                        // 而本项目无 Cookie，要求登录就等于图片显示不出来。限定 GET：
                        // 上传挂在 /api/v1/knowledge/admin/files，不能因为放行读取把写入也放开。
                        .requestMatchers(HttpMethod.GET, "/api/v1/knowledge/files/**").permitAll()
                        // 前端静态资源：同源部署时前端由本进程一并提供，入口页与资源目录必须放行，
                        // 否则连登录页都加载不出来（实测：未放行时 GET / 与 GET /admin/index.html 都是 401）。
                        // SPA 用 Hash 路由，所以**不必**为每个前端路由放行；**放行静态文件不等于放行数据**——
                        // 授权仍然只由 /api/** 上的规则决定，前端守卫从来不是授权边界。
                        .requestMatchers(HttpMethod.GET, "/", "/index.html", "/assets/**", "/admin/**", "/favicon.ico")
                        .permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }
}
