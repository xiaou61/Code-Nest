package com.xiaou.ai.structured;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.xiaou.ai.prompt.AiPromptSpec;
import org.springframework.util.Assert;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * AI 结构化输出契约定义。
 *
 * <p>将 Prompt 与输出 schema 显式绑定，避免 Prompt 文案、解析逻辑和校验规则长期分叉。</p>
 *
 * @author xiaou
 */
public final class AiStructuredOutputSpec {

    public enum RootType {
        OBJECT,
        ARRAY
    }

    private final AiPromptSpec promptSpec;
    private final RootType rootType;
    private final Consumer<AiStructuredObjectContract> objectContract;
    private final Consumer<AiStructuredArrayContract> arrayContract;

    private AiStructuredOutputSpec(AiPromptSpec promptSpec,
                                   RootType rootType,
                                   Consumer<AiStructuredObjectContract> objectContract,
                                   Consumer<AiStructuredArrayContract> arrayContract) {
        this.promptSpec = Objects.requireNonNull(promptSpec, "promptSpec 不能为空");
        this.rootType = Objects.requireNonNull(rootType, "rootType 不能为空");
        this.objectContract = objectContract;
        this.arrayContract = arrayContract;
    }

    public static AiStructuredOutputSpec object(AiPromptSpec promptSpec,
                                                Consumer<AiStructuredObjectContract> contract) {
        Assert.notNull(contract, "object contract 不能为空");
        return new AiStructuredOutputSpec(promptSpec, RootType.OBJECT, contract, null);
    }

    public static AiStructuredOutputSpec array(AiPromptSpec promptSpec,
                                               Consumer<AiStructuredArrayContract> contract) {
        Assert.notNull(contract, "array contract 不能为空");
        return new AiStructuredOutputSpec(promptSpec, RootType.ARRAY, null, contract);
    }

    public AiPromptSpec promptSpec() {
        return promptSpec;
    }

    public RootType rootType() {
        return rootType;
    }

    public String specId() {
        return promptSpec.promptId();
    }

    public String schemaId() {
        return "xiaou://ai/structured-output/" + promptSpec.promptId();
    }

    public String schemaFileName() {
        return promptSpec.key().replace('.', '-') + "-" + promptSpec.version() + ".schema.json";
    }

    public AiStructuredOutputValidator.ValidationResult validateObject(JSONObject json) {
        if (rootType != RootType.OBJECT) {
            return AiStructuredOutputValidator.ValidationResult.failure("root_type_mismatch");
        }
        AiStructuredOutputValidator.ObjectValidator validator = AiStructuredOutputValidator.object(json);
        objectContract.accept(AiStructuredValidationContracts.object(validator));
        return validator.validate();
    }

    public AiStructuredOutputValidator.ValidationResult validateArray(JSONArray array) {
        if (rootType != RootType.ARRAY) {
            return AiStructuredOutputValidator.ValidationResult.failure("root_type_mismatch");
        }
        AiStructuredOutputValidator.ArrayValidator validator = AiStructuredOutputValidator.array(array);
        arrayContract.accept(AiStructuredValidationContracts.array(validator));
        return validator.validate();
    }

    public JSONObject jsonSchema() {
        return switch (rootType) {
            case OBJECT -> AiStructuredJsonSchemaBuilder.buildObjectSchema(promptSpec, objectContract);
            case ARRAY -> AiStructuredJsonSchemaBuilder.buildArraySchema(promptSpec, arrayContract);
        };
    }
}
