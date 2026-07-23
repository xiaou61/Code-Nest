package com.xiaou.sre.client;

/**
 * Prometheus 只读查询失败异常。
 *
 * @author xiaou
 */
public class SrePrometheusException extends RuntimeException {

    public SrePrometheusException(String message) {
        super(message);
    }

    public SrePrometheusException(String message, Throwable cause) {
        super(message, cause);
    }
}
