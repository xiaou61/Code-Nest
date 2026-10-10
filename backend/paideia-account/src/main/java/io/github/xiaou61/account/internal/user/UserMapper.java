package io.github.xiaou61.account.internal.user;

import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 账号表访问。
 *
 * <p>SQL 只写在这里（沿用本项目 {@code @Mapper} + 注解/文本块的写法，不用 XML）。
 * 唯一性由数据库的唯一索引兜底，这里的查询只用于给出**友好的**错误提示；
 * 并发插入时靠捕获唯一索引冲突，而不是"先查再插"这种有竞态的写法。
 */
@Mapper
public interface UserMapper {

    @Insert("""
            INSERT INTO users (username, email, password_hash, role, email_verified_at)
            VALUES (#{username}, #{email}, #{passwordHash}, #{role}, #{emailVerifiedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    /**
     * 按用户名或邮箱查。两者都唯一，所以一条语句能覆盖"用哪个标识登录"。
     *
     * <p>库的排序规则是大小写不敏感的（{@code utf8mb4_0900_ai_ci}），因此
     * {@code Alice} 与 {@code alice} 会命中同一条记录。
     */
    @Select("""
            SELECT id, username, email, password_hash, role, email_verified_at
            FROM users
            WHERE username = #{identifier} OR email = #{identifier}
            LIMIT 1
            """)
    Optional<User> findByIdentifier(@Param("identifier") String identifier);

    @Select("""
            SELECT id, username, email, password_hash, role, email_verified_at
            FROM users WHERE username = #{username}
            """)
    Optional<User> findByUsername(@Param("username") String username);

    @Select("""
            SELECT id, username, email, password_hash, role, email_verified_at
            FROM users WHERE email = #{email}
            """)
    Optional<User> findByEmail(@Param("email") String email);

    @Select("SELECT id, username, email, password_hash, role, email_verified_at FROM users WHERE id = #{id}")
    Optional<User> findById(@Param("id") Long id);

    @Select("SELECT role FROM users WHERE id = #{id}")
    Optional<String> findRoleById(@Param("id") Long id);
}
