package com.xiaou.ai.regression;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 回归断言支撑。
 *
 * @author xiaou
 */
public final class AiRegressionAssertionSupport {

    private AiRegressionAssertionSupport() {
    }

    public static List<String> validate(AiRegressionCase testCase, Object actualResult) {
        ArrayList<String> errors = new ArrayList<>();
        if (actualResult == null) {
            errors.add(prefix(testCase) + "结果不能为空");
            return errors;
        }

        AiRegressionExpectation expectation = testCase.getExpect();
        if (expectation == null) {
            errors.add(prefix(testCase) + "缺少 expect 断言定义");
            return errors;
        }

        BeanWrapper beanWrapper = new BeanWrapperImpl(actualResult);

        if (expectation.getFallback() != null) {
            Object value = readProperty(beanWrapper, "fallback", testCase, errors);
            if (value != null && !expectation.getFallback().equals(value)) {
                errors.add(prefix(testCase) + "fallback 不符合预期");
            }
        }

        validateExactStrings(expectation.getExactStrings(), beanWrapper, testCase, errors);
        validateExactIntegers(expectation.getExactIntegers(), beanWrapper, testCase, errors);
        validateIntegerRanges(expectation.getIntRanges(), beanWrapper, testCase, errors);
        validateTextContains(expectation.getTextContains(), beanWrapper, testCase, errors);
        validateListContains(expectation.getListContains(), beanWrapper, testCase, errors);
        validateMinListSize(expectation.getMinListSize(), beanWrapper, testCase, errors);
        return errors;
    }

    public static Boolean readFallback(Object actualResult) {
        if (actualResult == null) {
            return null;
        }
        BeanWrapper beanWrapper = new BeanWrapperImpl(actualResult);
        if (!beanWrapper.isReadableProperty("fallback")) {
            return null;
        }
        Object value = beanWrapper.getPropertyValue("fallback");
        return value instanceof Boolean booleanValue ? booleanValue : null;
    }

    private static void validateExactStrings(Map<String, String> exactStrings,
                                             BeanWrapper beanWrapper,
                                             AiRegressionCase testCase,
                                             List<String> errors) {
        if (exactStrings == null) {
            return;
        }
        for (Map.Entry<String, String> entry : exactStrings.entrySet()) {
            Object actual = readProperty(beanWrapper, entry.getKey(), testCase, errors);
            if (actual != null && !entry.getValue().equals(actual)) {
                errors.add(prefix(testCase) + entry.getKey() + " 字段值不匹配");
            }
        }
    }

    private static void validateExactIntegers(Map<String, Integer> exactIntegers,
                                              BeanWrapper beanWrapper,
                                              AiRegressionCase testCase,
                                              List<String> errors) {
        if (exactIntegers == null) {
            return;
        }
        for (Map.Entry<String, Integer> entry : exactIntegers.entrySet()) {
            Integer actual = toInteger(readProperty(beanWrapper, entry.getKey(), testCase, errors), testCase, entry.getKey(), errors);
            if (actual != null && !entry.getValue().equals(actual)) {
                errors.add(prefix(testCase) + entry.getKey() + " 数值不匹配");
            }
        }
    }

    private static void validateIntegerRanges(Map<String, AiRegressionIntegerRange> intRanges,
                                              BeanWrapper beanWrapper,
                                              AiRegressionCase testCase,
                                              List<String> errors) {
        if (intRanges == null) {
            return;
        }
        for (Map.Entry<String, AiRegressionIntegerRange> entry : intRanges.entrySet()) {
            Integer actual = toInteger(readProperty(beanWrapper, entry.getKey(), testCase, errors), testCase, entry.getKey(), errors);
            if (actual == null) {
                continue;
            }
            AiRegressionIntegerRange range = entry.getValue();
            if (range.getMin() != null && actual < range.getMin()) {
                errors.add(prefix(testCase) + entry.getKey() + " 小于最小值 " + range.getMin());
            }
            if (range.getMax() != null && actual > range.getMax()) {
                errors.add(prefix(testCase) + entry.getKey() + " 大于最大值 " + range.getMax());
            }
        }
    }

    private static void validateTextContains(Map<String, List<String>> textContains,
                                             BeanWrapper beanWrapper,
                                             AiRegressionCase testCase,
                                             List<String> errors) {
        if (textContains == null) {
            return;
        }
        for (Map.Entry<String, List<String>> entry : textContains.entrySet()) {
            Object actual = readProperty(beanWrapper, entry.getKey(), testCase, errors);
            if (!(actual instanceof String actualText)) {
                if (actual != null) {
                    errors.add(prefix(testCase) + entry.getKey() + " 不是字符串字段");
                }
                continue;
            }
            for (String fragment : entry.getValue()) {
                if (actualText == null || !actualText.contains(fragment)) {
                    errors.add(prefix(testCase) + entry.getKey() + " 未包含预期片段: " + fragment);
                }
            }
        }
    }

    private static void validateListContains(Map<String, List<String>> listContains,
                                             BeanWrapper beanWrapper,
                                             AiRegressionCase testCase,
                                             List<String> errors) {
        if (listContains == null) {
            return;
        }
        for (Map.Entry<String, List<String>> entry : listContains.entrySet()) {
            Collection<?> actualCollection = asCollection(
                    readProperty(beanWrapper, entry.getKey(), testCase, errors),
                    testCase,
                    entry.getKey(),
                    errors
            );
            if (actualCollection == null) {
                continue;
            }
            List<String> actualValues = actualCollection.stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList());
            for (String expectedItem : entry.getValue()) {
                if (!actualValues.contains(expectedItem)) {
                    errors.add(prefix(testCase) + entry.getKey() + " 未包含预期元素: " + expectedItem);
                }
            }
        }
    }

    private static void validateMinListSize(Map<String, Integer> minListSize,
                                            BeanWrapper beanWrapper,
                                            AiRegressionCase testCase,
                                            List<String> errors) {
        if (minListSize == null) {
            return;
        }
        for (Map.Entry<String, Integer> entry : minListSize.entrySet()) {
            Collection<?> actualCollection = asCollection(
                    readProperty(beanWrapper, entry.getKey(), testCase, errors),
                    testCase,
                    entry.getKey(),
                    errors
            );
            if (actualCollection == null) {
                continue;
            }
            if (actualCollection.size() < entry.getValue()) {
                errors.add(prefix(testCase) + entry.getKey() + " 长度小于最小要求 " + entry.getValue());
            }
        }
    }

    private static Object readProperty(BeanWrapper beanWrapper, String propertyPath, AiRegressionCase testCase, List<String> errors) {
        if (!beanWrapper.isReadableProperty(propertyPath)) {
            errors.add(prefix(testCase) + "不存在可读取字段: " + propertyPath);
            return null;
        }
        return beanWrapper.getPropertyValue(propertyPath);
    }

    private static Integer toInteger(Object value,
                                     AiRegressionCase testCase,
                                     String propertyPath,
                                     List<String> errors) {
        if (value == null) {
            errors.add(prefix(testCase) + propertyPath + " 不能为空");
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        errors.add(prefix(testCase) + propertyPath + " 不是整数类型");
        return null;
    }

    private static Collection<?> asCollection(Object value,
                                              AiRegressionCase testCase,
                                              String propertyPath,
                                              List<String> errors) {
        if (value == null) {
            errors.add(prefix(testCase) + propertyPath + " 不能为空");
            return null;
        }
        if (!(value instanceof Collection<?> collection)) {
            errors.add(prefix(testCase) + propertyPath + " 不是集合类型");
            return null;
        }
        return collection;
    }

    private static String prefix(AiRegressionCase testCase) {
        return "[AI-REGRESSION " + testCase.getId() + "] ";
    }
}
