package io.github.xiaou61.account.internal.login;

import io.github.xiaou61.account.internal.support.Digest;
import io.github.xiaou61.account.internal.support.RateLimiter;
import io.github.xiaou61.account.internal.token.AuthTokens;
import io.github.xiaou61.account.internal.token.TokenService;
import io.github.xiaou61.account.internal.user.PasswordHasher;
import io.github.xiaou61.account.internal.user.User;
import io.github.xiaou61.account.internal.user.UserMapper;
import io.github.xiaou61.security.AuthPort;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * 登录。用户名与邮箱都能作为标识。
 *
 * <p>三条纪律，都是安全敏感路径上不能省的东西：
 *
 * <ul>
 *   <li><b>失败信息不区分原因</b>："账号不存在"与"密码错误"必须给出同一句话，
 *       否则登录接口就成了账号枚举器。</li>
 *   <li><b>耗时也要对齐</b>：账号不存在时对着一个假摘要跑一次同样的 BCrypt 校验，
 *       否则响应快慢本身就泄露了账号是否存在。</li>
 *   <li><b>限流只统计失败</b>：用"每次调用都计数"的写法会导致用户正常登录几次就被封——
 *       所以进门只查、失败才记，成功即清零。</li>
 * </ul>
 */
@Service
public class LoginService {

    private static final Duration FAILURE_WINDOW = Duration.ofMinutes(15);
    private static final int FAILURES_PER_IDENTIFIER = 5;
    private static final int FAILURES_PER_SOURCE = 20;

    /** 与账号不存在时保持一致的文案。 */
    private static final String GENERIC_FAILURE = "用户名或密码不正确";

    private final UserMapper userMapper;
    private final PasswordHasher passwordHasher;
    private final TokenService tokenService;
    private final RateLimiter rateLimiter;

    LoginService(UserMapper userMapper,
                 PasswordHasher passwordHasher,
                 TokenService tokenService,
                 RateLimiter rateLimiter) {
        this.userMapper = userMapper;
        this.passwordHasher = passwordHasher;
        this.tokenService = tokenService;
        this.rateLimiter = rateLimiter;
    }

    public AuthTokens login(String identifier, String rawPassword, String sourceAddress) {
        String trimmed = identifier.trim();
        String identifierKey = "login-id:" + Digest.sha256Hex(trimmed.toLowerCase(Locale.ROOT));
        String sourceKey = "login-source:" + Digest.sha256Hex(sourceAddress);

        rateLimiter.checkUnderLimit(identifierKey, FAILURES_PER_IDENTIFIER, FAILURE_WINDOW);
        rateLimiter.checkUnderLimit(sourceKey, FAILURES_PER_SOURCE, FAILURE_WINDOW);

        Optional<User> found = userMapper.findByIdentifier(trimmed);
        if (found.isEmpty()) {
            // 关键：账号不存在也要付出同样的代价，否则响应耗时就泄露了账号是否存在
            passwordHasher.wasteTimeLikeAMatch();
            rateLimiter.recordFailure(identifierKey, FAILURE_WINDOW);
            rateLimiter.recordFailure(sourceKey, FAILURE_WINDOW);
            throw AuthPort.unauthenticated(GENERIC_FAILURE);
        }

        User user = found.get();
        if (!passwordHasher.matches(rawPassword, user.getPasswordHash())) {
            rateLimiter.recordFailure(identifierKey, FAILURE_WINDOW);
            rateLimiter.recordFailure(sourceKey, FAILURE_WINDOW);
            throw AuthPort.unauthenticated(GENERIC_FAILURE);
        }

        // 登录成功清掉失败计数，避免历史失败把正常用户挡在门外
        rateLimiter.clear(identifierKey);
        return tokenService.issueFor(user);
    }
}
