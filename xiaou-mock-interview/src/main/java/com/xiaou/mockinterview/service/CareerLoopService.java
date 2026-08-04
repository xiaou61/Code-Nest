package com.xiaou.mockinterview.service;

import com.xiaou.mockinterview.domain.CareerLoopAction;
import com.xiaou.mockinterview.domain.CareerLoopSession;
import com.xiaou.mockinterview.domain.CareerLoopStageLog;
import com.xiaou.mockinterview.dto.request.CareerLoopEventRequest;
import com.xiaou.mockinterview.dto.request.CareerLoopProfileUpdateRequest;
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

    /**
     * 只读取已存在的活跃会话；不存在时返回 null，不创建会话或动作。
     */
    CareerLoopCurrentResponse findCurrentIfPresent(Long userId);

    List<CareerLoopStageLog> getTimeline(Long userId);

    List<CareerLoopAction> getActions(Long userId);

    void markActionDone(Long userId, Long actionId);

    /**
     * 只更新已存在活跃会话中的动作，不触发会话自动初始化。
     */
    void markExistingActionDone(Long userId, Long actionId);

    CareerLoopSession updateProfile(Long userId, CareerLoopProfileUpdateRequest request);

    CareerLoopCurrentResponse sync(Long userId, CareerLoopEventRequest request);

    void onEvent(Long userId, CareerLoopEventRequest request);
}

