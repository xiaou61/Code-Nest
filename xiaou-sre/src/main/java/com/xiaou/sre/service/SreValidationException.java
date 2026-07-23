package com.xiaou.sre.service;

/**
 * SRE webhook 业务载荷校验异常。
 *
 * @author xiaou
 */
public class SreValidationException extends RuntimeException {

    public SreValidationException(String message) {
        super(message);
    }
}
