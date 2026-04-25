package com.xiaou.ai.structured;

import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 结构化输出 JSON Schema 导出器。
 *
 * @author xiaou
 */
public final class AiStructuredOutputSchemaExporter {

    private AiStructuredOutputSchemaExporter() {
    }

    public static Map<String, JSONObject> exportAll() {
        Map<String, JSONObject> schemas = new LinkedHashMap<>();
        for (AiStructuredOutputSpec spec : AiStructuredOutputCatalog.all()) {
            schemas.put(spec.specId(), spec.jsonSchema());
        }
        return Map.copyOf(schemas);
    }

    public static void exportToDirectory(Path outputDirectory) throws IOException {
        Files.createDirectories(outputDirectory);
        for (AiStructuredOutputSpec spec : AiStructuredOutputCatalog.all()) {
            Path file = outputDirectory.resolve(spec.schemaFileName());
            Files.writeString(file, JSONUtil.toJsonPrettyStr(spec.jsonSchema()), StandardCharsets.UTF_8);
        }
    }
}
