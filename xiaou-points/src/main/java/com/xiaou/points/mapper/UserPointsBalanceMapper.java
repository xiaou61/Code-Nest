package com.xiaou.points.mapper;

import com.xiaou.points.domain.UserPointsBalance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户积分余额Mapper接口
 * 
 * @author xiaou
 */
@Mapper
public interface UserPointsBalanceMapper {
    
    /**
     * 查询用户积分余额
     */
    UserPointsBalance selectByUserId(@Param("userId") Long userId);
    
    /**
     * 插入用户积分记录
     */
    int insert(UserPointsBalance userPointsBalance);
    
    /**
     * 更新用户积分余额
     */
    int updateBalance(@Param("userId") Long userId, @Param("totalPoints") Integer totalPoints);
    
    /**
     * 增加用户积分
     */
    int addPoints(@Param("userId") Long userId, @Param("points") Integer points);
    
    /**
     * 减少用户积分
     */
    int subtractPoints(@Param("userId") Long userId, @Param("points") Integer points);
    
    /**
     * 扣除用户积分（带余额检查）
     */
    int deductPoints(@Param("userId") Long userId, @Param("points") Integer points);
    
    /**
     * 查询积分排行榜
     */
    List<UserPointsBalance> selectTopUsers(@Param("limit") Integer limit);
    
    /**
     * 查询积分余额列表（分页，支持用户名/积分范围过滤和排序）
     */
    List<UserPointsBalance> selectBalancePage(
            @Param("userName") String userName,
            @Param("minPoints") Integer minPoints,
            @Param("maxPoints") Integer maxPoints,
            @Param("orderBy") String orderBy,
            @Param("orderDirection") String orderDirection
    );
    
    /**
     * 统计总积分发放量
     */
    Long selectTotalPointsSum();
    
    /**
     * 统计积分用户数量
     */
    Integer selectActiveUserCount();
}
