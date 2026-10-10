package io.github.xiaou61.knowledge.internal.relation;

import io.github.xiaou61.knowledge.internal.entry.Entry;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 条目关系访问。
 *
 * <p>关系是**有向边 + 类型**：`prerequisite`（from 是 to 的前置）与 `related`
 * （无向语义，只存一条边，查询时两边都看）。
 *
 * <p>学习者侧一律只列**已发布**的对端：草稿出现在关系里等于间接泄露了"有这么个未发布的条目"。
 */
@Mapper
public interface RelationMapper {

    /**
     * 本条目**依赖**的前置。
     *
     * <p>边是 {@code (X → 本条目)}：X 是本条目的前置。所以查的是**入边**。
     */
    @Select("""
            SELECT e.id, e.title, e.published_at FROM knowledge_entry_relations r
            JOIN knowledge_entries e ON e.id = r.from_entry_id
            WHERE r.to_entry_id = #{id} AND r.relation_type = 'prerequisite'
              AND e.status = 'published'
            ORDER BY e.published_at DESC, e.id DESC
            """)
    List<Entry> findPrerequisites(@Param("id") Long id);

    /**
     * 反向链接：把本条目当作**前置**的条目。
     *
     * <p>边是 {@code (本条目 → Y)}：Y 依赖本条目。所以查的是**出边**。
     *
     * <p>这两个方法**互为镜像、极易写反**（查入边还是出边），而写反之后两边都会返回
     * "看起来合理"的结果——因此集成测试对两个方向都断言，见 `KnowledgeReadIntegrationTest`。
     */
    @Select("""
            SELECT e.id, e.title, e.published_at FROM knowledge_entry_relations r
            JOIN knowledge_entries e ON e.id = r.to_entry_id
            WHERE r.from_entry_id = #{id} AND r.relation_type = 'prerequisite'
              AND e.status = 'published'
            ORDER BY e.published_at DESC, e.id DESC
            """)
    List<Entry> findDependents(@Param("id") Long id);

    /**
     * 与本条目相关的内容。
     *
     * <p>`related` 是无向语义但只存一条边，因此两端都要看：用 `IF` 取出"另一头"的 id，
     * 而不是用 `UNION` 把两个方向拼起来——一条查询更好读，也少一次扫描。
     */
    @Select("""
            SELECT e.id, e.title, e.published_at FROM knowledge_entry_relations r
            JOIN knowledge_entries e
              ON e.id = IF(r.from_entry_id = #{id}, r.to_entry_id, r.from_entry_id)
            WHERE r.relation_type = 'related'
              AND (r.from_entry_id = #{id} OR r.to_entry_id = #{id})
              AND e.status = 'published'
            ORDER BY e.published_at DESC, e.id DESC
            """)
    List<Entry> findRelated(@Param("id") Long id);

    /**
     * 与本条目有关的**全部**边（两个方向），带 id。
     *
     * <p>管理端编辑关系时需要 id 才能删；学习者侧的三个视角（前置／相关／反向链接）
     * 是给人读的，不带 id。两者用途不同，因此分开两条查询。
     */
    @Select("""
            SELECT id, from_entry_id, to_entry_id, relation_type
            FROM knowledge_entry_relations
            WHERE from_entry_id = #{id} OR to_entry_id = #{id}
            ORDER BY id
            """)
    List<Relation> findTouching(@Param("id") Long id);

    @Insert("""
            INSERT INTO knowledge_entry_relations (from_entry_id, to_entry_id, relation_type)
            VALUES (#{fromEntryId}, #{toEntryId}, #{relationType})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Relation relation);

    @Delete("DELETE FROM knowledge_entry_relations WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Select("""
            SELECT id, from_entry_id, to_entry_id, relation_type
            FROM knowledge_entry_relations WHERE id = #{id}
            """)
    Relation findById(@Param("id") Long id);
}
