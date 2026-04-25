package com.xiaou.common.exception.ai;

/**
 * AI 调用异常。
 *
 * @author xiaou
 */
public class AiInvocationException extends RuntimeException {

    public AiInvocationException(String message) {
        super(message);
    }

    public AiInvocationException(String message, Throwable cause) {
        super(message, cause);
    }
}
