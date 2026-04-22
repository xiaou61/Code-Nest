package com.xiaou.ai.structured;

import java.util.Set;
import java.util.function.Consumer;

/**
 * 结构化输出对象契约。
 *
 * @author xiaou
 */
public interface AiStructuredObjectContract {

    AiStructuredObjectContract requireString(String key);

    AiStructuredObjectContract requireStringInSet(String key, Set<String> allowedValues);

    AiStructuredObjectContract requireInt(String key);

    AiStructuredObjectContract requireIntRange(String key, int min, int max);

    AiStructuredObjectContract requirePositiveInt(String key);

    AiStructuredObjectContract requireStringArray(String key);

    AiStructuredObjectContract requireStringOrStringArray(String key);

    AiStructuredObjectContract requireObject(String key, Consumer<AiStructuredObjectContract> nestedValidator);

    AiStructuredObjectContract requireObjectArray(String key, Consumer<AiStructuredObjectContract> itemValidator);
}
