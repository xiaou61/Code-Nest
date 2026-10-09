package io.github.xiaou61.platform;

/**
 * 对外统一返回结构。{@code traceId} 由 web 层在请求入口填充，平台层不感知其来源。
 */
public record ApiResponse<T>(int code, String message, T data, String traceId) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(ErrorCode.OK.code(), ErrorCode.OK.defaultMessage(), data, null);
    }

    public static <T> ApiResponse<T> failure(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.code(), errorCode.defaultMessage(), null, null);
    }

    public static <T> ApiResponse<T> failure(ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.code(), message, null, null);
    }

    public ApiResponse<T> withTraceId(String traceId) {
        return new ApiResponse<>(code, message, data, traceId);
    }
}
