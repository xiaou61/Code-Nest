package com.xiaou.aigrowth.mapper;

import com.xiaou.aigrowth.domain.AiGrowthCoachConfig;

import java.util.List;

/**
 * AI成长教练配置Mapper
 */
public interface AiGrowthCoachConfigMapper {

    List<AiGrowthCoachConfig> selectAll();

    AiGrowthCoachConfig selectByKey(String configKey);

    int insert(AiGrowthCoachConfig config);

    int updateByKey(AiGrowthCoachConfig config);
}
