package io.github.xiaou61.account.internal.token;

import java.time.Instant;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** refresh 令牌的状态访问。SQL 只写在这里。 */
@Mapper
public interface RefreshTokenMapper {

    @Insert("""
            INSERT INTO refresh_tokens (user_id, token_hash, family_id, expires_at)
            VALUES (#{userId}, #{tokenHash}, #{familyId}, #{expiresAt})
            """)
    int insert(@Param("userId") Long userId,
               @Param("tokenHash") String tokenHash,
               @Param("familyId") String familyId,
               @Param("expiresAt") Instant expiresAt);

    @Select("""
            SELECT id, user_id, token_hash, family_id, expires_at, revoked_at
            FROM refresh_tokens WHERE token_hash = #{tokenHash}
            """)
    Optional<RefreshToken> findByHash(@Param("tokenHash") String tokenHash);

    /** 轮换：把用掉的那一条作废。带 {@code revoked_at IS NULL} 是为了并发下只成功一次。 */
    @Update("UPDATE refresh_tokens SET revoked_at = #{revokedAt} WHERE id = #{id} AND revoked_at IS NULL")
    int revokeById(@Param("id") Long id, @Param("revokedAt") Instant revokedAt);

    /** 吊销整条链：登出，以及检测到令牌重用时终止该会话。 */
    @Update("""
            UPDATE refresh_tokens SET revoked_at = #{revokedAt}
            WHERE family_id = #{familyId} AND revoked_at IS NULL
            """)
    int revokeFamily(@Param("familyId") String familyId, @Param("revokedAt") Instant revokedAt);

    @Select("SELECT COUNT(*) FROM refresh_tokens WHERE family_id = #{familyId} AND revoked_at IS NULL")
    int countAliveInFamily(@Param("familyId") String familyId);
}
