package com.xiaou.ai.prompt;

import org.springframework.util.Assert;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Prompt/RAG Spec 目录收集工具。
 *
 * @author xiaou
 */
final class AiCatalogSupport {

    private AiCatalogSupport() {
    }

    static <T> List<T> collectStaticSpecs(List<Class<?>> holders, Class<T> specType) {
        List<T> specs = new ArrayList<>();
        for (Class<?> holder : holders) {
            for (Field field : holder.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) || !Modifier.isPublic(field.getModifiers())) {
                    continue;
                }
                if (!specType.equals(field.getType())) {
                    continue;
                }

                try {
                    Object value = field.get(null);
                    Assert.notNull(value, () -> holder.getSimpleName() + "." + field.getName() + " 不能为空");
                    specs.add(specType.cast(value));
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("读取 Spec 目录失败: " + holder.getName() + "." + field.getName(), e);
                }
            }
        }
        return List.copyOf(specs);
    }
}
