package com.xiaou.mockinterview.mapper;

import com.xiaou.mockinterview.domain.JobBattleMatchRecord;
import com.xiaou.mockinterview.dto.request.JobBattleMatchEngineHistoryRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 岗位匹配引擎记录Mapper
 *
 * @author xiaou
 */
@Mapper
public interface JobBattleMatchRecordMapper {

    /**
     * 新增记录
     */
    int insert(JobBattleMatchRecord record);

    /**
     * 分页查询用户历史记录
     */
    List<JobBattleMatchRecord> selectPageByUserId(@Param("userId") Long userId,
                                                  @Param("request") JobBattleMatchEngineHistoryRequest request);

    /**
     * 根据ID查询详情
     */
    JobBattleMatchRecord selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 查询最近一次分析结果
     */
    JobBattleMatchRecord selectLatestByUserId(@Param("userId") Long userId);
}

