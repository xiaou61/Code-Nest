package com.xiaou.system.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LlamaIndex 检索命中节点。
 *
 * @author xiaou
 */
@Data
public class AiRagDebugNodeResponse {

    /**
     * 节点 ID。
     */
    private String id;

    /**
     * 相似度分数。
     */
    private Double score;

    /**
     * 文本内容。
     */
    private String text;

    /**
     * 元数据。
     */
    private Map<String, Object> metadata = new LinkedHashMap<>();

    /**
     * 命中的关键词。
     */
    private List<String> matchedTerms = new ArrayList<>();

    /**
     * 分数拆解。
     */
    private Map<String, Double> scoreBreakdown = new LinkedHashMap<>();

    /**
     * 最主要的命中字段。
     */
    private String bestMatchField;

    /**
     * 最有代表性的命中片段。
     */
    private String bestSnippet;
}
