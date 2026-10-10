package io.github.xiaou61.account.internal.registration;

import io.github.xiaou61.account.internal.credential.EmailCodeService;
import io.github.xiaou61.account.internal.token.AuthTokens;
import io.github.xiaou61.account.internal.token.TokenService;
import io.github.xiaou61.account.internal.user.PasswordHasher;
import io.github.xiaou61.account.internal.user.User;
import io.github.xiaou61.account.internal.user.UserMapper;
import io.github.xiaou61.platform.BizException;
import io.github.xiaou61.platform.ErrorCode;
import io.github.xiaou61.security.AuthPort;
import java.time.Instant;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 注册。
 *
 * <p><b>步骤顺序是设计的一部分，不是随手写的</b>：
 *
 * <ol>
 *   <li><b>先校验邮箱验证码</b>——这同时做了两件事：证明请求者控制该邮箱，以及把账号枚举的
 *       成本抬到"先过图形验证码 + 真的收到邮件"。</li>
 *   <li><b>再查用户名/邮箱占用</b>——因此"这个邮箱已注册"只会告诉一个已经能收到该邮箱邮件的人。
 *       如果反过来先查占用，任何人拿一串邮箱就能问出哪些已注册。</li>
 *   <li><b>插入时仍有竞态</b>（两个请求同时通过第 2 步），所以唯一索引兜底，捕获冲突后
 *       给出与第 2 步一致的提示，而不是 500。</li>
 * </ol>
 *
 * <p>注册出来的一律是学习者角色。管理员只能由种子迁移或运维手段产生——否则任何人都能
 * 通过改一个字段把自己变成管理员。
 */
@Service
public class RegistrationService {

    private final EmailCodeService emailCodeService;
    private final UserMapper userMapper;
    private final PasswordHasher passwordHasher;
    private final TokenService tokenService;

    RegistrationService(EmailCodeService emailCodeService,
                        UserMapper userMapper,
                        PasswordHasher passwordHasher,
                        TokenService tokenService) {
        this.emailCodeService = emailCodeService;
        this.userMapper = userMapper;
        this.passwordHasher = passwordHasher;
        this.tokenService = tokenService;
    }

    @Transactional
    public AuthTokens register(String username, String email, String rawPassword, String code) {
        String normalizedUsername = username.trim();
        String normalizedEmail = email.trim();

        emailCodeService.verifyAndConsume(normalizedEmail, code);

        if (userMapper.findByUsername(normalizedUsername).isPresent()) {
            throw new BizException(ErrorCode.CONFLICT, "用户名已被占用");
        }
        if (userMapper.findByEmail(normalizedEmail).isPresent()) {
            throw new BizException(ErrorCode.CONFLICT, "邮箱已被注册");
        }

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordHasher.hash(rawPassword));
        user.setRole(AuthPort.ROLE_LEARNER);
        // 注册本身就走邮箱验证码，所以此刻邮箱已验证
        user.setEmailVerifiedAt(Instant.now());

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            // 并发下两个请求同时通过了上面的占用检查；唯一索引是最后一道闸
            throw new BizException(ErrorCode.CONFLICT, "用户名或邮箱已被占用");
        }

        return tokenService.issueFor(user);
    }
}
