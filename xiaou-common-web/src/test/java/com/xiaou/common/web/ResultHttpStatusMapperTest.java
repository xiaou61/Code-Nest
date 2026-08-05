package com.xiaou.common.web;

import com.xiaou.common.core.domain.ResultCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultHttpStatusMapperTest {

    @Test
    void mapsTransportAndDomainCodesToStableHttpStatuses() {
        assertEquals(HttpStatus.BAD_REQUEST, ResultHttpStatusMapper.resolve(ResultCode.BAD_REQUEST.getCode()));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY,
                ResultHttpStatusMapper.resolve(ResultCode.BUSINESS_ERROR.getCode()));
        assertEquals(HttpStatus.BAD_REQUEST,
                ResultHttpStatusMapper.resolve(ResultCode.PARAM_VALIDATE_ERROR.getCode()));
        assertEquals(HttpStatus.NOT_FOUND, ResultHttpStatusMapper.resolve(ResultCode.DATA_NOT_EXIST.getCode()));
        assertEquals(HttpStatus.CONFLICT, ResultHttpStatusMapper.resolve(ResultCode.DATA_ALREADY_EXIST.getCode()));
        assertEquals(HttpStatus.CONFLICT, ResultHttpStatusMapper.resolve(ResultCode.OPERATION_NOT_ALLOWED.getCode()));
        assertEquals(HttpStatus.UNAUTHORIZED, ResultHttpStatusMapper.resolve(ResultCode.TOKEN_INVALID.getCode()));
        assertEquals(HttpStatus.UNAUTHORIZED, ResultHttpStatusMapper.resolve(ResultCode.TOKEN_EXPIRED.getCode()));
        assertEquals(HttpStatus.FORBIDDEN, ResultHttpStatusMapper.resolve(ResultCode.PERMISSION_DENIED.getCode()));
        assertEquals(HttpStatus.FORBIDDEN, ResultHttpStatusMapper.resolve(ResultCode.ACCOUNT_DISABLED.getCode()));
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE,
                ResultHttpStatusMapper.resolve(ResultCode.FILE_SIZE_EXCEEDED.getCode()));
    }

    @Test
    void treatsUnknownApplicationErrorsAsUnprocessableContent() {
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ResultHttpStatusMapper.resolve(999));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ResultHttpStatusMapper.resolve(null));
    }
}
