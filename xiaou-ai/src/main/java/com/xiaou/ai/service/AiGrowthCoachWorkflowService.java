package com.xiaou.ai.service;

import com.xiaou.ai.dto.growthcoach.GrowthCoachChatRequest;
import com.xiaou.ai.dto.growthcoach.GrowthCoachChatResult;
import com.xiaou.ai.dto.growthcoach.GrowthCoachActionReplanRequest;
import com.xiaou.ai.dto.growthcoach.GrowthCoachActionReplanResult;
import com.xiaou.ai.dto.growthcoach.GrowthCoachSnapshotAiRequest;
import com.xiaou.ai.dto.growthcoach.GrowthCoachSnapshotAiResult;

/**
 * AI成长教练工作流服务
 */
public interface AiGrowthCoachWorkflowService {

    GrowthCoachSnapshotAiResult generateSnapshot(GrowthCoachSnapshotAiRequest request);

    GrowthCoachActionReplanResult replan(GrowthCoachActionReplanRequest request);

    GrowthCoachChatResult chat(GrowthCoachChatRequest request);
}
