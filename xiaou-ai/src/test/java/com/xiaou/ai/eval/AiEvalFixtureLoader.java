package com.xiaou.ai.eval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * AI 回归评测样例加载器。
 *
 * @author xiaou
 */
public final class AiEvalFixtureLoader {

    private static final String FIXTURE_PATH = "ai-evals/scene-regression-cases.json";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private AiEvalFixtureLoader() {
    }

    public static List<AiEvalCase> loadSceneRegressionCases() {
        try {
            ClassPathResource resource = new ClassPathResource(FIXTURE_PATH);
            try (InputStream inputStream = resource.getInputStream()) {
                return OBJECT_MAPPER.readValue(inputStream, new TypeReference<List<AiEvalCase>>() {
                });
            }
        } catch (IOException e) {
            throw new IllegalStateException("加载 AI 回归评测样例失败: " + FIXTURE_PATH, e);
        }
    }
}
