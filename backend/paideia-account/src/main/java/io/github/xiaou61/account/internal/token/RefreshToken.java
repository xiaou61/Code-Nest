package io.github.xiaou61.account.internal.token;

import java.time.Instant;

/**
 * 一条 refresh 令牌记录。可变类是为了让 MyBatis 能按列名回填。
 *
 * <p>库里**没有**令牌明文，只有它的 SHA-256。因此拖库也拿不到可用的令牌。
 */
public class RefreshToken {

    private Long id;
    private Long userId;
    private String tokenHash;
    private String familyId;
    private Instant expiresAt;
    private Instant revokedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public String getFamilyId() {
        return familyId;
    }

    public void setFamilyId(String familyId) {
        this.familyId = familyId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }
}
