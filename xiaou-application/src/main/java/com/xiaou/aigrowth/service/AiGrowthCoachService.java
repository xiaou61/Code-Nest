package com.xiaou.aigrowth.service;

import com.xiaou.aigrowth.dto.request.AiGrowthCoachChatRequest;
import com.xiaou.aigrowth.dto.request.AiGrowthCoachConfigUpdateRequest;
import com.xiaou.aigrowth.dto.request.AiGrowthCoachReplanRequest;
import com.xiaou.aigrowth.dto.request.AiGrowthCoachRefreshRequest;
import com.xiaou.aigrowth.dto.response.AiGrowthCoachChatResponse;
import com.xiaou.aigrowth.dto.response.AiGrowthCoachConfigResponse;
import com.xiaou.aigrowth.dto.response.AiGrowthCoachFailureResponse;
import com.xiaou.aigrowth.dto.response.AiGrowthCoachReplanResponse;
import com.xiaou.aigrowth.dto.response.AiGrowthCoachSnapshotDetailResponse;
import com.xiaou.aigrowth.dto.response.AiGrowthCoachStatisticsResponse;

import java.util.List;

/**
 * AI成长教练服务
 */
public interface AiGrowthCoachService {

    AiGrowthCoachSnapshotDetailResponse getSummary(Long userId, String scene);

    AiGrowthCoachSnapshotDetailResponse getSnapshotDetail(Long userId, Long snapshotId);

    AiGrowthCoachSnapshotDetailResponse refresh(Long userId, AiGrowthCoachRefreshRequest request);

    AiGrowthCoachChatResponse chat(Long userId, AiGrowthCoachChatRequest request);

    AiGrowthCoachReplanResponse replan(Long userId, AiGrowthCoachReplanRequest request);

    AiGrowthCoachChatResponse getChatSession(Long userId, Long sessionId);

    void updateActionStatus(Long userId, Long actionId, String status);

    AiGrowthCoachStatisticsResponse getStatistics();

    List<AiGrowthCoachFailureResponse> getFailures();

    List<AiGrowthCoachConfigResponse> getConfigs();

    void updateConfigs(List<AiGrowthCoachConfigUpdateRequest> requests);
}
