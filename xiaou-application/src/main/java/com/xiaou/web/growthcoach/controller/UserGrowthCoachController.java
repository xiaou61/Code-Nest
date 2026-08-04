package com.xiaou.web.growthcoach.controller;

import com.xiaou.common.core.domain.Result;
import com.xiaou.common.satoken.StpUserUtil;
import com.xiaou.web.growthcoach.dto.GrowthCoachActionRunResponse;
import com.xiaou.web.growthcoach.dto.GrowthApplicationOutcomeResponse;
import com.xiaou.web.growthcoach.dto.GrowthCoachBriefingResponse;
import com.xiaou.web.growthcoach.dto.GrowthCoachConfirmRequest;
import com.xiaou.web.growthcoach.dto.GrowthCodeArtifactAttachRequest;
import com.xiaou.web.growthcoach.dto.GrowthCodeArtifactPreviewResponse;
import com.xiaou.web.growthcoach.dto.GrowthCodeArtifactResponse;
import com.xiaou.web.growthcoach.dto.GrowthCodeReviewRequest;
import com.xiaou.web.growthcoach.dto.GrowthCodeReviewResponse;
import com.xiaou.web.growthcoach.dto.GrowthEvidenceSummaryResponse;
import com.xiaou.web.growthcoach.dto.GrowthEvidenceProfileResponse;
import com.xiaou.web.growthcoach.dto.GrowthGithubAuthorizationResponse;
import com.xiaou.web.growthcoach.dto.GrowthGithubConnectionResponse;
import com.xiaou.web.growthcoach.dto.GrowthJobBattleGapResponse;
import com.xiaou.web.growthcoach.dto.GrowthJobPreparationLoopResponse;
import com.xiaou.web.growthcoach.dto.GrowthJobMarketSignalResponse;
import com.xiaou.web.growthcoach.dto.GrowthJourneyEventRequest;
import com.xiaou.web.growthcoach.dto.GrowthJourneyEventResponse;
import com.xiaou.web.growthcoach.dto.GrowthWeeklyReviewResponse;
import com.xiaou.web.growthcoach.dto.GrowthCoachPreviewRequest;
import com.xiaou.web.growthcoach.dto.GrowthCoachTodayActionResponse;
import com.xiaou.web.growthcoach.dto.GrowthCareerNextActionResponse;
import com.xiaou.web.growthcoach.dto.GrowthSkillInsightResponse;
import com.xiaou.web.growthcoach.service.GrowthCoachApplicationService;
import com.xiaou.web.growthcoach.service.GrowthApplicationOutcomeService;
import com.xiaou.web.growthcoach.service.GrowthCoachBriefingService;
import com.xiaou.web.growthcoach.service.GrowthCodeArtifactService;
import com.xiaou.web.growthcoach.service.GrowthCodeReviewService;
import com.xiaou.web.growthcoach.service.GrowthEvidenceQueryService;
import com.xiaou.web.growthcoach.service.GrowthEvidenceProfileService;
import com.xiaou.web.growthcoach.service.GrowthCareerNextActionService;
import com.xiaou.web.growthcoach.service.GrowthJobBattleGapService;
import com.xiaou.web.growthcoach.service.GrowthJobPreparationLoopService;
import com.xiaou.web.growthcoach.service.GrowthJobMarketSignalService;
import com.xiaou.web.growthcoach.service.GrowthJourneyEventService;
import com.xiaou.web.growthcoach.service.GrowthSkillInsightService;
import com.xiaou.web.growthcoach.service.GrowthWeeklyReviewService;
import com.xiaou.web.growthcoach.service.GrowthGithubConnectionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import org.springframework.web.util.UriComponentsBuilder;

/**
 * 用户侧 AI 成长教练接口。
 */
@Validated
@RestController
@RequestMapping("/user/growth-coach")
@RequiredArgsConstructor
@Slf4j
public class UserGrowthCoachController {

    private final GrowthCoachApplicationService growthCoachApplicationService;
    private final GrowthCoachBriefingService growthCoachBriefingService;
    private final GrowthJourneyEventService growthJourneyEventService;
    private final GrowthCodeArtifactService growthCodeArtifactService;
    private final GrowthCodeReviewService growthCodeReviewService;
    private final GrowthApplicationOutcomeService growthApplicationOutcomeService;
    private final GrowthEvidenceQueryService growthEvidenceQueryService;
    private final GrowthEvidenceProfileService growthEvidenceProfileService;
    private final GrowthCareerNextActionService growthCareerNextActionService;
    private final GrowthSkillInsightService growthSkillInsightService;
    private final GrowthJobBattleGapService growthJobBattleGapService;
    private final GrowthJobPreparationLoopService growthJobPreparationLoopService;
    private final GrowthJobMarketSignalService growthJobMarketSignalService;
    private final GrowthWeeklyReviewService growthWeeklyReviewService;
    private final GrowthGithubConnectionService growthGithubConnectionService;

    @GetMapping("/today-action")
    public Result<GrowthCoachTodayActionResponse> getTodayAction() {
        StpUserUtil.checkLogin();
        return Result.success(growthCoachApplicationService.getTodayAction(StpUserUtil.getLoginIdAsLong()));
    }

    @GetMapping("/github-connection")
    public Result<GrowthGithubConnectionResponse> getGithubConnection() {
        StpUserUtil.checkLogin();
        return Result.success(growthGithubConnectionService.getStatus(StpUserUtil.getLoginIdAsLong()));
    }

    @PostMapping("/github-connection/authorize")
    public Result<GrowthGithubAuthorizationResponse> authorizeGithubConnection() {
        StpUserUtil.checkLogin();
        return Result.success(growthGithubConnectionService.startAuthorization(StpUserUtil.getLoginIdAsLong()));
    }

    @DeleteMapping("/github-connection")
    public Result<Void> unlinkGithubConnection() {
        StpUserUtil.checkLogin();
        growthGithubConnectionService.unlink(StpUserUtil.getLoginIdAsLong());
        return Result.success();
    }

    /**
     * GitHub OAuth 回调只处理固定的成功/失败跳转，不把授权码或错误详情回传给前端。
     */
    @GetMapping("/github-connection/callback")
    public void githubConnectionCallback(
            @RequestParam(name = "state", required = false)
            @Size(max = 128, message = "授权状态无效") String state,
            @RequestParam(name = "code", required = false)
            @Size(max = 2048, message = "授权码无效") String code,
            @RequestParam(name = "error", required = false)
            @Size(max = 128, message = "授权错误无效") String error,
            HttpServletResponse response
    ) throws IOException {
        boolean connected = false;
        Long userId = null;
        try {
            if (StringUtils.hasText(error)) {
                userId = growthGithubConnectionService.rejectAuthorization(state);
            } else {
                userId = growthGithubConnectionService.completeAuthorization(state, code);
                try {
                    growthCodeArtifactService.refreshOwnershipForUser(
                            userId,
                            growthGithubConnectionService.linkedGithubUserId(userId)
                    );
                } catch (RuntimeException refreshException) {
                    log.warn("GitHub 公开来源归属补验延后: {}", refreshException.getClass().getSimpleName());
                }
                connected = true;
            }
        } catch (RuntimeException exception) {
            log.warn("GitHub OAuth 回调未完成: {}", exception.getClass().getSimpleName());
        }
        String redirectUrl = connected
                ? growthGithubConnectionService.successRedirectUrl()
                : growthGithubConnectionService.failureRedirectUrl();
        if (!StringUtils.hasText(redirectUrl)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "GitHub OAuth 回调地址未配置");
            return;
        }
        String target = UriComponentsBuilder.fromUriString(redirectUrl)
                .queryParam("github", connected ? "connected" : "error")
                .build()
                .encode()
                .toUriString();
        response.sendRedirect(target);
    }

    @GetMapping("/code-artifacts")
    public Result<List<GrowthCodeArtifactResponse>> listCodeArtifacts() {
        StpUserUtil.checkLogin();
        return Result.success(growthCodeArtifactService.listForUser(StpUserUtil.getLoginIdAsLong()));
    }

    @PostMapping("/code-artifacts/preview")
    public Result<GrowthCodeArtifactPreviewResponse> previewCodeArtifact(
            @Valid @RequestBody GrowthCodeArtifactAttachRequest request
    ) {
        StpUserUtil.checkLogin();
        return Result.success(growthCodeArtifactService.preview(StpUserUtil.getLoginIdAsLong(), request));
    }

    @PostMapping("/code-artifacts")
    public Result<GrowthCodeArtifactResponse> attachCodeArtifact(
            @Valid @RequestBody GrowthCodeArtifactAttachRequest request
    ) {
        StpUserUtil.checkLogin();
        return Result.success("公开代码来源已附加",
                growthCodeArtifactService.attach(StpUserUtil.getLoginIdAsLong(), request));
    }

    @DeleteMapping("/code-artifacts/{artifactId}")
    public Result<Void> deleteCodeArtifact(
            @Min(value = 1, message = "artifactId不能小于1") @PathVariable Long artifactId
    ) {
        StpUserUtil.checkLogin();
        growthCodeArtifactService.delete(StpUserUtil.getLoginIdAsLong(), artifactId);
        return Result.success();
    }

    @PostMapping("/code-reviews")
    public Result<GrowthCodeReviewResponse> reviewCodePen(
            @Valid @RequestBody GrowthCodeReviewRequest request
    ) {
        StpUserUtil.checkLogin();
        return Result.success("已完成当前保存版本的代码审查",
                growthCodeReviewService.review(StpUserUtil.getLoginIdAsLong(), request.getPenId()));
    }

    @GetMapping("/code-reviews/latest")
    public Result<GrowthCodeReviewResponse> getLatestCodeReview(
            @RequestParam @Min(value = 1, message = "penId不能小于1") Long penId
    ) {
        StpUserUtil.checkLogin();
        return Result.success(growthCodeReviewService.getLatest(StpUserUtil.getLoginIdAsLong(), penId));
    }

    @DeleteMapping("/code-reviews/{reviewId}")
    public Result<Void> deleteCodeReview(
            @Min(value = 1, message = "reviewId不能小于1") @PathVariable Long reviewId
    ) {
        StpUserUtil.checkLogin();
        growthCodeReviewService.delete(StpUserUtil.getLoginIdAsLong(), reviewId);
        return Result.success();
    }

    @GetMapping("/briefing")
    public Result<GrowthCoachBriefingResponse> getBriefing() {
        StpUserUtil.checkLogin();
        return Result.success(growthCoachBriefingService.getForUser(StpUserUtil.getLoginIdAsLong()));
    }

    @PostMapping("/journey-events")
    public Result<GrowthJourneyEventResponse> recordJourneyEvent(
            @Valid @RequestBody GrowthJourneyEventRequest request
    ) {
        StpUserUtil.checkLogin();
        return Result.success(growthJourneyEventService.record(StpUserUtil.getLoginIdAsLong(), request));
    }

    @GetMapping("/evidence")
    public Result<List<GrowthEvidenceSummaryResponse>> listEvidence(
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "limit不能小于1")
            @Max(value = 30, message = "limit不能大于30") Integer limit
    ) {
        StpUserUtil.checkLogin();
        return Result.success(growthEvidenceQueryService.listForUser(StpUserUtil.getLoginIdAsLong(), limit));
    }

    @GetMapping("/evidence-profile")
    public Result<GrowthEvidenceProfileResponse> getEvidenceProfile() {
        StpUserUtil.checkLogin();
        return Result.success(growthEvidenceProfileService.getForUser(StpUserUtil.getLoginIdAsLong()));
    }

    @GetMapping("/skill-insights")
    public Result<List<GrowthSkillInsightResponse>> listSkillInsights() {
        StpUserUtil.checkLogin();
        return Result.success(growthSkillInsightService.listForUser(StpUserUtil.getLoginIdAsLong()));
    }

    @GetMapping("/career-next-action")
    public Result<GrowthCareerNextActionResponse> getCareerNextAction() {
        StpUserUtil.checkLogin();
        return Result.success(growthCareerNextActionService.getNextAction(StpUserUtil.getLoginIdAsLong()));
    }

    @GetMapping("/application-outcomes")
    public Result<GrowthApplicationOutcomeResponse> getApplicationOutcomes() {
        StpUserUtil.checkLogin();
        return Result.success(growthApplicationOutcomeService.getForUser(StpUserUtil.getLoginIdAsLong()));
    }

    @GetMapping("/job-battle-gap")
    public Result<GrowthJobBattleGapResponse> getJobBattleGap() {
        StpUserUtil.checkLogin();
        return Result.success(growthJobBattleGapService.getCurrentGap(StpUserUtil.getLoginIdAsLong()));
    }

    @GetMapping("/job-preparation-loop")
    public Result<GrowthJobPreparationLoopResponse> getJobPreparationLoop() {
        StpUserUtil.checkLogin();
        return Result.success(growthJobPreparationLoopService.getForUser(StpUserUtil.getLoginIdAsLong()));
    }

    @GetMapping("/job-market-signal")
    public Result<GrowthJobMarketSignalResponse> getJobMarketSignal() {
        StpUserUtil.checkLogin();
        return Result.success(growthJobMarketSignalService.getCurrentSignal(StpUserUtil.getLoginIdAsLong()));
    }

    @GetMapping("/weekly-review")
    public Result<GrowthWeeklyReviewResponse> getWeeklyReview() {
        StpUserUtil.checkLogin();
        return Result.success(growthWeeklyReviewService.getCurrentReview(StpUserUtil.getLoginIdAsLong()));
    }

    @PostMapping("/career-next-action/{actionId}/complete")
    public Result<Void> completeCareerNextAction(
            @Min(value = 1, message = "actionId不能小于1") @PathVariable Long actionId
    ) {
        StpUserUtil.checkLogin();
        growthCareerNextActionService.markDone(StpUserUtil.getLoginIdAsLong(), actionId);
        return Result.success();
    }

    @PostMapping("/plan-adjustments/preview")
    public Result<GrowthCoachActionRunResponse> preview(@Valid @RequestBody GrowthCoachPreviewRequest request) {
        StpUserUtil.checkLogin();
        return Result.success("计划变更预览已生成",
                growthCoachApplicationService.preview(StpUserUtil.getLoginIdAsLong(), request));
    }

    @PostMapping("/plan-adjustments/{runId}/confirm")
    public Result<GrowthCoachActionRunResponse> confirm(
            @NotBlank(message = "runId不能为空") @PathVariable String runId,
            @Valid @RequestBody GrowthCoachConfirmRequest request
    ) {
        StpUserUtil.checkLogin();
        return Result.success("计划调整已处理",
                growthCoachApplicationService.confirm(StpUserUtil.getLoginIdAsLong(), runId, request));
    }

    @PostMapping("/plan-adjustments/{runId}/cancel")
    public Result<GrowthCoachActionRunResponse> cancel(
            @NotBlank(message = "runId不能为空") @PathVariable String runId
    ) {
        StpUserUtil.checkLogin();
        return Result.success("计划预览已取消",
                growthCoachApplicationService.cancel(StpUserUtil.getLoginIdAsLong(), runId));
    }

    @GetMapping("/plan-adjustments/{runId}")
    public Result<GrowthCoachActionRunResponse> getRun(
            @NotBlank(message = "runId不能为空") @PathVariable String runId
    ) {
        StpUserUtil.checkLogin();
        return Result.success(growthCoachApplicationService.getRun(StpUserUtil.getLoginIdAsLong(), runId));
    }
}
