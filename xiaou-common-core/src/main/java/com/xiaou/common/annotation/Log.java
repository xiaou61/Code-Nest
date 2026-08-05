package com.xiaou.common.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 *
 * @author xiaou
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {

    /**
     * 操作模块
     */
    String module() default "";

    /**
     * 操作类型
     */
    OperationType type() default OperationType.OTHER;

    /**
     * 操作描述
     */
    String description() default "";

    /**
     * 是否保存请求参数
     */
    boolean saveRequestData() default true;

    /**
     * 是否保存响应参数
     */
    boolean saveResponseData() default true;

    /**
     * 操作类型枚举
     */
    enum OperationType {
        /**
         * 查询
         */
        SELECT("查询"),

        /**
         * 新增
         */
        INSERT("新增"),

        /**
         * 修改
         */
        UPDATE("修改"),

        /**
         * 删除
         */
        DELETE("删除"),

        /**
         * 授权
         */
        GRANT("授权"),

        /**
         * 导出
         */
        EXPORT("导出"),

        /**
         * 导入
         */
        IMPORT("导入"),

        /**
         * 强退
         */
        FORCE("强退"),

        /**
         * 生成代码
         */
        GENCODE("生成代码"),

        /**
         * 清空数据
         */
        CLEAN("清空数据"),

        /**
         * 其它
         */
        OTHER("其它");

        private final String value;

        OperationType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
