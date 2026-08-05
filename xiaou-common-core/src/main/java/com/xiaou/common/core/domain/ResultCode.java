package com.xiaou.common.core.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应状态码枚举
 *
 * @author xiaou
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    /* 成功状态码 */
    SUCCESS(200, "操作成功"),

    /* 客户端错误 */
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权访问"),
    FORBIDDEN(403, "访问被禁止"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    REQUEST_TIMEOUT(408, "请求超时"),
    CONFLICT(409, "数据冲突"),
    UNSUPPORTED_MEDIA_TYPE(415, "不支持的媒体类型"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),

    /* 服务端错误 */
    ERROR(500, "系统内部错误"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),

    /* 业务错误码 */
    BUSINESS_ERROR(600, "业务处理失败"),
    PARAM_VALIDATE_ERROR(601, "参数校验失败"),
    DATA_NOT_EXIST(602, "数据不存在"),
    DATA_ALREADY_EXIST(603, "数据已存在"),
    OPERATION_NOT_ALLOWED(604, "操作不被允许"),

    /* 认证授权相关 */
    TOKEN_INVALID(701, "Token无效"),
    TOKEN_EXPIRED(702, "Token已过期"),
    PERMISSION_DENIED(703, "权限不足"),
    ACCOUNT_DISABLED(704, "账户已被禁用"),
    LOGIN_FAILED(705, "登录失败"),

    /* 文件相关 */
    FILE_UPLOAD_ERROR(801, "文件上传失败"),
    FILE_DOWNLOAD_ERROR(802, "文件下载失败"),
    FILE_NOT_EXIST(803, "文件不存在"),
    FILE_TYPE_ERROR(804, "文件类型不支持"),
    FILE_SIZE_EXCEEDED(805, "文件大小超出限制");

    /**
     * 状态码
     */
    private final Integer code;

    /**
     * 状态消息
     */
    private final String message;

    /**
     * 根据状态码获取枚举
     */
    public static ResultCode getByCode(Integer code) {
        for (ResultCode resultCode : values()) {
            if (resultCode.getCode().equals(code)) {
                return resultCode;
            }
        }
        return ERROR;
    }
}
