package com.xiaou.mockinterview.controller;

import com.xiaou.common.core.domain.Result;
import com.xiaou.common.satoken.StpUserUtil;
import com.xiaou.mockinterview.domain.CareerLoopAction;
import com.xiaou.mockinterview.domain.CareerLoopSession;
import com.xiaou.mockinterview.domain.CareerLoopStageLog;
import com.xiaou.mockinterview.dto.request.CareerLoopEventRequest;
import com.xiaou.mockinterview.dto.request.CareerLoopProfileUpdateRequest;
import com.xiaou.mockinterview.dto.request.CareerLoopStartRequest;
import com.xiaou.mockinterview.dto.response.CareerLoopCurrentResponse;
import com.xiaou.mockinterview.service.CareerLoopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 求职闭环中台控制器
 *
 * @author xiaou
 */
@Tag(name = "求职闭环中台-用户端", description = "统一管理求职闭环进度")
@RestController
@RequestMapping("/user/career-loop")
@RequiredArgsConstructor
public class CareerLoopController {

    private final CareerLoopService careerLoopService;

    @Operation(summary = "启动/获取当前闭环会话")
    @PostMapping("/start")
    public Result<CareerLoopSession> start(@RequestBody(required = false) CareerLoopStartRequest request) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        CareerLoopSession session = careerLoopService.start(userId, request == null ? new CareerLoopStartRequest() : request);
        return Result.success(session);
    }

    @Operation(summary = "获取当前闭环状态")
    @GetMapping("/current")
    public Result<CareerLoopCurrentResponse> current() {
        Long userId = StpUserUtil.getLoginIdAsLong();
        return Result.success(careerLoopService.getCurrent(userId));
    }

    @Operation(summary = "获取闭环时间线")
    @GetMapping("/timeline")
    public Result<List<CareerLoopStageLog>> timeline() {
        Long userId = StpUserUtil.getLoginIdAsLong();
        return Result.success(careerLoopService.getTimeline(userId));
    }

    @Operation(summary = "获取闭环动作清单")
    @GetMapping("/actions")
    public Result<List<CareerLoopAction>> actions() {
        Long userId = StpUserUtil.getLoginIdAsLong();
        return Result.success(careerLoopService.getActions(userId));
    }

    @Operation(summary = "完成动作项")
    @PostMapping("/actions/{id}/done")
    public Result<Void> completeAction(@PathVariable("id") Long actionId) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        careerLoopService.markActionDone(userId, actionId);
        return Result.success();
    }

    @Operation(summary = "更新闭环会话目标配置")
    @PostMapping("/profile")
    public Result<CareerLoopSession> updateProfile(@RequestBody(required = false) CareerLoopProfileUpdateRequest request) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        CareerLoopProfileUpdateRequest body = request == null ? new CareerLoopProfileUpdateRequest() : request;
        return Result.success(careerLoopService.updateProfile(userId, body));
    }

    @Operation(summary = "手动同步闭环状态")
    @PostMapping("/sync")
    public Result<CareerLoopCurrentResponse> sync(@RequestBody(required = false) CareerLoopEventRequest request) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        return Result.success(careerLoopService.sync(userId, request));
    }

    @Operation(summary = "上报阶段事件")
    @PostMapping("/event")
    public Result<Void> event(@RequestBody CareerLoopEventRequest request) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        careerLoopService.onEvent(userId, request);
        return Result.success();
    }
}

