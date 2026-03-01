package com.xiaou.mockinterview.mapper;

import com.xiaou.mockinterview.domain.JobBattlePlanRecord;
import com.xiaou.mockinterview.dto.request.JobBattlePlanHistoryRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 求职作战台计划记录Mapper
 *
 * @author xiaou
 */
@Mapper
public interface JobBattlePlanRecordMapper {

    /**
     * 插入计划记录
     */
    int insert(JobBattlePlanRecord record);

    /**
     * 分页查询用户计划历史
     */
    List<JobBattlePlanRecord> selectPageByUserId(@Param("userId") Long userId,
                                                 @Param("request") JobBattlePlanHistoryRequest request);

    /**
     * 根据ID查询用户计划详情
     */
    JobBattlePlanRecord selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}

