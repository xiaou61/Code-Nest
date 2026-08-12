package com.xiaou.moment.mapper;

import com.xiaou.moment.domain.Moment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 动态Mapper接口
 */
@Mapper
public interface MomentMapper {
    
    /**
     * 插入动态
     */
    int insert(Moment moment);
    
    /**
     * 根据ID删除动态
     */
    int deleteById(Long id);
    
    /**
     * 根据ID更新动态
     */
    int updateById(Moment moment);
    
    /**
     * 根据ID查询动态
     */
    Moment selectById(Long id);
    
    /**
     * 查询动态列表
     */
    List<Moment> selectList(Map<String, Object> params);
    
    /**
     * 查询动态总数
     */
    Long selectCount(Map<String, Object> params);
    
    /**
     * 增加点赞数
     */
    int incrementLikeCount(Long momentId);
    
    /**
     * 减少点赞数
     */
    int decrementLikeCount(Long momentId);
    
    /**
     * 增加评论数
     */
    int incrementCommentCount(Long momentId);
    
    /**
     * 减少评论数
     */
    int decrementCommentCount(Long momentId);
    
    /**
     * 统计总点赞数
     */
    Long selectTotalLikes(Map<String, Object> params);
    
    /**
     * 统计活跃用户数
     */
    Long selectActiveUsersCount(Map<String, Object> params);
    
    /**
     * 查询热门动态（按热度排序）
     */
    List<Moment> selectHotMoments(@Param("limit") Integer limit);
    
    /**
     * 搜索动态（按关键词）
     * 注意：使用PageHelper分页，不需要offset和limit参数
     */
    List<Moment> searchMoments(@Param("keyword") String keyword);
    
    /**
     * 根据用户ID查询动态列表
     */
    List<Moment> selectByUserId(@Param("userId") Long userId);
    
    /**
     * 统计用户动态总数
     */
    Long countByUserId(Long userId);
    
    /**
     * 统计用户获赞总数
     */
    Long countTotalLikesByUserId(Long userId);
    
    /**
     * 统计用户评论总数
     */
    Long countTotalCommentsByUserId(Long userId);
    
    /**
     * 增加浏览数
     */
    int incrementViewCount(Long momentId);

    /**
     * 按增量增加浏览数，避免按次数循环 UPDATE。
     */
    int incrementViewCountByDelta(@Param("momentId") Long momentId, @Param("delta") int delta);
    
    /**
     * 增加收藏数
     */
    int incrementFavoriteCount(Long momentId);
    
    /**
     * 减少收藏数
     */
    int decrementFavoriteCount(Long momentId);
    
    /**
     * 根据ID列表批量查询动态
     */
    List<Moment> selectByIds(@Param("ids") List<Long> ids);
    
    /**
     * 批量删除动态
     * @param ids 动态ID列表
     * @return 影响行数
     */
    int deleteBatchIds(@Param("ids") List<Long> ids);
}
