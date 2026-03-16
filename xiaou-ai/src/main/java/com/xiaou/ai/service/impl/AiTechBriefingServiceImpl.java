package com.xiaou.ai.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.xiaou.ai.dto.techbriefing.TechBriefingSummaryRequest;
import com.xiaou.ai.dto.techbriefing.TechBriefingSummaryResult;
import com.xiaou.ai.dto.techbriefing.TechBriefingTranslateRequest;
import com.xiaou.ai.dto.techbriefing.TechBriefingTranslateResult;
import com.xiaou.ai.service.AiTechBriefingService;
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
 * 科技热点 AI 服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiTechBriefingServiceImpl implements AiTechBriefingService {

    private final CozeUtils cozeUtils;

    @Override
    public TechBriefingTranslateResult translate(TechBriefingTranslateRequest request) {
        try {
            if (!cozeUtils.isClientAvailable() || request == null || StrUtil.isBlank(request.getTitle())) {
                return null;
            }
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("title", request.getTitle());
            params.put("summary", request.getSummary());
            params.put("content", request.getContent());
            params.put("sourceName", request.getSourceName());
            Result<String> result = cozeUtils.runWorkflow(CozeWorkflowEnum.TECH_BRIEFING_TRANSLATE, params);
            if (!result.isSuccess() || CozeResponseParser.isErrorResponse(result.getData())) {
                return null;
            }
            JSONObject json = CozeResponseParser.parse(result.getData());
            if (json == null) {
                return null;
            }
            return new TechBriefingTranslateResult()
                    .setTitleZh(CozeResponseParser.getString(json, "titleZh", null))
                    .setSummaryZh(CozeResponseParser.getString(json, "summaryZh", null))
                    .setContentZh(CozeResponseParser.getString(json, "contentZh", null))
                    .setModelName(CozeResponseParser.getString(json, "modelName", "coze"));
        } catch (Exception ex) {
            log.warn("科技热点翻译工作流调用失败", ex);
            return null;
        }
    }

    @Override
    public TechBriefingSummaryResult summarize(TechBriefingSummaryRequest request) {
        try {
            if (!cozeUtils.isClientAvailable() || request == null || StrUtil.isBlank(request.getTitleZh())) {
                return null;
            }
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("title", request.getTitle());
            params.put("titleZh", request.getTitleZh());
            params.put("summary", request.getSummary());
            params.put("summaryZh", request.getSummaryZh());
            params.put("contentZh", request.getContentZh());
            params.put("sourceName", request.getSourceName());
            Result<String> result = cozeUtils.runWorkflow(CozeWorkflowEnum.TECH_BRIEFING_SUMMARY, params);
            if (!result.isSuccess() || CozeResponseParser.isErrorResponse(result.getData())) {
                return null;
            }
            JSONObject json = CozeResponseParser.parse(result.getData());
            if (json == null) {
                return null;
            }
            return new TechBriefingSummaryResult()
                    .setSummary(CozeResponseParser.getString(json, "summary", null))
                    .setWhyImportant(CozeResponseParser.getString(json, "whyImportant", null))
                    .setImpactScope(CozeResponseParser.getString(json, "impactScope", null))
                    .setKeywords(parseStringList(json, "keywords"))
                    .setModelName(CozeResponseParser.getString(json, "modelName", "coze"));
        } catch (Exception ex) {
            log.warn("科技热点摘要工作流调用失败", ex);
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
