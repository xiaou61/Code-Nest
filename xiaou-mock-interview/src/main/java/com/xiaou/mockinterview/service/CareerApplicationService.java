package com.xiaou.mockinterview.service;

import com.xiaou.mockinterview.dto.request.CareerApplicationUpsertRequest;
import com.xiaou.mockinterview.dto.response.CareerApplicationResponse;
import com.xiaou.mockinterview.dto.response.CareerApplicationSummaryResponse;

import java.util.List;

/**
 * 求职投递记录服务。
 */
public interface CareerApplicationService {

    List<CareerApplicationResponse> listForUser(Long userId, String status);

    CareerApplicationSummaryResponse getSummary(Long userId);

    CareerApplicationResponse create(Long userId, CareerApplicationUpsertRequest request);

    CareerApplicationResponse update(Long userId, Long recordId, CareerApplicationUpsertRequest request);

    void delete(Long userId, Long recordId);
}
