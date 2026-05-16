package com.xiaou.common.exception;

import cn.dev33.satoken.exception.DisableServiceException;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.core.domain.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;


import java.util.Objects;
import java.util.Set;

/**
 * 全局异常处理器
 *
 * @author xiaou
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常处理
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * Sa-Token 未登录异常处理
     *
     * 统一返回 HTTP 200，通过业务状态码区分（701/702）
     * 这样前端可以统一在响应拦截器中处理，而不是分散在 HTTP 状态码和业务状态码两个地方
     */
    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleNotLoginException(NotLoginException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.warn("请求地址'{}',Token验证失败: {}", requestURI, e.getMessage());

        // 根据不同的异常类型返回不同的消息和状态码
        String message;
        Integer code;
        switch (e.getType()) {
            case NotLoginException.NOT_TOKEN:
                message = "未提供Token，请先登录";
                code = ResultCode.TOKEN_INVALID.getCode(); // 701
                break;
            case NotLoginException.INVALID_TOKEN:
                message = "Token无效，请重新登录";
                code = ResultCode.TOKEN_INVALID.getCode(); // 701
                break;
            case NotLoginException.TOKEN_TIMEOUT:
                message = "Token已过期，请重新登录";
                code = ResultCode.TOKEN_EXPIRED.getCode(); // 702
                break;
            case NotLoginException.BE_REPLACED:
                message = "Token已被顶替，请重新登录";
                code = ResultCode.TOKEN_EXPIRED.getCode(); // 702
                break;
            case NotLoginException.KICK_OUT:
                message = "已被踢下线，请重新登录";
                code = ResultCode.TOKEN_EXPIRED.getCode(); // 702
                break;
            default:
                message = "登录状态已失效，请重新登录";
                code = ResultCode.TOKEN_INVALID.getCode(); // 701
        }

        return Result.error(code, message);
    }

    /**
     * Sa-Token 权限不足异常处理
     *
     * 统一返回 HTTP 200，通过业务状态码 703 区分
     */
    @ExceptionHandler(NotPermissionException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleNotPermissionException(NotPermissionException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.warn("请求地址'{}',权限不足: 缺少权限[{}]", requestURI, e.getPermission());
        return Result.error(ResultCode.PERMISSION_DENIED.getCode(), "权限不足，无法访问");
    }

    /**
     * Sa-Token 角色权限不足异常处理
     *
     * 统一返回 HTTP 200，通过业务状态码 703 区分
     */
    @ExceptionHandler(NotRoleException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleNotRoleException(NotRoleException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.warn("请求地址'{}',角色权限不足: 缺少角色[{}]", requestURI, e.getRole());
        return Result.error(ResultCode.PERMISSION_DENIED.getCode(), "角色权限不足，无法访问");
    }

    /**
     * Sa-Token 服务封禁异常处理
     *
     * 统一返回 HTTP 200，通过业务状态码 704 区分
     */
    @ExceptionHandler(DisableServiceException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleDisableServiceException(DisableServiceException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.warn("请求地址'{}',服务已被封禁: {}", requestURI, e.getMessage());
        return Result.error(ResultCode.ACCOUNT_DISABLED.getCode(), "账号已被封禁，请联系管理员");
    }

    /**
     * 请求方式不支持异常处理
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e,
                                                            HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',不支持'{}'请求", requestURI, e.getMethod());
        return Result.error(ResultCode.METHOD_NOT_ALLOWED.getCode(),
                          String.format("请求方法'%s'不支持", e.getMethod()));
    }

    /**
     * 请求媒体类型不支持异常处理
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public Result<Void> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException e,
                                                        HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',不支持'{}'媒体类型", requestURI, e.getContentType());
        return Result.error(ResultCode.UNSUPPORTED_MEDIA_TYPE.getCode(), "不支持的媒体类型");
    }

    /**
     * 请求可接受类型不支持异常处理
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    @ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
    public Result<Void> handleHttpMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException e,
                                                         HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',不支持客户端要求的响应媒体类型", requestURI);
        return Result.error(ResultCode.BAD_REQUEST.getCode(), "不支持的响应媒体类型");
    }

    /**
     * 请求体不可读异常处理
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e,
                                                     HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',请求体解析失败: {}", requestURI, e.getMessage());
        return Result.error(ResultCode.BAD_REQUEST.getCode(), "请求体格式错误或缺少必要内容");
    }

    /**
     * 路径变量缺失异常处理
     */
    @ExceptionHandler(MissingPathVariableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingPathVariable(MissingPathVariableException e,
                                                 HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',缺少必要的路径变量'{}'", requestURI, e.getVariableName());
        return Result.error(ResultCode.BAD_REQUEST.getCode(),
                          String.format("缺少必要的路径变量[%s]", e.getVariableName()));
    }

    /**
     * 请求参数缺失异常处理
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingServletRequestParameter(MissingServletRequestParameterException e,
                                                            HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',缺少必要的请求参数'{}'", requestURI, e.getParameterName());
        return Result.error(ResultCode.BAD_REQUEST.getCode(),
                          String.format("缺少必要的请求参数[%s]", e.getParameterName()));
    }

    /**
     * 请求头缺失异常处理
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingRequestHeader(MissingRequestHeaderException e,
                                                   HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',缺少必要的请求头'{}'", requestURI, e.getHeaderName());
        return Result.error(ResultCode.BAD_REQUEST.getCode(),
                String.format("缺少必要的请求头[%s]", e.getHeaderName()));
    }

    /**
     * 参数类型不匹配异常处理
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e,
                                                         HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',请求参数类型不匹配,参数'{}'需要类型'{}'",
                 requestURI, e.getName(), e.getRequiredType().getName());
        return Result.error(ResultCode.BAD_REQUEST.getCode(),
                          String.format("参数类型不匹配,参数[%s]需要[%s]类型", e.getName(), e.getRequiredType().getSimpleName()));
    }

    /**
     * 请求参数验证异常处理
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
                                                     HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        log.error("请求地址'{}',参数校验失败'{}'", requestURI, message);
        return Result.error(ResultCode.PARAM_VALIDATE_ERROR.getCode(), message);
    }

    /**
     * 绑定异常处理
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e) {
        log.error("参数绑定校验异常", e);
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数绑定失败";
        return Result.error(ResultCode.PARAM_VALIDATE_ERROR.getCode(), message);
    }

    /**
     * 约束违反异常处理
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        log.error("参数校验失败", e);
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        String message = violations.isEmpty() ? "参数校验失败" : violations.iterator().next().getMessage();
        return Result.error(ResultCode.PARAM_VALIDATE_ERROR.getCode(), message);
    }

    /**
     * 非法参数异常处理
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e,
                                                       HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String message = Objects.requireNonNullElse(e.getMessage(), "请求参数错误");
        log.warn("请求地址'{}',非法参数: {}", requestURI, message);
        return Result.error(ResultCode.BAD_REQUEST.getCode(), message);
    }

    /**
     * 文件大小超限异常处理
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public Result<Void> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e,
                                                    HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',上传文件大小超出限制", requestURI, e);
        return Result.error(ResultCode.FILE_SIZE_EXCEEDED.getCode(), ResultCode.FILE_SIZE_EXCEEDED.getMessage());
    }

    /**
     * 404异常处理
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoHandlerFoundException(NoHandlerFoundException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',无法找到对应的处理器", requestURI);
        return Result.error(ResultCode.NOT_FOUND.getCode(), String.format("请求地址'%s'不存在", requestURI));
    }

    /**
     * 系统异常处理
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',发生系统异常.", requestURI, e);
        return Result.error(ResultCode.ERROR.getCode(), "系统内部错误，请联系管理员");
    }
}
