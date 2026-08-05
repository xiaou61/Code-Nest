package com.xiaou.common.annotation;

import java.lang.annotation.*;

/**
 * 管理员权限验证注解
 * 用于标记需要管理员权限的方法
 *
 * @author xiaou
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireAdmin {

    /**
     * 是否允许超级管理员访问
     *
     * @return 默认为true
     */
    boolean allowSuperAdmin() default true;

    /**
     * 错误消息
     *
     * @return 默认错误消息
     */
    String message() default "需要管理员权限";
}
