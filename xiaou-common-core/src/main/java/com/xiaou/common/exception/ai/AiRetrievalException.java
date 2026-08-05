package com.xiaou.common.exception.ai;

/**
 * AI 检索异常。
 *
 * @author xiaou
 */
public class AiRetrievalException extends RuntimeException {

    public AiRetrievalException(String message) {
        super(message);
    }

    public AiRetrievalException(String message, Throwable cause) {
        super(message, cause);
    }
}
