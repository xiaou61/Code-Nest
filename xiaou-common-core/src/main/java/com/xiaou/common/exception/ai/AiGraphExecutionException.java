package com.xiaou.common.exception.ai;

/**
 * AI 图编排执行异常。
 *
 * @author xiaou
 */
public class AiGraphExecutionException extends RuntimeException {

    public AiGraphExecutionException(String message) {
        super(message);
    }

    public AiGraphExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
