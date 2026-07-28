package com.xiaou.sre.service;

import java.util.List;

/**
 * SRE 调查可调用的固定只读工具边界。
 *
 * @author xiaou
 */
public interface SreReadOnlyInvestigationToolService {

    List<String> availableToolKeys();

    List<String> fallbackToolKeys(String alertName);

    SreReadOnlyToolResult execute(Long incidentId, Long runId, String toolKey);
}
