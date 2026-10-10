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
     * 按用户名或邮箱查。两者都唯一，所以"用哪个标识登录"都能落到唯一索引上。
     *
     * <p>用两次单列查询，而不是 {@code username = #{identifier} OR email = #{identifier}}：
     * 跨两列的 OR 用不上任何一个唯一索引（随用户数退化成全表扫），而且当 A 的用户名恰好等于
     * B 的邮箱时，一条语句返回哪一行并不确定。分开查则顺序明确：先用户名，再邮箱。
     *
     * <p>库的排序规则是大小写不敏感的（{@code utf8mb4_0900_ai_ci}），因此
     * {@code Alice} 与 {@code alice} 会命中同一条记录。
     */
    default Optional<User> findByIdentifier(String identifier) {
        return findByUsername(identifier).or(() -> findByEmail(identifier));
    }

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
