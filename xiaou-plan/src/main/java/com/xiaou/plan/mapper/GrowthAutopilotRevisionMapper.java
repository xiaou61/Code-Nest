package com.xiaou.plan.mapper;

import com.xiaou.plan.domain.GrowthAutopilotRevision;
import org.apache.ibatis.annotations.Mapper;

/**
 * 成长计划版本快照 Mapper。
 */
@Mapper
public interface GrowthAutopilotRevisionMapper {

    int insert(GrowthAutopilotRevision revision);
}
