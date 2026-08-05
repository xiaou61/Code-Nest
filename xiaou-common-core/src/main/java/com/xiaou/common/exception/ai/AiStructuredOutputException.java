package com.xiaou.common.exception.ai;

/**
 * AI 结构化输出异常。
 *
 * @author xiaou
 */
public class AiStructuredOutputException extends RuntimeException {

    public AiStructuredOutputException(String message) {
        super(message);
    }

    public AiStructuredOutputException(String message, Throwable cause) {
        super(message, cause);
    }
}
