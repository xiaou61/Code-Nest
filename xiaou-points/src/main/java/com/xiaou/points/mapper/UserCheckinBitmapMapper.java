package com.xiaou.points.mapper;

import com.xiaou.points.domain.UserCheckinBitmap;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 用户打卡位图Mapper接口
 * 
 * @author xiaou
 */
@Mapper
public interface UserCheckinBitmapMapper {
    
    /**
     * 根据用户ID和年月查询位图记录
     */
    UserCheckinBitmap selectByUserIdAndYearMonth(@Param("userId") Long userId, 
                                                @Param("yearMonth") String yearMonth);
    
    /**
     * 插入位图记录
     */
    int insert(UserCheckinBitmap userCheckinBitmap);
    
    /**
     * 更新位图记录
     */
    int updateBitmap(UserCheckinBitmap userCheckinBitmap);
    
    /**
     * 查询用户指定月份范围的位图记录
     */
    List<UserCheckinBitmap> selectByUserIdAndMonthRange(@Param("userId") Long userId,
                                                       @Param("startYearMonth") String startYearMonth,
                                                       @Param("endYearMonth") String endYearMonth);
    
    /**
     * 查询用户最近的位图记录
     */
    UserCheckinBitmap selectLatestByUserId(@Param("userId") Long userId);

    /**
     * 批量查询多个用户最近的位图记录
     */
    List<UserCheckinBitmap> selectLatestByUserIds(@Param("userIds") List<Long> userIds);
    
    /**
     * 查询用户所有位图记录
     */
    List<UserCheckinBitmap> selectByUserId(@Param("userId") Long userId);
    
    /**
     * 统计用户总打卡天数
     */
    Integer selectTotalCheckinDays(@Param("userId") Long userId);
    
    /**
     * 查询活跃用户统计（有打卡记录的用户）
     */
    Integer selectActiveCheckinUserCount();
    
    /**
     * 查询指定日期的打卡用户数
     */
    Integer selectCheckinUserCountByDate(@Param("date") LocalDate date);
}
