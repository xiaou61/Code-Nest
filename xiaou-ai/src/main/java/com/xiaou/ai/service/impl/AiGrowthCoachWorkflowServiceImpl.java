package com.xiaou.ai.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.xiaou.ai.dto.growthcoach.GrowthCoachChatRequest;
import com.xiaou.ai.dto.growthcoach.GrowthCoachChatResult;
import com.xiaou.ai.dto.growthcoach.GrowthCoachActionReplanRequest;
import com.xiaou.ai.dto.growthcoach.GrowthCoachActionReplanResult;
import com.xiaou.ai.dto.growthcoach.GrowthCoachSnapshotAiRequest;
import com.xiaou.ai.dto.growthcoach.GrowthCoachSnapshotAiResult;
import com.xiaou.ai.service.AiGrowthCoachWorkflowService;
import com.xiaou.ai.util.CozeResponseParser;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.enums.CozeWorkflowEnum;
import com.xiaou.common.utils.CozeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI成长教练工作流服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiGrowthCoachWorkflowServiceImpl implements AiGrowthCoachWorkflowService {

    private final CozeUtils cozeUtils;

    @Override
    public GrowthCoachSnapshotAiResult generateSnapshot(GrowthCoachSnapshotAiRequest request) {
        try {
            if (!cozeUtils.isClientAvailable() || request == null) {
                return null;
            }
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("scene", request.getScene());
            params.put("headline", request.getHeadline());
            params.put("summaryJson", request.getSummaryJson());
            params.put("sourceDigestJson", request.getSourceDigestJson());
            params.put("actionJson", request.getActionJson());
            Result<String> result = cozeUtils.runWorkflow(CozeWorkflowEnum.GROWTH_COACH_SNAPSHOT, params);
            if (!result.isSuccess() || CozeResponseParser.isErrorResponse(result.getData())) {
                return null;
            }
            JSONObject json = CozeResponseParser.parse(result.getData());
            if (json == null) {
                return null;
            }
            return new GrowthCoachSnapshotAiResult()
                    .setHeadline(CozeResponseParser.getString(json, "headline", null))
                    .setSummary(CozeResponseParser.getString(json, "summary", null))
                    .setRiskLevel(CozeResponseParser.getString(json, "riskLevel", null))
                    .setRiskFlags(parseStringList(json, "riskFlags"))
                    .setFocusAreas(parseStringList(json, "focusAreas"))
                    .setRecommendedQuestions(parseStringList(json, "recommendedQuestions"));
        } catch (Exception ex) {
            log.warn("AI成长教练快照工作流调用失败", ex);
            return null;
        }
    }

    @Override
    public GrowthCoachActionReplanResult replan(GrowthCoachActionReplanRequest request) {
        try {
            if (!cozeUtils.isClientAvailable() || request == null || request.getAvailableMinutes() == null) {
                return null;
            }
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("scene", request.getScene());
            params.put("availableMinutes", request.getAvailableMinutes());
            params.put("headline", request.getHeadline());
            params.put("summaryJson", request.getSummaryJson());
            params.put("sourceDigestJson", request.getSourceDigestJson());
            params.put("selectedActionJson", request.getSelectedActionJson());
            params.put("deferredActionJson", request.getDeferredActionJson());
            Result<String> result = cozeUtils.runWorkflow(CozeWorkflowEnum.GROWTH_COACH_ACTION_REPLAN, params);
            if (!result.isSuccess() || CozeResponseParser.isErrorResponse(result.getData())) {
                return null;
            }
            JSONObject json = CozeResponseParser.parse(result.getData());
            if (json == null) {
                return null;
            }
            return new GrowthCoachActionReplanResult()
                    .setSummary(CozeResponseParser.getString(json, "summary", null))
                    .setSuggestedQuestions(parseStringList(json, "suggestedQuestions"));
        } catch (Exception ex) {
            log.warn("AI成长教练行动重排工作流调用失败", ex);
            return null;
        }
    }

    @Override
    public GrowthCoachChatResult chat(GrowthCoachChatRequest request) {
        try {
            if (!cozeUtils.isClientAvailable() || request == null || StrUtil.isBlank(request.getMessage())) {
                return null;
            }
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("scene", request.getScene());
            params.put("message", request.getMessage());
            params.put("headline", request.getHeadline());
            params.put("summaryJson", request.getSummaryJson());
            params.put("sourceDigestJson", request.getSourceDigestJson());
            Result<String> result = cozeUtils.runWorkflow(CozeWorkflowEnum.GROWTH_COACH_CHAT, params);
            if (!result.isSuccess() || CozeResponseParser.isErrorResponse(result.getData())) {
                return null;
            }
            JSONObject json = CozeResponseParser.parse(result.getData());
            if (json == null) {
                return null;
            }
            return new GrowthCoachChatResult()
                    .setReply(CozeResponseParser.getString(json, "reply", null))
                    .setSuggestedQuestions(parseStringList(json, "suggestedQuestions"))
                    .setReferences(parseStringList(json, "references"));
        } catch (Exception ex) {
            log.warn("AI成长教练对话工作流调用失败", ex);
            return null;
        }
    }

    private List<String> parseStringList(JSONObject json, String key) {
        if (json == null) {
            return List.of();
        }
        List<String> values = json.getBeanList(key, String.class);
        return values == null ? List.of() : values;
    }
}
