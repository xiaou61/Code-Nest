package com.xiaou.ai.eval;

import org.junit.jupiter.api.Assertions;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 回归评测断言工具。
 *
 * @author xiaou
 */
public final class AiEvalAssertions {

    private AiEvalAssertions() {
    }

    public static void assertMatches(AiEvalCase testCase, Object actualResult) {
        Assertions.assertNotNull(actualResult, prefix(testCase) + "结果不能为空");

        AiEvalExpectation expectation = testCase.getExpect();
        Assertions.assertNotNull(expectation, prefix(testCase) + "缺少 expect 断言定义");

        BeanWrapper beanWrapper = new BeanWrapperImpl(actualResult);

        if (expectation.getFallback() != null) {
            Object value = readProperty(beanWrapper, "fallback", testCase);
            Assertions.assertEquals(expectation.getFallback(), value, prefix(testCase) + "fallback 不符合预期");
        }

        assertExactStrings(expectation.getExactStrings(), beanWrapper, testCase);
        assertExactIntegers(expectation.getExactIntegers(), beanWrapper, testCase);
        assertIntRanges(expectation.getIntRanges(), beanWrapper, testCase);
        assertTextContains(expectation.getTextContains(), beanWrapper, testCase);
        assertListContains(expectation.getListContains(), beanWrapper, testCase);
        assertMinListSize(expectation.getMinListSize(), beanWrapper, testCase);
    }

    private static void assertExactStrings(Map<String, String> exactStrings, BeanWrapper beanWrapper, AiEvalCase testCase) {
        if (exactStrings == null) {
            return;
        }
        for (Map.Entry<String, String> entry : exactStrings.entrySet()) {
            Object actual = readProperty(beanWrapper, entry.getKey(), testCase);
            Assertions.assertEquals(entry.getValue(), actual, prefix(testCase) + entry.getKey() + " 字段值不匹配");
        }
    }

    private static void assertExactIntegers(Map<String, Integer> exactIntegers, BeanWrapper beanWrapper, AiEvalCase testCase) {
        if (exactIntegers == null) {
            return;
        }
        for (Map.Entry<String, Integer> entry : exactIntegers.entrySet()) {
            int actual = toInt(readProperty(beanWrapper, entry.getKey(), testCase), testCase, entry.getKey());
            Assertions.assertEquals(entry.getValue(), actual, prefix(testCase) + entry.getKey() + " 数值不匹配");
        }
    }

    private static void assertIntRanges(Map<String, AiEvalIntegerRange> intRanges, BeanWrapper beanWrapper, AiEvalCase testCase) {
        if (intRanges == null) {
            return;
        }
        for (Map.Entry<String, AiEvalIntegerRange> entry : intRanges.entrySet()) {
            int actual = toInt(readProperty(beanWrapper, entry.getKey(), testCase), testCase, entry.getKey());
            AiEvalIntegerRange range = entry.getValue();
            if (range.getMin() != null) {
                Assertions.assertTrue(actual >= range.getMin(),
                        prefix(testCase) + entry.getKey() + " 小于最小值 " + range.getMin());
            }
            if (range.getMax() != null) {
                Assertions.assertTrue(actual <= range.getMax(),
                        prefix(testCase) + entry.getKey() + " 大于最大值 " + range.getMax());
            }
        }
    }

    private static void assertTextContains(Map<String, List<String>> textContains, BeanWrapper beanWrapper, AiEvalCase testCase) {
        if (textContains == null) {
            return;
        }
        for (Map.Entry<String, List<String>> entry : textContains.entrySet()) {
            Object actual = readProperty(beanWrapper, entry.getKey(), testCase);
            Assertions.assertInstanceOf(String.class, actual, prefix(testCase) + entry.getKey() + " 不是字符串字段");
            String actualText = (String) actual;
            for (String fragment : entry.getValue()) {
                Assertions.assertTrue(actualText != null && actualText.contains(fragment),
                        prefix(testCase) + entry.getKey() + " 未包含预期片段: " + fragment);
            }
        }
    }

    private static void assertListContains(Map<String, List<String>> listContains, BeanWrapper beanWrapper, AiEvalCase testCase) {
        if (listContains == null) {
            return;
        }
        for (Map.Entry<String, List<String>> entry : listContains.entrySet()) {
            Collection<?> actualCollection = asCollection(readProperty(beanWrapper, entry.getKey(), testCase), testCase, entry.getKey());
            List<String> actualValues = actualCollection.stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList());
            for (String expectedItem : entry.getValue()) {
                Assertions.assertTrue(actualValues.contains(expectedItem),
                        prefix(testCase) + entry.getKey() + " 未包含预期元素: " + expectedItem);
            }
        }
    }

    private static void assertMinListSize(Map<String, Integer> minListSize, BeanWrapper beanWrapper, AiEvalCase testCase) {
        if (minListSize == null) {
            return;
        }
        for (Map.Entry<String, Integer> entry : minListSize.entrySet()) {
            Collection<?> actualCollection = asCollection(readProperty(beanWrapper, entry.getKey(), testCase), testCase, entry.getKey());
            Assertions.assertTrue(actualCollection.size() >= entry.getValue(),
                    prefix(testCase) + entry.getKey() + " 长度小于最小要求 " + entry.getValue());
        }
    }

    private static Object readProperty(BeanWrapper beanWrapper, String propertyPath, AiEvalCase testCase) {
        Assertions.assertTrue(beanWrapper.isReadableProperty(propertyPath),
                prefix(testCase) + "不存在可读取字段: " + propertyPath);
        return beanWrapper.getPropertyValue(propertyPath);
    }

    private static int toInt(Object value, AiEvalCase testCase, String propertyPath) {
        Assertions.assertNotNull(value, prefix(testCase) + propertyPath + " 不能为空");
        if (value instanceof Number number) {
            return number.intValue();
        }
        Assertions.fail(prefix(testCase) + propertyPath + " 不是整数类型");
        return 0;
    }

    private static Collection<?> asCollection(Object value, AiEvalCase testCase, String propertyPath) {
        Assertions.assertNotNull(value, prefix(testCase) + propertyPath + " 不能为空");
        Assertions.assertInstanceOf(Collection.class, value, prefix(testCase) + propertyPath + " 不是集合类型");
        return (Collection<?>) value;
    }

    private static String prefix(AiEvalCase testCase) {
        return "[AI-EVAL " + testCase.getId() + "] ";
    }
}
