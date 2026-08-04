package com.xiaou.ai.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xiaou.ai.dto.codereview.CodePenReviewResult;
import com.xiaou.ai.prompt.codereview.CodeReviewPromptSpecs;
import com.xiaou.ai.service.AiCodeReviewService;
import com.xiaou.ai.structured.AiStructuredOutputValidator;
import com.xiaou.ai.structured.codereview.CodeReviewStructuredOutputSpecs;
import com.xiaou.ai.support.AiExecutionSupport;
import com.xiaou.ai.util.AiJsonResponseParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 通过统一 AI 运行时生成 CodePen 的结构化审查结果。
 */
@Service
@RequiredArgsConstructor
public class AiCodeReviewServiceImpl implements AiCodeReviewService {

    private static final String SCENE_NAME = "code_review.codepen_review";
    private static final int MAX_FINDINGS = 5;
    private static final int MAX_ACTIONS = 3;

    private final AiExecutionSupport aiExecutionSupport;

    @Override
    public CodePenReviewResult reviewCodePen(String title, String htmlCode, String cssCode, String javascriptCode) {
        return aiExecutionSupport.chatWithFallback(
                SCENE_NAME,
                CodeReviewPromptSpecs.CODEPEN_REVIEW,
                promptVariables(title, htmlCode, cssCode, javascriptCode),
                this::parse,
                CodePenReviewResult::unavailable
        );
    }

    private Map<String, Object> promptVariables(String title, String htmlCode, String cssCode, String javascriptCode) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("title", promptData(title));
        variables.put("htmlCode", promptData(htmlCode));
        variables.put("cssCode", promptData(cssCode));
        variables.put("jsCode", promptData(javascriptCode));
        return variables;
    }

    private CodePenReviewResult parse(String response) {
        JSONObject json = AiJsonResponseParser.parse(response);
        if (json == null) {
            throw new IllegalArgumentException("code review output invalid");
        }
        AiStructuredOutputValidator.ValidationResult validation =
                CodeReviewStructuredOutputSpecs.CODEPEN_REVIEW.validateObject(json);
        if (!validation.valid()) {
            throw new IllegalArgumentException("code review output invalid");
        }

        CodePenReviewResult result = new CodePenReviewResult();
        result.setScore(json.getInt("score", 0));
        result.setSummary(cleanText(json.getStr("summary"), 160));
        result.setFindings(parseFindings(json.getJSONArray("findings")));
        result.setActionItems(parseActions(json.getJSONArray("actionItems")));
        result.setFallback(false);
        return result;
    }

    private List<CodePenReviewResult.Finding> parseFindings(JSONArray values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<CodePenReviewResult.Finding> result = new ArrayList<>();
        for (Object value : values) {
            if (result.size() >= MAX_FINDINGS || value == null) {
                break;
            }
            JSONObject item = JSONUtil.parseObj(value);
            CodePenReviewResult.Finding finding = new CodePenReviewResult.Finding();
            finding.setSeverity(normalizeSeverity(item.getStr("severity")));
            finding.setArea(normalizeArea(item.getStr("area")));
            finding.setTitle(cleanText(item.getStr("title"), 80));
            finding.setDescription(cleanText(item.getStr("description"), 240));
            finding.setRecommendedAction(cleanText(item.getStr("recommendedAction"), 220));
            result.add(finding);
        }
        return result;
    }

    private List<CodePenReviewResult.ActionItem> parseActions(JSONArray values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<CodePenReviewResult.ActionItem> result = new ArrayList<>();
        for (Object value : values) {
            if (result.size() >= MAX_ACTIONS || value == null) {
                break;
            }
            JSONObject item = JSONUtil.parseObj(value);
            CodePenReviewResult.ActionItem action = new CodePenReviewResult.ActionItem();
            action.setTitle(cleanText(item.getStr("title"), 80));
            action.setDescription(cleanText(item.getStr("description"), 220));
            action.setVerification(cleanText(item.getStr("verification"), 180));
            result.add(action);
        }
        return result;
    }

    private String normalizeSeverity(String value) {
        String normalized = cleanText(value, 16).toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "CRITICAL", "HIGH", "MEDIUM", "LOW" -> normalized;
            default -> "LOW";
        };
    }

    private String normalizeArea(String value) {
        String normalized = cleanText(value, 16).toUpperCase(Locale.ROOT);
        if ("JS".equals(normalized) || "JAVASCRIPT".equals(normalized)) {
            return "JAVASCRIPT";
        }
        return switch (normalized) {
            case "HTML", "CSS", "CROSS" -> normalized;
            default -> "CROSS";
        };
    }

    private String cleanText(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value
                .replace("```", "")
                .replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "")
                .replaceAll("\\s+", " ")
                .trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private String promptData(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
