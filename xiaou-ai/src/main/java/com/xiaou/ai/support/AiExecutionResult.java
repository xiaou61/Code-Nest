package com.xiaou.ai.support;

/**
 * 带降级状态和运行时来源信息的 AI 执行结果。
 *
 * @param value           场景解析后的值或确定性降级值
 * @param outcome         SUCCESS/MODEL_UNAVAILABLE/EMPTY_RESPONSE/INVOCATION_EXCEPTION/PARSER_FAILURE
 * @param provider        请求使用的配置提供商
 * @param configuredModel 请求构建时配置的模型
 * @param actualModel     上游响应声明的实际模型，未发起或未返回时为空
 * @param <T>             场景结果类型
 * @author xiaou
 */
public record AiExecutionResult<T>(
        T value,
        String outcome,
        String provider,
        String configuredModel,
        String actualModel
) {
}
