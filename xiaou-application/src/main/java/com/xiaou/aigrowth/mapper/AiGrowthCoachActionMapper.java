package com.xiaou.aigrowth.mapper;

import com.xiaou.aigrowth.domain.AiGrowthCoachAction;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI成长教练动作Mapper
 */
public interface AiGrowthCoachActionMapper {

    List<AiGrowthCoachAction> selectBySnapshotId(Long snapshotId);

    AiGrowthCoachAction selectById(Long id);

    int insert(AiGrowthCoachAction action);

    int deleteBySnapshotId(Long snapshotId);

    int updateStatus(@Param("id") Long id, @Param("userId") Long userId, @Param("status") String status);

    long countAll();

    long countDone();
}
