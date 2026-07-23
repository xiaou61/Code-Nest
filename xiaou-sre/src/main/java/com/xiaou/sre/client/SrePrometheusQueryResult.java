package com.xiaou.sre.client;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 已经做过大小和字段裁剪的 Prometheus 查询结果。
 *
 * @author xiaou
 */
@Data
public class SrePrometheusQueryResult {

    private String resultType;
    private List<Map<String, Object>> results;
    private boolean truncated;
}
