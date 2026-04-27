package com.xiaou.sqloptimizer.mapper;

import com.xiaou.sqloptimizer.domain.SqlOptimizeRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * SQL优化记录Mapper
 *
 * @author xiaou
 */
@Mapper
public interface SqlOptimizeRecordMapper {

    String RECORD_COLUMNS = "id, user_id, original_sql, explain_result, explain_format, table_structures, " +
            "mysql_version, analysis_result, score, is_favorite, create_time, update_time, deleted";

    /**
     * 插入记录
     */
    @Insert("INSERT INTO sql_optimize_record (user_id, original_sql, explain_result, explain_format, " +
            "table_structures, mysql_version, analysis_result, score, is_favorite, create_time, update_time, deleted) " +
            "VALUES (#{userId}, #{originalSql}, #{explainResult}, #{explainFormat}, " +
            "#{tableStructures}, #{mysqlVersion}, #{analysisResult}, #{score}, #{isFavorite}, #{createTime}, #{updateTime}, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SqlOptimizeRecord record);

    /**
     * 根据ID查询
     */
    @Select("SELECT " + RECORD_COLUMNS + " FROM sql_optimize_record WHERE id = #{id} AND deleted = 0")
    SqlOptimizeRecord selectById(Long id);

    /**
     * 根据用户ID分页查询
     */
    @Select("SELECT " + RECORD_COLUMNS + " FROM sql_optimize_record WHERE user_id = #{userId} AND deleted = 0 " +
            "ORDER BY create_time DESC LIMIT #{offset}, #{limit}")
    List<SqlOptimizeRecord> selectByUserId(@Param("userId") Long userId, 
                                            @Param("offset") int offset, 
                                            @Param("limit") int limit);

    /**
     * 查询用户全部记录（按时间倒序）
     */
    @Select("SELECT " + RECORD_COLUMNS + " FROM sql_optimize_record WHERE user_id = #{userId} AND deleted = 0 ORDER BY create_time DESC")
    List<SqlOptimizeRecord> selectAllByUserId(@Param("userId") Long userId);

    /**
     * 统计用户记录数
     */
    @Select("SELECT COUNT(*) FROM sql_optimize_record WHERE user_id = #{userId} AND deleted = 0")
    int countByUserId(Long userId);

    /**
     * 切换收藏状态
     */
    @Update("UPDATE sql_optimize_record SET is_favorite = #{isFavorite}, update_time = NOW() " +
            "WHERE id = #{id} AND user_id = #{userId}")
    int updateFavorite(@Param("id") Long id, @Param("userId") Long userId, @Param("isFavorite") Integer isFavorite);

    /**
     * 更新分析结果JSON
     */
    @Update("UPDATE sql_optimize_record SET analysis_result = #{analysisResult}, update_time = NOW() " +
            "WHERE id = #{id} AND user_id = #{userId} AND deleted = 0")
    int updateAnalysisResult(@Param("id") Long id, @Param("userId") Long userId, @Param("analysisResult") String analysisResult);

    /**
     * 逻辑删除
     */
    @Update("UPDATE sql_optimize_record SET deleted = 1, update_time = NOW() " +
            "WHERE id = #{id} AND user_id = #{userId}")
    int deleteById(@Param("id") Long id, @Param("userId") Long userId);
}
