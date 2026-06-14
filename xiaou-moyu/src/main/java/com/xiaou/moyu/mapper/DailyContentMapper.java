package com.xiaou.moyu.mapper;

import com.xiaou.moyu.domain.DailyContent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 每日内容数据访问层
 *
 * @author xiaou
 */
@Mapper
public interface DailyContentMapper {

    /**
     * 根据ID查询内容
     *
     * @param id 内容ID
     * @return 内容信息
     */
    DailyContent selectById(@Param("id") Long id);

    /**
     * 根据内容类型查询内容列表
     *
     * @param contentType 内容类型
     * @param status 状态
     * @param limit 限制数量
     * @return 内容列表
     */
    List<DailyContent> selectByContentType(@Param("contentType") Integer contentType,
                                           @Param("status") Integer status,
                                           @Param("limit") Integer limit);

    /**
     * 根据编程语言查询内容列表
     *
     * @param programmingLanguage 编程语言
     * @param status 状态
     * @param limit 限制数量
     * @return 内容列表
     */
    List<DailyContent> selectByProgrammingLanguage(@Param("programmingLanguage") String programmingLanguage,
                                                   @Param("status") Integer status,
                                                   @Param("limit") Integer limit);

    /**
     * 根据难度等级查询内容列表
     *
     * @param difficultyLevel 难度等级
     * @param status 状态
     * @param limit 限制数量
     * @return 内容列表
     */
    List<DailyContent> selectByDifficultyLevel(@Param("difficultyLevel") Integer difficultyLevel,
                                               @Param("status") Integer status,
                                               @Param("limit") Integer limit);

    /**
     * 随机获取指定类型的内容
     *
     * @param contentType 内容类型
     * @param status 状态
     * @param limit 限制数量
     * @return 内容列表
     */
    List<DailyContent> selectRandomByContentType(@Param("contentType") Integer contentType,
                                                 @Param("status") Integer status,
                                                 @Param("limit") Integer limit);

    /**
     * 根据用户偏好查询推荐内容
     *
     * @param contentTypes 内容类型数组
     * @param programmingLanguages 编程语言数组
     * @param difficultyLevel 难度等级
     * @param status 状态
     * @param limit 限制数量
     * @return 内容列表
     */
    List<DailyContent> selectByUserPreference(@Param("contentTypes") List<Integer> contentTypes,
                                              @Param("programmingLanguages") List<String> programmingLanguages,
                                              @Param("difficultyLevel") Integer difficultyLevel,
                                              @Param("status") Integer status,
                                              @Param("limit") Integer limit);

    /**
     * 查询热门内容
     *
     * @param status 状态
     * @param limit 限制数量
     * @return 内容列表
     */
    List<DailyContent> selectPopularContent(@Param("status") Integer status,
                                            @Param("limit") Integer limit);

    /**
     * 插入内容
     *
     * @param content 内容信息
     * @return 影响行数
     */
    int insert(DailyContent content);

    /**
     * 根据ID更新内容
     *
     * @param content 内容信息
     * @return 影响行数
     */
    int updateById(DailyContent content);

    /**
     * 根据ID更新内容状态
     *
     * @param id 内容ID
     * @param status 状态
     * @return 影响行数
     */
    int updateStatusById(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 增加查看次数
     *
     * @param id 内容ID
     * @return 影响行数
     */
    int incrementViewCount(@Param("id") Long id);

    /**
     * 增加点赞数
     *
     * @param id 内容ID
     * @return 影响行数
     */
    int incrementLikeCount(@Param("id") Long id);

    /**
     * 根据ID删除内容
     *
     * @param id 内容ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 统计内容总数
     *
     * @param status 状态
     * @return 内容总数
     */
    Long countByStatus(@Param("status") Integer status);

    /**
     * 批量查询内容
     *
     * @param ids 内容ID列表
     * @return 内容列表
     */
    List<DailyContent> selectBatchIds(@Param("ids") List<Long> ids);

    /**
     * 批量删除内容
     *
     * @param ids 内容ID列表
     * @return 影响行数
     */
    int deleteBatchIds(@Param("ids") List<Long> ids);
}
