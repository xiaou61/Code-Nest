package com.xiaou.ai.structured;

import java.util.Set;
import java.util.function.Consumer;

/**
 * 结构化输出校验契约适配器。
 *
 * @author xiaou
 */
final class AiStructuredValidationContracts {

    private AiStructuredValidationContracts() {
    }

    static AiStructuredObjectContract object(AiStructuredOutputValidator.ObjectValidator validator) {
        return new ValidationObjectContract(validator);
    }

    static AiStructuredArrayContract array(AiStructuredOutputValidator.ArrayValidator validator) {
        return new ValidationArrayContract(validator);
    }

    private static final class ValidationObjectContract implements AiStructuredObjectContract {

        private final AiStructuredOutputValidator.ObjectValidator delegate;

        private ValidationObjectContract(AiStructuredOutputValidator.ObjectValidator delegate) {
            this.delegate = delegate;
        }

        @Override
        public AiStructuredObjectContract requireString(String key) {
            delegate.requireString(key);
            return this;
        }

        @Override
        public AiStructuredObjectContract requireStringInSet(String key, Set<String> allowedValues) {
            delegate.requireStringInSet(key, allowedValues);
            return this;
        }

        @Override
        public AiStructuredObjectContract requireInt(String key) {
            delegate.requireInt(key);
            return this;
        }

        @Override
        public AiStructuredObjectContract requireIntRange(String key, int min, int max) {
            delegate.requireIntRange(key, min, max);
            return this;
        }

        @Override
        public AiStructuredObjectContract requirePositiveInt(String key) {
            delegate.requirePositiveInt(key);
            return this;
        }

        @Override
        public AiStructuredObjectContract requireStringArray(String key) {
            delegate.requireStringArray(key);
            return this;
        }

        @Override
        public AiStructuredObjectContract requireStringOrStringArray(String key) {
            delegate.requireStringOrStringArray(key);
            return this;
        }

        @Override
        public AiStructuredObjectContract requireObject(String key, Consumer<AiStructuredObjectContract> nestedValidator) {
            delegate.requireObject(key, nested -> nestedValidator.accept(new ValidationObjectContract(nested)));
            return this;
        }

        @Override
        public AiStructuredObjectContract requireObjectArray(String key, Consumer<AiStructuredObjectContract> itemValidator) {
            delegate.requireObjectArray(key, item -> itemValidator.accept(new ValidationObjectContract(item)));
            return this;
        }
    }

    private static final class ValidationArrayContract implements AiStructuredArrayContract {

        private final AiStructuredOutputValidator.ArrayValidator delegate;

        private ValidationArrayContract(AiStructuredOutputValidator.ArrayValidator delegate) {
            this.delegate = delegate;
        }

        @Override
        public AiStructuredArrayContract requireObjectItems(Consumer<AiStructuredObjectContract> itemValidator) {
            delegate.requireObjectItems(item -> itemValidator.accept(new ValidationObjectContract(item)));
            return this;
        }

        @Override
        public AiStructuredArrayContract requireStringItems() {
            delegate.requireStringItems();
            return this;
        }
    }
}
