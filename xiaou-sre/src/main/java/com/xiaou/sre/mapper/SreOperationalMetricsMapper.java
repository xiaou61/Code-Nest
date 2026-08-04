package com.xiaou.sre.mapper;

import com.xiaou.sre.metrics.SreOperationalMetricsRow;
import org.apache.ibatis.annotations.Mapper;

/**
 * Read-only aggregate query for SRE operational gauges.
 *
 * @author xiaou
 */
@Mapper
public interface SreOperationalMetricsMapper {

    SreOperationalMetricsRow selectSnapshot();
}
