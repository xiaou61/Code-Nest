package com.xiaou.ai.structured;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

import java.util.Set;
import java.util.function.Consumer;

/**
 * 统一结构化输出校验器。
 *
 * <p>用于在 JSON 解析成功后继续校验字段结构、类型与取值范围，
 * 避免“返回了 JSON 但 schema 不符合约定”时继续向后传播错误数据。</p>
 *
 * @author xiaou
 */
public final class AiStructuredOutputValidator {

    private AiStructuredOutputValidator() {
    }

    public static ObjectValidator object(JSONObject json) {
        return new ObjectValidator(json);
    }

    public static ArrayValidator array(JSONArray array) {
        return new ArrayValidator(array);
    }

    public record ValidationResult(boolean valid, String reason) {

        public static ValidationResult success() {
            return new ValidationResult(true, "");
        }

        public static ValidationResult failure(String reason) {
            return new ValidationResult(false, reason);
        }
    }

    public static final class ObjectValidator {

        private final JSONObject json;
        private String failureReason;

        private ObjectValidator(JSONObject json) {
            this.json = json;
            if (json == null) {
                this.failureReason = "root_object_missing";
            }
        }

        public ObjectValidator requireString(String key) {
            return require(key, value -> value instanceof CharSequence, key + "_missing_or_not_string");
        }

        public ObjectValidator requireStringInSet(String key, Set<String> allowedValues) {
            if (failureReason != null) {
                return this;
            }
            Object value = getValue(key);
            if (!(value instanceof CharSequence sequence)) {
                failureReason = key + "_missing_or_not_string";
                return this;
            }
            String text = sequence.toString().trim();
            if (!allowedValues.contains(text)) {
                failureReason = key + "_not_in_allowed_set";
            }
            return this;
        }

        public ObjectValidator requireInt(String key) {
            if (failureReason != null) {
                return this;
            }
            Integer value = json == null ? null : json.getInt(key, null);
            if (value == null) {
                failureReason = key + "_missing_or_not_integer";
            }
            return this;
        }

        public ObjectValidator requireIntRange(String key, int min, int max) {
            if (failureReason != null) {
                return this;
            }
            Integer value = json == null ? null : json.getInt(key, null);
            if (value == null) {
                failureReason = key + "_missing_or_not_integer";
                return this;
            }
            if (value < min || value > max) {
                failureReason = key + "_out_of_range";
            }
            return this;
        }

        public ObjectValidator requirePositiveInt(String key) {
            if (failureReason != null) {
                return this;
            }
            Integer value = json == null ? null : json.getInt(key, null);
            if (value == null) {
                failureReason = key + "_missing_or_not_integer";
                return this;
            }
            if (value <= 0) {
                failureReason = key + "_not_positive";
            }
            return this;
        }

        public ObjectValidator requireStringArray(String key) {
            if (failureReason != null) {
                return this;
            }
            JSONArray array = json == null ? null : json.getJSONArray(key);
            if (array == null) {
                failureReason = key + "_missing_or_not_array";
                return this;
            }
            for (int i = 0; i < array.size(); i++) {
                Object item = array.get(i);
                if (!(item instanceof CharSequence)) {
                    failureReason = key + "_item_not_string";
                    return this;
                }
            }
            return this;
        }

        public ObjectValidator requireStringOrStringArray(String key) {
            if (failureReason != null) {
                return this;
            }
            Object value = getValue(key);
            if (value instanceof CharSequence) {
                return this;
            }
            JSONArray array = json == null ? null : json.getJSONArray(key);
            if (array == null) {
                failureReason = key + "_missing_or_not_string_or_array";
                return this;
            }
            for (int i = 0; i < array.size(); i++) {
                Object item = array.get(i);
                if (!(item instanceof CharSequence)) {
                    failureReason = key + "_item_not_string";
                    return this;
                }
            }
            return this;
        }

        public ObjectValidator requireObject(String key, Consumer<ObjectValidator> nestedValidator) {
            if (failureReason != null) {
                return this;
            }
            JSONObject object = json == null ? null : json.getJSONObject(key);
            if (object == null) {
                failureReason = key + "_missing_or_not_object";
                return this;
            }
            ObjectValidator validator = new ObjectValidator(object);
            nestedValidator.accept(validator);
            ValidationResult result = validator.validate();
            if (!result.valid()) {
                failureReason = key + "." + result.reason();
            }
            return this;
        }

        public ObjectValidator requireObjectArray(String key, Consumer<ObjectValidator> itemValidator) {
            if (failureReason != null) {
                return this;
            }
            JSONArray array = json == null ? null : json.getJSONArray(key);
            if (array == null) {
                failureReason = key + "_missing_or_not_array";
                return this;
            }
            ArrayValidator validator = new ArrayValidator(array).requireObjectItems(itemValidator);
            ValidationResult result = validator.validate();
            if (!result.valid()) {
                failureReason = key + "." + result.reason();
            }
            return this;
        }

        public ValidationResult validate() {
            return failureReason == null ? ValidationResult.success() : ValidationResult.failure(failureReason);
        }

        private ObjectValidator require(String key, java.util.function.Predicate<Object> predicate, String reason) {
            if (failureReason != null) {
                return this;
            }
            Object value = getValue(key);
            if (!predicate.test(value)) {
                failureReason = reason;
            }
            return this;
        }

        private Object getValue(String key) {
            if (json == null || !json.containsKey(key)) {
                return null;
            }
            return json.get(key);
        }
    }

    public static final class ArrayValidator {

        private final JSONArray array;
        private String failureReason;

        private ArrayValidator(JSONArray array) {
            this.array = array;
            if (array == null) {
                this.failureReason = "root_array_missing";
            }
        }

        public ArrayValidator requireObjectItems(Consumer<ObjectValidator> itemValidator) {
            if (failureReason != null) {
                return this;
            }
            for (int i = 0; i < array.size(); i++) {
                JSONObject item = array.getJSONObject(i);
                if (item == null) {
                    failureReason = "item_not_object";
                    return this;
                }
                ObjectValidator validator = new ObjectValidator(item);
                itemValidator.accept(validator);
                ValidationResult result = validator.validate();
                if (!result.valid()) {
                    failureReason = "item_" + i + "." + result.reason();
                    return this;
                }
            }
            return this;
        }

        public ArrayValidator requireStringItems() {
            if (failureReason != null) {
                return this;
            }
            for (int i = 0; i < array.size(); i++) {
                Object item = array.get(i);
                if (!(item instanceof CharSequence sequence) || StrUtil.isBlank(sequence.toString())) {
                    failureReason = "item_" + i + "_not_string";
                    return this;
                }
            }
            return this;
        }

        public ValidationResult validate() {
            return failureReason == null ? ValidationResult.success() : ValidationResult.failure(failureReason);
        }
    }
}
