package com.xiaou.moment.mapper;

import com.xiaou.moment.domain.MomentLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 动态点赞Mapper接口
 */
@Mapper
public interface MomentLikeMapper {
    
    /**
     * 插入点赞
     */
    int insert(MomentLike momentLike);

    /**
     * Insert a like unless the user already liked the moment.
     *
     * @return 1 when inserted, 0 when the unique key already exists
     */
    int insertIfAbsent(MomentLike momentLike);
    
    /**
     * 根据ID删除点赞
     */
    int deleteById(Long id);
    
    /**
     * 根据动态ID和用户ID删除点赞
     */
    int deleteByMomentIdAndUserId(@Param("momentId") Long momentId, @Param("userId") Long userId);
    
    /**
     * 根据动态ID和用户ID查询点赞
     */
    MomentLike selectByMomentIdAndUserId(@Param("momentId") Long momentId, @Param("userId") Long userId);
    
    /**
     * 根据动态ID查询点赞列表
     */
    List<MomentLike> selectByMomentId(Long momentId);
    
    /**
     * 批量查询用户对多个动态的点赞状态
     * @param momentIds 动态ID列表
     * @param userId 用户ID
     * @return 用户已点赞的动态ID列表
     */
    List<Long> selectLikedMomentIds(@Param("momentIds") List<Long> momentIds, @Param("userId") Long userId);
}
