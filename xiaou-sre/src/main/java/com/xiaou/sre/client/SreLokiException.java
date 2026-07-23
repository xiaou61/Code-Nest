package com.xiaou.sre.client;

/**
 * Loki 只读查询失败异常。
 *
 * @author xiaou
 */
public class SreLokiException extends RuntimeException {

    public SreLokiException(String message) {
        super(message);
    }

    public SreLokiException(String message, Throwable cause) {
        super(message, cause);
    }
}
