package com.xiaou.aigrowth.controller.user;

import com.xiaou.aigrowth.dto.request.AiGrowthCoachActionStatusRequest;
import com.xiaou.aigrowth.dto.request.AiGrowthCoachChatRequest;
import com.xiaou.aigrowth.dto.request.AiGrowthCoachReplanRequest;
import com.xiaou.aigrowth.dto.request.AiGrowthCoachRefreshRequest;
import com.xiaou.aigrowth.dto.response.AiGrowthCoachChatResponse;
import com.xiaou.aigrowth.dto.response.AiGrowthCoachReplanResponse;
import com.xiaou.aigrowth.dto.response.AiGrowthCoachSnapshotDetailResponse;
import com.xiaou.aigrowth.service.AiGrowthCoachService;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.satoken.StpUserUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户端AI成长教练控制器
 */
@Validated
@RestController
@RequestMapping("/user/ai-growth-coach")
@RequiredArgsConstructor
public class UserAiGrowthCoachController {

    private final AiGrowthCoachService aiGrowthCoachService;

    @GetMapping("/summary")
    public Result<AiGrowthCoachSnapshotDetailResponse> summary(@RequestParam(required = false) String scene) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        return Result.success(aiGrowthCoachService.getSummary(userId, scene));
    }

    @GetMapping("/snapshots/{snapshotId}")
    public Result<AiGrowthCoachSnapshotDetailResponse> detail(@PathVariable Long snapshotId) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        return Result.success(aiGrowthCoachService.getSnapshotDetail(userId, snapshotId));
    }

    @PostMapping("/refresh")
    public Result<AiGrowthCoachSnapshotDetailResponse> refresh(@RequestBody(required = false) AiGrowthCoachRefreshRequest request) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        return Result.success(aiGrowthCoachService.refresh(userId, request == null ? new AiGrowthCoachRefreshRequest() : request));
    }

    @PostMapping("/chat")
    public Result<AiGrowthCoachChatResponse> chat(@Valid @RequestBody AiGrowthCoachChatRequest request) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        return Result.success(aiGrowthCoachService.chat(userId, request));
    }

    @PostMapping("/replan")
    public Result<AiGrowthCoachReplanResponse> replan(@Valid @RequestBody AiGrowthCoachReplanRequest request) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        return Result.success(aiGrowthCoachService.replan(userId, request));
    }

    @GetMapping("/chat/sessions/{sessionId}")
    public Result<AiGrowthCoachChatResponse> session(@PathVariable Long sessionId) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        return Result.success(aiGrowthCoachService.getChatSession(userId, sessionId));
    }

    @PostMapping("/actions/{actionId}/status")
    public Result<Void> updateActionStatus(@PathVariable Long actionId,
                                           @Valid @RequestBody AiGrowthCoachActionStatusRequest request) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        aiGrowthCoachService.updateActionStatus(userId, actionId, request.getStatus());
        return Result.success();
    }
}
