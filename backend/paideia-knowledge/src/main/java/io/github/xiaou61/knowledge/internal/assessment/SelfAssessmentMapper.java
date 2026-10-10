package io.github.xiaou61.knowledge.internal.assessment;

import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 自评访问。
 *
 * <p><b>每一条查询都带 {@code user_id} 条件</b>，包括"查一条标记"——隔离靠数据过滤而不是
 * 靠调用方记得过滤。这也是为什么这里按 id 查也要传 userId，而不是查出来再判断归属。
 */
@Mapper
public interface SelfAssessmentMapper {

    /**
     * 标记或改标记。唯一键 {@code (user_id, entry_id)} 让重复标记变成一次更新而不是两行。
     *
     * <p>用行别名写法（{@code AS new}）而不是已废弃的 {@code VALUES(level)}。
     */
    @Insert("""
            INSERT INTO knowledge_self_assessments (user_id, entry_id, level)
            VALUES (#{userId}, #{entryId}, #{level}) AS incoming
            ON DUPLICATE KEY UPDATE level = incoming.level
            """)
    int upsert(@Param("userId") Long userId, @Param("entryId") Long entryId, @Param("level") String level);

    /** 取消标记；返回受影响行数，0 表示本来就没有（调用方按幂等处理）。 */
    @Delete("""
            DELETE FROM knowledge_self_assessments
            WHERE user_id = #{userId} AND entry_id = #{entryId}
            """)
    int delete(@Param("userId") Long userId, @Param("entryId") Long entryId);

    @Select("""
            SELECT level FROM knowledge_self_assessments
            WHERE user_id = #{userId} AND entry_id = #{entryId}
            """)
    Optional<String> findLevel(@Param("userId") Long userId, @Param("entryId") Long entryId);

    /** 我标记过的条目。{@code level} 为 null 表示不过滤。只列**已发布**的条目。 */
    @Select("""
            <script>
            SELECT a.entry_id, e.title, a.level, a.updated_at
            FROM knowledge_self_assessments a
            JOIN knowledge_entries e ON e.id = a.entry_id
            WHERE a.user_id = #{userId} AND e.status = 'published'
            <if test="level != null">AND a.level = #{level}</if>
            ORDER BY a.updated_at DESC, a.entry_id DESC
            </script>
            """)
    List<AssessedEntry> findByUser(@Param("userId") Long userId, @Param("level") String level);
}
