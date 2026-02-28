package com.xiaou.mockinterview.controller;

import com.xiaou.ai.dto.jobbattle.JobBattleInterviewReviewResult;
import com.xiaou.ai.dto.jobbattle.JobBattleJdParseResult;
import com.xiaou.ai.dto.jobbattle.JobBattlePlanResult;
import com.xiaou.ai.dto.jobbattle.JobBattleResumeMatchResult;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.satoken.StpUserUtil;
import com.xiaou.mockinterview.domain.JobBattlePlanRecord;
import com.xiaou.mockinterview.dto.request.JobBattleGeneratePlanRequest;
import com.xiaou.mockinterview.dto.request.JobBattleInterviewReviewRequest;
import com.xiaou.mockinterview.dto.request.JobBattlePlanHistoryRequest;
import com.xiaou.mockinterview.dto.request.JobBattleParseJdRequest;
import com.xiaou.mockinterview.dto.request.JobBattleResumeMatchRequest;
import com.xiaou.mockinterview.service.JobBattleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 求职作战台控制器
 *
 * @author xiaou
 */
@Tag(name = "求职作战台-用户端", description = "求职作战台增强版接口")
@RestController
@RequestMapping("/user/job-battle")
@Validated
@RequiredArgsConstructor
public class JobBattleController {

    private final JobBattleService jobBattleService;

    @Operation(summary = "JD解析")
    @PostMapping("/jd/parse")
    public Result<JobBattleJdParseResult> parseJd(@Valid @RequestBody JobBattleParseJdRequest request) {
        JobBattleJdParseResult result = jobBattleService.parseJd(request);
        return Result.success("JD解析完成", result);
    }

    @Operation(summary = "简历匹配评估")
    @PostMapping("/resume/match")
    public Result<JobBattleResumeMatchResult> matchResume(@Valid @RequestBody JobBattleResumeMatchRequest request) {
        JobBattleResumeMatchResult result = jobBattleService.matchResume(request);
        return Result.success("简历匹配完成", result);
    }

    @Operation(summary = "生成补短板计划")
    @PostMapping("/plan/generate")
    public Result<JobBattlePlanResult> generatePlan(@Valid @RequestBody JobBattleGeneratePlanRequest request) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        JobBattlePlanResult result = jobBattleService.generatePlan(userId, request);
        return Result.success("行动计划生成完成", result);
    }

    @Operation(summary = "获取计划历史")
    @PostMapping("/plan/history")
    public Result<PageResult<JobBattlePlanRecord>> getPlanHistory(@RequestBody(required = false) JobBattlePlanHistoryRequest request) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        if (request == null) {
            request = new JobBattlePlanHistoryRequest();
        }
        PageResult<JobBattlePlanRecord> result = jobBattleService.getPlanHistory(userId, request);
        return Result.success(result);
    }

    @Operation(summary = "获取计划历史详情")
    @GetMapping("/plan/history/{id}")
    public Result<JobBattlePlanRecord> getPlanHistoryDetail(@PathVariable Long id) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        JobBattlePlanRecord result = jobBattleService.getPlanHistoryDetail(userId, id);
        return Result.success(result);
    }

    @Operation(summary = "面试复盘总结")
    @PostMapping("/interview/review")
    public Result<JobBattleInterviewReviewResult> reviewInterview(@Valid @RequestBody JobBattleInterviewReviewRequest request) {
        JobBattleInterviewReviewResult result = jobBattleService.reviewInterview(request);
        return Result.success("面试复盘完成", result);
    }
}
