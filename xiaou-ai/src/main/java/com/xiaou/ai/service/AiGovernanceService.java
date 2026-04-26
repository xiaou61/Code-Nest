package com.xiaou.ai.service;

import com.xiaou.ai.dto.governance.AiGovernanceOverviewResponse;

/**
 * AI治理服务
 *
 * @author xiaou
 */
public interface AiGovernanceService {

    /**
     * 获取AI运行治理总览
     *
     * @return AI治理总览
     */
    AiGovernanceOverviewResponse getOverview();
}
