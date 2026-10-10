package io.github.xiaou61.account.internal.token;

import io.github.xiaou61.account.internal.support.Digest;
import io.github.xiaou61.account.internal.user.User;
import io.github.xiaou61.account.internal.user.UserMapper;
import io.github.xiaou61.platform.BizException;
import io.github.xiaou61.security.AuthPort;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 令牌的签发、轮换与吊销。
 *
 * <p><b>access 是 JWT（无状态），refresh 是随机串 + 状态表</b>。之所以让 refresh 有状态，
 * 是因为"轮换"的全部价值就在撤销与盗用检测上——纯无状态的 JWT 无法作废，
 * 那样轮换只是换个新串，旧串仍然有效到过期。
 *
 * <p><b>好一条链（family）的语义</b>：同一会话内每次轮换产生的新 refresh 都归到同一个
 * {@code family_id}。因此 ①登出可以一次终止整条会话；②**旧 refresh 被再次使用时**，
 * 说明它已经从某个地方泄露了出去，于是吊销整条链——对手拿到的和用户手上的同时失效，
 * 用户被迫重新登录，泄露的令牌因此失去价值。
 *
 * <p><b>access 的剩余寿命</b>：登出或吊销后，已签发的 access 在它自己的有效期（默认 15 分钟）
 * 内**仍然有效**。这是选择 JWT 的固有代价；要彻底即时失效就得引入服务端黑名单，
 * 本期不做，但这条必须写明而不是假装已解决。
 */
@Service
public class TokenService {

    /** refresh 的存活期。比 access 长得多，因为它的用途就是"不用天天登录"。 */
    private static final Duration REFRESH_TTL = Duration.ofDays(14);

    private final AuthPort authPort;
    private final RefreshTokenMapper refreshTokenMapper;
    private final UserMapper userMapper;
    private final SecureRandom random = new SecureRandom();

    TokenService(AuthPort authPort, RefreshTokenMapper refreshTokenMapper, UserMapper userMapper) {
        this.authPort = authPort;
        this.refreshTokenMapper = refreshTokenMapper;
        this.userMapper = userMapper;
    }

    /** 登录或注册成功后调用，开启一条新的令牌链。 */
    public AuthTokens issueFor(User user) {
        return issueWithin(user, UUID.randomUUID().toString());
    }

    /**
     * 用 refresh 换一组新令牌：旧的一次性作废，新的留在同一条链上。
     *
     * <p>{@code noRollbackFor = BizException.class} 是有意的：本方法在"检测到重用"时先写吊销
     * 再抛异常，用默认的"运行时异常即回滚"会把这次吊销一起回滚掉，重用检测就失效了。
     * 反过来，基础设施异常（非 BizException）仍会回滚，于是一次失败的轮换不会把用户的
     * 旧 refresh 白白消耗掉，客户端可以重试。
     *
     * @throws io.github.xiaou61.platform.BizException 令牌不存在、已过期、或**检测到重用**
     */
    @Transactional(noRollbackFor = BizException.class)
    public AuthTokens rotate(String presentedRefresh) {
        if (presentedRefresh == null || presentedRefresh.isBlank()) {
            throw AuthPort.unauthenticated("缺少刷新令牌");
        }
        RefreshToken stored = refreshTokenMapper
                .findByHash(Digest.sha256Hex(presentedRefresh))
                .orElseThrow(() -> AuthPort.unauthenticated("登录已失效，请重新登录"));

        Instant now = Instant.now();
        if (stored.getRevokedAt() != null) {
            // 重用检测：这个 refresh 已经被换过了，说明有第二个人拿着同一个令牌
            refreshTokenMapper.revokeFamily(stored.getFamilyId(), now);
            throw AuthPort.unauthenticated("检测到登录令牌被重复使用，已终止该会话，请重新登录");
        }
        if (!stored.getExpiresAt().isAfter(now)) {
            throw AuthPort.unauthenticated("登录已过期，请重新登录");
        }

        // 这条 UPDATE 带 `AND revoked_at IS NULL`，返回值就是"谁抢到了这次轮换"的唯一信号。
        // 返回 0 说明另一个并发请求已经换过了；它与盗用在这里没有区别，按同一套处理：
        // 吊销整条链、要求重新登录。
        if (refreshTokenMapper.revokeById(stored.getId(), now) == 0) {
            refreshTokenMapper.revokeFamily(stored.getFamilyId(), now);
            throw AuthPort.unauthenticated("检测到登录令牌被重复使用，已终止该会话，请重新登录");
        }
        User user = userMapper.findById(stored.getUserId())
                .orElseThrow(() -> AuthPort.unauthenticated("登录已失效，请重新登录"));
        return issueWithin(user, stored.getFamilyId());
    }
    /** 登出：吊销整条链。已签发的 access 按上文说明仍有剩余寿命。 */
    public void revoke(String presentedRefresh) {
        if (presentedRefresh == null || presentedRefresh.isBlank()) {
            return;
        }
        refreshTokenMapper
                .findByHash(Digest.sha256Hex(presentedRefresh))
                .ifPresent(token -> refreshTokenMapper.revokeFamily(token.getFamilyId(), Instant.now()));
    }

    private AuthTokens issueWithin(User user, String familyId) {
        AuthPort.Token access = authPort.issue(
                new AuthPort.Subject(String.valueOf(user.getId()), user.getRole()));

        // 256 位随机数：refresh 的安全性来自不可猜测，而不是来自签名
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant refreshExpiresAt = Instant.now().plus(REFRESH_TTL);

        refreshTokenMapper.insert(user.getId(), Digest.sha256Hex(refresh), familyId, refreshExpiresAt);

        return new AuthTokens(
                access.value(),
                access.expiresAt().toString(),
                refresh,
                refreshExpiresAt.toString(),
                AuthTokens.UserView.of(user));
    }
}
