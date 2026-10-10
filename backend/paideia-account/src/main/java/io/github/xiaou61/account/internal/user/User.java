package io.github.xiaou61.account.internal.user;

import java.time.Instant;

/**
 * 账号实体。
 *
 * <p>刻意是可变的普通类而不是 record：{@code @Options(useGeneratedKeys = true)} 需要 MyBatis
 * 在插入后把自增主键写回对象，而 record 没有 setter。
 *
 * <p>{@code passwordHash} 只在本模块内部流转，**不得**出现在任何对外响应里——
 * 对外的用户视图是 {@code TokenIssuer} 里的那个小记录。
 */
public class User {

    private Long id;
    private String username;
    private String email;
    private String passwordHash;
    private String role;
    private Instant emailVerifiedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Instant getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public void setEmailVerifiedAt(Instant emailVerifiedAt) {
        this.emailVerifiedAt = emailVerifiedAt;
    }

    /** 出现在日志里时只留 id 与用户名，不带邮箱与口令摘要。 */
    @Override
    public String toString() {
        return "User{id=" + id + ", username=" + username + ", role=" + role + "}";
    }
}
