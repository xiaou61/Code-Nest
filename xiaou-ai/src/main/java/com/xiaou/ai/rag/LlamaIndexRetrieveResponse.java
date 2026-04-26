package com.xiaou.ai.rag;

import cn.hutool.json.JSONUtil;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LlamaIndex 检索响应。
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class LlamaIndexRetrieveResponse {

    /**
     * 原始查询
     */
    private String query;

    /**
     * 命中文档片段
     */
    private List<Node> nodes = new ArrayList<>();

    /**
     * 是否为降级结果
     */
    private boolean fallback;

    public String toContextSnippet() {
        if (nodes == null || nodes.isEmpty()) {
            return "";
        }

        List<String> blocks = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            if (node == null || !StringUtils.hasText(node.getText())) {
                continue;
            }

            StringBuilder builder = new StringBuilder();
            builder.append("<knowledge_item>\n");
            builder.append("index=").append(i + 1);
            if (StringUtils.hasText(node.getId())) {
                builder.append("\nid=").append(node.getId().trim());
            }
            if (node.getScore() != null) {
                builder.append("\nscore=").append(node.getScore());
            }
            if (StringUtils.hasText(node.getBestMatchField())) {
                builder.append("\nbest_match_field=").append(node.getBestMatchField().trim());
            }
            if (node.getMatchedTerms() != null && !node.getMatchedTerms().isEmpty()) {
                builder.append("\nmatched_terms=").append(String.join(", ", node.getMatchedTerms()));
            }
            if (node.getMetadata() != null && !node.getMetadata().isEmpty()) {
                builder.append("\nmetadata=").append(JSONUtil.toJsonStr(node.getMetadata()));
            }
            builder.append("\ncontent:\n").append(node.getText().trim());
            builder.append("\n</knowledge_item>");
            blocks.add(builder.toString());
        }
        return String.join("\n\n", blocks);
    }

    @Data
    @Accessors(chain = true)
    public static class Node {

        /**
         * 片段ID
         */
        private String id;

        /**
         * 相似度分数
         */
        private Double score;

        /**
         * 片段文本
         */
        private String text;

        /**
         * 元数据
         */
        private Map<String, Object> metadata = new LinkedHashMap<>();

        /**
         * 命中的关键词。
         */
        private List<String> matchedTerms = new ArrayList<>();

        /**
         * 打分拆解。
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
}
