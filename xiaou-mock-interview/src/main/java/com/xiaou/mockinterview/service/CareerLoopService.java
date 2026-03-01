package com.xiaou.mockinterview.service;

import com.xiaou.mockinterview.domain.CareerLoopAction;
import com.xiaou.mockinterview.domain.CareerLoopSession;
import com.xiaou.mockinterview.domain.CareerLoopStageLog;
import com.xiaou.mockinterview.dto.request.CareerLoopEventRequest;
import com.xiaou.mockinterview.dto.request.CareerLoopStartRequest;
import com.xiaou.mockinterview.dto.response.CareerLoopCurrentResponse;

import java.util.List;

/**
 * 求职闭环中台服务
 *
 * @author xiaou
 */
public interface CareerLoopService {

    CareerLoopSession start(Long userId, CareerLoopStartRequest request);

    CareerLoopCurrentResponse getCurrent(Long userId);

    List<CareerLoopStageLog> getTimeline(Long userId);

    List<CareerLoopAction> getActions(Long userId);

    void markActionDone(Long userId, Long actionId);

    CareerLoopCurrentResponse sync(Long userId, CareerLoopEventRequest request);

    void onEvent(Long userId, CareerLoopEventRequest request);
}

