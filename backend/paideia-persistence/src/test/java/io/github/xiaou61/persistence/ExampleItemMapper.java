package io.github.xiaou61.persistence;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 示例 mapper，用于验证本模块约定的分页写法：分页参数由调用方显式传入，
 * SQL 自己写 LIMIT/OFFSET，总数由独立的 count 语句提供。
 *
 * <p>标注 {@code @Mapper} 是为了让 paideia-app 的自动扫描能发现它（被授权的隔离测试要用它）。
 */
@Mapper
public interface ExampleItemMapper {

    @Insert("INSERT INTO t_example_item (owner_id, title) VALUES (#{ownerId}, #{title})")
    int insert(@Param("ownerId") String ownerId, @Param("title") String title);

    /** 带归属过滤的分页查询：归属条件由调用方从令牌解析后传入，不接受客户端直传的用户标识。 */
    @Select("""
            SELECT id, owner_id, title
            FROM t_example_item
            WHERE owner_id = #{ownerId}
            ORDER BY id
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<ExampleItem> pageByOwner(@Param("ownerId") String ownerId,
                                  @Param("limit") int limit,
                                  @Param("offset") long offset);

    @Select("SELECT COUNT(*) FROM t_example_item WHERE owner_id = #{ownerId}")
    long countByOwner(@Param("ownerId") String ownerId);

    /** 不带归属过滤的分页查询，仅用于对照，说明越权取值确实能读到别人的数据。 */
    @Select("""
            SELECT id, owner_id, title
            FROM t_example_item
            ORDER BY id
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<ExampleItem> pageAll(@Param("limit") int limit, @Param("offset") long offset);

    @Insert("DELETE FROM t_example_item")
    int deleteAll();
}
