package com.xiaou.common.exception.ai;

/**
 * AI 配置异常。
 *
 * @author xiaou
 */
public class AiConfigurationException extends RuntimeException {

    public AiConfigurationException(String message) {
        super(message);
    }

    public AiConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
