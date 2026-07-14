package com.xiaou.moment.mapper;

import com.xiaou.moment.domain.MomentFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 动态收藏Mapper
 */
@Mapper
public interface MomentFavoriteMapper {
    
    /**
     * 插入收藏记录
     */
    int insert(MomentFavorite favorite);

    /**
     * Insert a favorite unless the user already favorited the moment.
     *
     * @return 1 when inserted, 0 when the unique key already exists
     */
    int insertIfAbsent(MomentFavorite favorite);
    
    /**
     * 删除收藏记录
     */
    int delete(@Param("momentId") Long momentId, @Param("userId") Long userId);
    
    /**
     * 根据ID删除收藏记录
     */
    int deleteById(Long id);
    
    /**
     * 查询用户是否收藏了该动态
     */
    MomentFavorite selectByMomentIdAndUserId(@Param("momentId") Long momentId, @Param("userId") Long userId);
    
    /**
     * 批量查询用户对多个动态的收藏状态
     */
    List<Long> selectFavoritedMomentIds(@Param("momentIds") List<Long> momentIds, @Param("userId") Long userId);
    
    /**
     * 查询用户的收藏列表
     * 注意：使用PageHelper分页，不需要offset和limit参数
     */
    List<Long> selectFavoriteMomentIdsByUserId(@Param("userId") Long userId);
    
    /**
     * 统计用户收藏总数
     */
    Long countByUserId(Long userId);
}

