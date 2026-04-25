package com.xiaou.ai.service.impl;

import cn.hutool.json.JSONObject;
import com.xiaou.ai.dto.community.PostSummaryResult;
import com.xiaou.ai.metrics.AiMetricsRecorder;
import com.xiaou.ai.prompt.community.CommunityPromptSpecs;
import com.xiaou.ai.service.AiCommunityService;
import com.xiaou.ai.structured.community.CommunityStructuredOutputSpecs;
import com.xiaou.ai.support.AiExecutionSupport;
import com.xiaou.ai.util.AiJsonResponseParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 社区AI服务实现
 *
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiCommunityServiceImpl implements AiCommunityService {
    private final AiExecutionSupport aiExecutionSupport;
    private final AiMetricsRecorder aiMetricsRecorder;

    @Override
    public PostSummaryResult generatePostSummary(String title, String content) {
        return aiExecutionSupport.chatWithFallback(
                "community_summary",
                CommunityPromptSpecs.POST_SUMMARY,
                buildPromptVariables(title, content),
                this::parseSummaryResult,
                () -> PostSummaryResult.fallbackResult(title)
        );
    }

    /**
     * 解析关键词字段
     */
    private List<String> parseKeywords(Object keywordsObj) {
        if (keywordsObj == null) {
            return List.of();
        }

        List<String> keywords = new ArrayList<>();
        if (keywordsObj instanceof String) {
            // 字符串格式，按逗号分割
            String keywordsStr = ((String) keywordsObj).trim();
            if (!keywordsStr.isEmpty()) {
                for (String kw : keywordsStr.split(",\\s*")) {
                    keywords.add(kw.trim());
                }
            }
        } else if (keywordsObj instanceof List) {
            // 数组格式
            for (Object item : (List<?>) keywordsObj) {
                if (item != null) {
                    keywords.add(item.toString());
                }
            }
        }
        return keywords;
    }

    private Map<String, Object> buildPromptVariables(String title, String content) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("title", title == null ? "" : title.trim());
        variables.put("content", content == null ? "" : content.trim());
        return variables;
    }

    private PostSummaryResult parseSummaryResult(String response) {
        JSONObject json = AiJsonResponseParser.parse(response);
        if (json == null || AiJsonResponseParser.isErrorResponse(response)) {
            aiMetricsRecorder.recordStructuredParseFailure("community_summary", CommunityPromptSpecs.POST_SUMMARY, "invalid_json");
            return PostSummaryResult.fallbackResult(null);
        }

        var validation = CommunityStructuredOutputSpecs.POST_SUMMARY.validateObject(json);
        if (!validation.valid()) {
            aiMetricsRecorder.recordStructuredParseFailure("community_summary", CommunityPromptSpecs.POST_SUMMARY, validation.reason());
            return PostSummaryResult.fallbackResult(null);
        }

        String summary = AiJsonResponseParser.getString(json, "summary", "暂无摘要");
        List<String> keywords = parseKeywords(json.get("keywords"));

        return new PostSummaryResult()
                .setSummary(summary)
                .setKeywords(keywords)
                .setFallback(false);
    }
}
