package com.xiaou.oj.controller.pub;

import com.xiaou.common.satoken.StpUserUtil;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.core.domain.Result;
import com.xiaou.oj.domain.OjSubmission;
import com.xiaou.oj.dto.OjStatisticsVO;
import com.xiaou.oj.dto.SelfTestRequest;
import com.xiaou.oj.dto.SelfTestResult;
import com.xiaou.oj.dto.SubmissionQueryRequest;
import com.xiaou.oj.dto.SubmitCodeRequest;
import com.xiaou.oj.judge.CodeRunnerService;
import com.xiaou.oj.service.OjSubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * OJ提交接口 (用户端)
 *
 * @author xiaou
 */
@Tag(name = "OJ提交", description = "OJ代码提交和查询功能")
@RestController
@RequestMapping("/oj")
@RequiredArgsConstructor
public class OjSubmissionController {

    private final OjSubmissionService submissionService;
    private final CodeRunnerService codeRunnerService;

    @Operation(summary = "自由运行代码")
    @PostMapping("/run")
    public Result<CodeRunnerService.RunResult> runCode(@Valid @RequestBody CodeRunnerService.RunRequest request) {
        return Result.success(codeRunnerService.run(request));
    }

    @Operation(summary = "自测模式")
    @PostMapping("/test")
    public Result<SelfTestResult> selfTest(@Valid @RequestBody SelfTestRequest request) {
        return Result.success(codeRunnerService.selfTest(request));
    }

    @Operation(summary = "提交代码")
    @PostMapping("/submit")
    public Result<Long> submitCode(@Valid @RequestBody SubmitCodeRequest request) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        Long submissionId = submissionService.submitCode(userId, request);
        return Result.success(submissionId);
    }

    @Operation(summary = "查看提交详情")
    @GetMapping("/submissions/{id}")
    public Result<OjSubmission> getSubmission(@PathVariable Long id) {
        return Result.success(submissionService.getSubmissionById(id));
    }

    @Operation(summary = "我的提交记录")
    @PostMapping("/submissions/my")
    public Result<PageResult<OjSubmission>> mySubmissions(@RequestBody SubmissionQueryRequest request) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        request.setUserId(userId);
        return Result.success(submissionService.getSubmissions(request));
    }

    @Operation(summary = "某题的提交记录")
    @GetMapping("/problems/{problemId}/submissions")
    public Result<PageResult<OjSubmission>> problemSubmissions(@PathVariable Long problemId,
                                                               @RequestParam(defaultValue = "1") Integer pageNum,
                                                               @RequestParam(defaultValue = "20") Integer pageSize) {
        SubmissionQueryRequest request = new SubmissionQueryRequest();
        request.setProblemId(problemId);
        request.setPageNum(pageNum);
        request.setPageSize(pageSize);
        return Result.success(submissionService.getSubmissions(request));
    }

    @Operation(summary = "个人做题统计")
    @GetMapping("/statistics/me")
    public Result<OjStatisticsVO> getStatistics() {
        Long userId = StpUserUtil.getLoginIdAsLong();
        return Result.success(submissionService.getStatistics(userId));
    }
}
