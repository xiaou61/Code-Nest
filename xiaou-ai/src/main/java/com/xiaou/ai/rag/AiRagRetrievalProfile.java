package com.xiaou.ai.rag;

import com.xiaou.ai.prompt.AiRagQuerySpec;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LlamaIndex 检索配置画像。
 *
 * <p>统一管理检索 scene、topK 和 metadata filters，避免图编排层分散维护。</p>
 *
 * @author xiaou
 */
public record AiRagRetrievalProfile(AiRagQuerySpec querySpec,
                                    String scene,
                                    int topK,
                                    Map<String, Object> metadataFilters) {

    public AiRagRetrievalProfile {
        Assert.notNull(querySpec, "querySpec 不能为空");
        Assert.hasText(scene, "scene 不能为空");
        Assert.isTrue(topK > 0, "topK 必须大于 0");

        scene = scene.trim();
        metadataFilters = metadataFilters == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(metadataFilters));
    }

    public static AiRagRetrievalProfile of(AiRagQuerySpec querySpec,
                                           String scene,
                                           int topK,
                                           Map<String, Object> metadataFilters) {
        return new AiRagRetrievalProfile(querySpec, scene, topK, metadataFilters);
    }

    public String profileId() {
        return querySpec.queryId();
    }

    public LlamaIndexRetrieveRequest buildRequest(String query) {
        Assert.hasText(query, "query 不能为空");
        LlamaIndexRetrieveRequest request = new LlamaIndexRetrieveRequest()
                .setScene(scene)
                .setQuery(query.trim())
                .setTopK(topK)
                .setMetadataFilters(new LinkedHashMap<>(metadataFilters));

        if (!StringUtils.hasText(query)) {
            throw new IllegalArgumentException("query 不能为空");
        }
        return request;
    }
}
