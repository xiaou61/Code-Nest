package com.xiaou.ai.structured;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.xiaou.ai.prompt.AiPromptSpec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 结构化输出 JSON Schema 构造器。
 *
 * @author xiaou
 */
final class AiStructuredJsonSchemaBuilder {

    private static final String JSON_SCHEMA_DRAFT = "https://json-schema.org/draft/2020-12/schema";

    private AiStructuredJsonSchemaBuilder() {
    }

    static JSONObject buildObjectSchema(AiPromptSpec promptSpec, Consumer<AiStructuredObjectContract> contract) {
        JsonSchemaObjectContract objectContract = new JsonSchemaObjectContract();
        contract.accept(objectContract);
        return wrapSchemaMetadata(promptSpec, objectContract.toSchema(), "object");
    }

    static JSONObject buildArraySchema(AiPromptSpec promptSpec, Consumer<AiStructuredArrayContract> contract) {
        JsonSchemaArrayContract arrayContract = new JsonSchemaArrayContract();
        contract.accept(arrayContract);
        return wrapSchemaMetadata(promptSpec, arrayContract.toSchema(), "array");
    }

    private static JSONObject wrapSchemaMetadata(AiPromptSpec promptSpec, Map<String, Object> rootSchema, String rootType) {
        JSONObject schema = new JSONObject(new LinkedHashMap<>());
        schema.set("$schema", JSON_SCHEMA_DRAFT);
        schema.set("$id", "xiaou://ai/structured-output/" + promptSpec.promptId());
        schema.set("title", promptSpec.key());
        schema.set("description", "Structured output schema for " + promptSpec.promptId());
        schema.set("x-prompt-key", promptSpec.key());
        schema.set("x-prompt-version", promptSpec.version());
        schema.set("x-root-type", rootType);
        rootSchema.forEach(schema::set);
        return schema;
    }

    private static final class JsonSchemaObjectContract implements AiStructuredObjectContract {

        private final Map<String, Object> properties = new LinkedHashMap<>();
        private final List<String> required = new ArrayList<>();

        @Override
        public AiStructuredObjectContract requireString(String key) {
            addProperty(key, Map.of("type", "string"));
            return this;
        }

        @Override
        public AiStructuredObjectContract requireStringInSet(String key, Set<String> allowedValues) {
            Map<String, Object> schema = new LinkedHashMap<>();
            schema.put("type", "string");
            schema.put("enum", new ArrayList<>(allowedValues));
            addProperty(key, schema);
            return this;
        }

        @Override
        public AiStructuredObjectContract requireInt(String key) {
            addProperty(key, Map.of("type", "integer"));
            return this;
        }

        @Override
        public AiStructuredObjectContract requireIntRange(String key, int min, int max) {
            Map<String, Object> schema = new LinkedHashMap<>();
            schema.put("type", "integer");
            schema.put("minimum", min);
            schema.put("maximum", max);
            addProperty(key, schema);
            return this;
        }

        @Override
        public AiStructuredObjectContract requirePositiveInt(String key) {
            Map<String, Object> schema = new LinkedHashMap<>();
            schema.put("type", "integer");
            schema.put("minimum", 1);
            addProperty(key, schema);
            return this;
        }

        @Override
        public AiStructuredObjectContract requireStringArray(String key) {
            Map<String, Object> schema = new LinkedHashMap<>();
            schema.put("type", "array");
            schema.put("items", Map.of("type", "string"));
            addProperty(key, schema);
            return this;
        }

        @Override
        public AiStructuredObjectContract requireStringOrStringArray(String key) {
            Map<String, Object> schema = new LinkedHashMap<>();
            schema.put("oneOf", List.of(
                    Map.of("type", "string"),
                    Map.of(
                            "type", "array",
                            "items", Map.of("type", "string")
                    )
            ));
            addProperty(key, schema);
            return this;
        }

        @Override
        public AiStructuredObjectContract requireObject(String key, Consumer<AiStructuredObjectContract> nestedValidator) {
            JsonSchemaObjectContract nested = new JsonSchemaObjectContract();
            nestedValidator.accept(nested);
            addProperty(key, nested.toSchema());
            return this;
        }

        @Override
        public AiStructuredObjectContract requireObjectArray(String key, Consumer<AiStructuredObjectContract> itemValidator) {
            JsonSchemaObjectContract nested = new JsonSchemaObjectContract();
            itemValidator.accept(nested);
            Map<String, Object> schema = new LinkedHashMap<>();
            schema.put("type", "array");
            schema.put("items", nested.toSchema());
            addProperty(key, schema);
            return this;
        }

        private void addProperty(String key, Map<String, Object> schema) {
            properties.put(key, schema);
            if (!required.contains(key)) {
                required.add(key);
            }
        }

        private Map<String, Object> toSchema() {
            Map<String, Object> schema = new LinkedHashMap<>();
            schema.put("type", "object");
            schema.put("additionalProperties", false);
            schema.put("properties", properties);
            schema.put("required", required);
            return schema;
        }
    }

    private static final class JsonSchemaArrayContract implements AiStructuredArrayContract {

        private Object itemsSchema = Map.of();

        @Override
        public AiStructuredArrayContract requireObjectItems(Consumer<AiStructuredObjectContract> itemValidator) {
            JsonSchemaObjectContract nested = new JsonSchemaObjectContract();
            itemValidator.accept(nested);
            this.itemsSchema = nested.toSchema();
            return this;
        }

        @Override
        public AiStructuredArrayContract requireStringItems() {
            this.itemsSchema = Map.of("type", "string");
            return this;
        }

        private Map<String, Object> toSchema() {
            Map<String, Object> schema = new LinkedHashMap<>();
            schema.put("type", "array");
            schema.put("items", itemsSchema);
            return schema;
        }
    }
}
