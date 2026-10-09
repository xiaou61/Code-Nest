package io.github.xiaou61.platform;

/**
 * 稳定错误码。已发布的数值不再变更，新增只能追加。
 */
public enum ErrorCode {

    OK(0, "ok"),
    INVALID_ARGUMENT(40000, "请求参数不合法"),
    UNAUTHENTICATED(40100, "未认证"),
    FORBIDDEN(40300, "无权访问"),
    NOT_FOUND(40400, "资源不存在"),
    CONFLICT(40900, "状态冲突"),
    INTERNAL(50000, "服务器内部错误");

    private final int code;
    private final String defaultMessage;

    ErrorCode(int code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public int code() {
        return code;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
