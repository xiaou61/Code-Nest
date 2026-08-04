package com.xiaou.mockinterview.controller;

import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.satoken.StpUserUtil;
import com.xiaou.interview.domain.InterviewQuestionSet;
import com.xiaou.mockinterview.domain.MockInterviewDirection;
import com.xiaou.mockinterview.domain.MockInterviewSession;
import com.xiaou.mockinterview.dto.request.CreateInterviewRequest;
import com.xiaou.mockinterview.dto.request.InterviewHistoryRequest;
import com.xiaou.mockinterview.dto.response.InterviewConfigResponse;
import com.xiaou.mockinterview.dto.response.InterviewReportResponse;
import com.xiaou.mockinterview.dto.response.InterviewSessionResponse;
import com.xiaou.mockinterview.dto.response.InterviewStatsResponse;
import com.xiaou.mockinterview.service.MockInterviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模拟面试用户端控制器
 *
 * @author xiaou
 */
@Tag(name = "模拟面试-用户端", description = "用户端模拟面试相关接口")
@RestController
@RequestMapping("/user/mock-interview")
@RequiredArgsConstructor
public class MockInterviewController {

    private final MockInterviewService mockInterviewService;

    @Operation(summary = "获取面试方向列表")
    @GetMapping("/directions")
    public Result<List<MockInterviewDirection>> getDirections() {
        List<MockInterviewDirection> directions = mockInterviewService.getDirections();
        return Result.success(directions);
    }

    @Operation(summary = "获取面试配置选项")
    @GetMapping("/config")
    public Result<InterviewConfigResponse> getConfig() {
        InterviewConfigResponse config = mockInterviewService.getConfig();
        return Result.success(config);
    }

    @Operation(summary = "获取可用题库列表")
    @GetMapping("/question-sets")
    public Result<List<InterviewQuestionSet>> getQuestionSets(@RequestParam(required = false) String direction) {
        List<InterviewQuestionSet> questionSets = mockInterviewService.getQuestionSets(direction);
        return Result.success(questionSets);
    }

    @Operation(summary = "创建面试会话")
    @PostMapping("/create")
    public Result<InterviewSessionResponse> createInterview(@Valid @RequestBody CreateInterviewRequest request) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        InterviewSessionResponse response = mockInterviewService.createInterview(userId, request);
        return Result.success("创建成功", response);
    }

    @Operation(summary = "获取面试历史记录")
    @PostMapping("/history")
    public Result<PageResult<MockInterviewSession>> getHistory(@RequestBody(required = false) InterviewHistoryRequest request) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        if (request == null) {
            request = new InterviewHistoryRequest();
        }
        PageResult<MockInterviewSession> result = mockInterviewService.getHistory(userId, request);
        return Result.success(result);
    }

    @Operation(summary = "获取面试报告")
    @GetMapping("/{id}/report")
    public Result<InterviewReportResponse> getReport(@PathVariable Long id) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        InterviewReportResponse report = mockInterviewService.getReport(userId, id);
        return Result.success(report);
    }

    @Operation(summary = "删除面试记录")
    @DeleteMapping("/{id}")
    public Result<Void> deleteInterview(@PathVariable Long id) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        mockInterviewService.deleteInterview(userId, id);
        return Result.success();
    }

    @Operation(summary = "获取统计总览")
    @GetMapping("/stats/overview")
    public Result<InterviewStatsResponse> getStatsOverview() {
        Long userId = StpUserUtil.getLoginIdAsLong();
        InterviewStatsResponse stats = mockInterviewService.getStats(userId);
        return Result.success(stats);
    }

    @Operation(summary = "生成面试总结")
    @PostMapping("/{id}/summary")
    public Result<InterviewReportResponse> generateSummary(@PathVariable Long id) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        InterviewReportResponse report = mockInterviewService.generateSummary(userId, id);
        return Result.success("生成成功", report);
    }
}
