package io.github.xiaou61.knowledge.internal.entry;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 知识条目访问。
 *
 * <p>SQL 只写在这里（沿用本项目 {@code @Mapper} + 注解/文本块的写法，不用 XML）。
 *
 * <p><b>学习者侧的每一条查询都在 SQL 里写死 {@code status = 'published'}</b>，
 * 不在控制器里"查完再判断"。这样漏改一处的后果只是少一个接口，而不是把草稿泄露出去。
 *
 * <p>检索走两条路（由 {@code SearchQuery} 判定），都用参数绑定：
 * 长查询走 `MATCH … AGAINST`（ngram 全文索引），短查询退回 `LIKE`。
 * 用自然语言模式而不是 BOOLEAN 模式——BOOLEAN 把 `+ - * "` 当操作符，而检索串是用户随手
 * 输入的，把它当查询语法解析既容易报错也容易给出意外结果。
 */
@Mapper
public interface EntryMapper {

    @Select("""
            <script>
            SELECT id, category_id, title, status, published_at, updated_at
            FROM knowledge_entries
            WHERE status = 'published'
            <if test="categoryIds != null">
              AND category_id IN
              <foreach item="cid" collection="categoryIds" open="(" separator="," close=")">#{cid}</foreach>
            </if>
            <if test="keyword != null">
              <choose>
                <when test="fulltext">AND MATCH(title, body) AGAINST(#{keyword})</when>
                <otherwise>AND (title LIKE #{likePattern} OR body LIKE #{likePattern})</otherwise>
              </choose>
            </if>
            ORDER BY published_at DESC, id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<Entry> pagePublished(@Param("categoryIds") List<Long> categoryIds,
                              @Param("keyword") String keyword,
                              @Param("fulltext") boolean fulltext,
                              @Param("likePattern") String likePattern,
                              @Param("limit") int limit,
                              @Param("offset") long offset);

    @Select("""
            <script>
            SELECT COUNT(*) FROM knowledge_entries
            WHERE status = 'published'
            <if test="categoryIds != null">
              AND category_id IN
              <foreach item="cid" collection="categoryIds" open="(" separator="," close=")">#{cid}</foreach>
            </if>
            <if test="keyword != null">
              <choose>
                <when test="fulltext">AND MATCH(title, body) AGAINST(#{keyword})</when>
                <otherwise>AND (title LIKE #{likePattern} OR body LIKE #{likePattern})</otherwise>
              </choose>
            </if>
            </script>
            """)
    long countPublished(@Param("categoryIds") List<Long> categoryIds,
                        @Param("keyword") String keyword,
                        @Param("fulltext") boolean fulltext,
                        @Param("likePattern") String likePattern);

    @Select("""
            SELECT id, category_id, title, body, status, published_at, created_at, updated_at
            FROM knowledge_entries WHERE id = #{id} AND status = 'published'
            """)
    Optional<Entry> findPublishedById(@Param("id") Long id);

    /**
     * 同分类内的上一篇／下一篇，按 (published_at, id) 排序。
     *
     * <p>用行值比较 `(a, b) < (c, d)` 一次比两个字段，避免"同一秒发布时按 id 再分不出先后"
     * 导致某一篇重复出现或跳过。
     *
     * <p><b>这里写的是裸 `&lt;` 而不是 `&amp;lt;`</b>：MyBatis 只在 `<script>` 块里按 XML 解析
     * 注解文本，普通注解里的实体不会被解码——写成 `&amp;lt;` 会原样发给 MySQL 变成语法错误。
     */
    @Select("""
            SELECT id, title FROM knowledge_entries
            WHERE status = 'published' AND category_id = #{categoryId}
              AND (published_at, id) < (#{publishedAt}, #{id})
            ORDER BY published_at DESC, id DESC LIMIT 1
            """)
    Optional<Entry> findPreviousInCategory(@Param("categoryId") Long categoryId,
                                           @Param("publishedAt") Instant publishedAt,
                                           @Param("id") Long id);

    /** 见 {@link #findPreviousInCategory} 关于裸 `<` 的说明。 */
    @Select("""
            SELECT id, title FROM knowledge_entries
            WHERE status = 'published' AND category_id = #{categoryId}
              AND (published_at, id) > (#{publishedAt}, #{id})
            ORDER BY published_at ASC, id ASC LIMIT 1
            """)
    Optional<Entry> findNextInCategory(@Param("categoryId") Long categoryId,
                                       @Param("publishedAt") Instant publishedAt,
                                       @Param("id") Long id);

    /** 反向链接：把当前条目当作**前置**的那些条目（只列已发布的）。 */
    // 关系类查询统一放在 RelationMapper（前置／反向／相关三种视角在一处更好读）。

    // ---- 管理端：可以看见草稿 ----

    @Select("""
            <script>
            SELECT id, category_id, title, status, published_at, updated_at
            FROM knowledge_entries
            <where>
              <if test="status != null">status = #{status}</if>
            </where>
            ORDER BY updated_at DESC, id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<Entry> pageAdmin(@Param("status") String status,
                          @Param("limit") int limit,
                          @Param("offset") long offset);

    @Select("""
            <script>
            SELECT COUNT(*) FROM knowledge_entries
            <where>
              <if test="status != null">status = #{status}</if>
            </where>
            </script>
            """)
    long countAdmin(@Param("status") String status);

    /** 管理端详情：**不过滤状态**，草稿预览靠它。 */
    @Select("""
            SELECT id, category_id, title, body, status, published_at, created_at, updated_at
            FROM knowledge_entries WHERE id = #{id}
            """)
    Optional<Entry> findByIdIncludingDraft(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM knowledge_entries WHERE category_id = #{id}")
    long countByCategory(@Param("id") Long id);

    @Insert("""
            INSERT INTO knowledge_entries (category_id, title, body, status, published_at)
            VALUES (#{categoryId}, #{title}, #{body}, #{status}, #{publishedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Entry entry);

    @Update("""
            UPDATE knowledge_entries
            SET category_id = #{categoryId}, title = #{title}, body = #{body},
                status = #{status}, published_at = #{publishedAt}
            WHERE id = #{id}
            """)
    int update(Entry entry);

    @Delete("DELETE FROM knowledge_entries WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
