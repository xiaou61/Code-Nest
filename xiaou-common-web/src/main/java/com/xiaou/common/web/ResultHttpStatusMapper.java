package com.xiaou.common.web;

import com.xiaou.common.core.domain.ResultCode;
import org.springframework.http.HttpStatus;

final class ResultHttpStatusMapper {

    private ResultHttpStatusMapper() {
    }

    static HttpStatus resolve(Integer code) {
        if (code == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }

        if (ResultCode.BUSINESS_ERROR.getCode().equals(code)) {
            return HttpStatus.UNPROCESSABLE_ENTITY;
        }
        if (ResultCode.PARAM_VALIDATE_ERROR.getCode().equals(code)) {
            return HttpStatus.BAD_REQUEST;
        }
        if (ResultCode.DATA_NOT_EXIST.getCode().equals(code)
                || ResultCode.FILE_NOT_EXIST.getCode().equals(code)) {
            return HttpStatus.NOT_FOUND;
        }
        if (ResultCode.DATA_ALREADY_EXIST.getCode().equals(code)
                || ResultCode.OPERATION_NOT_ALLOWED.getCode().equals(code)) {
            return HttpStatus.CONFLICT;
        }
        if (ResultCode.TOKEN_INVALID.getCode().equals(code)
                || ResultCode.TOKEN_EXPIRED.getCode().equals(code)
                || ResultCode.LOGIN_FAILED.getCode().equals(code)) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (ResultCode.PERMISSION_DENIED.getCode().equals(code)
                || ResultCode.ACCOUNT_DISABLED.getCode().equals(code)) {
            return HttpStatus.FORBIDDEN;
        }
        if (ResultCode.FILE_TYPE_ERROR.getCode().equals(code)) {
            return HttpStatus.UNSUPPORTED_MEDIA_TYPE;
        }
        if (ResultCode.FILE_SIZE_EXCEEDED.getCode().equals(code)) {
            return HttpStatus.PAYLOAD_TOO_LARGE;
        }
        if (ResultCode.FILE_UPLOAD_ERROR.getCode().equals(code)
                || ResultCode.FILE_DOWNLOAD_ERROR.getCode().equals(code)) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }

        HttpStatus transportStatus = HttpStatus.resolve(code);
        return transportStatus != null ? transportStatus : HttpStatus.UNPROCESSABLE_ENTITY;
    }
}
