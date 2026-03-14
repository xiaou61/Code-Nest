package com.xiaou.aigrowth.mapper;

import com.xiaou.aigrowth.domain.AiGrowthCoachReplanLog;

/**
 * AI成长教练时间压缩重排日志Mapper
 */
public interface AiGrowthCoachReplanLogMapper {

    int insert(AiGrowthCoachReplanLog log);

    long countAll();

    long countToday();

    long countFallback();

    Double avgCompressionRate();
}
