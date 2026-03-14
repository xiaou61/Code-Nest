package com.xiaou.learningasset.controller.admin;

import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.satoken.StpAdminUtil;
import com.xiaou.learningasset.dto.request.LearningAssetAdminCandidateUpdateRequest;
import com.xiaou.learningasset.dto.request.LearningAssetApproveRequest;
import com.xiaou.learningasset.dto.request.LearningAssetMergeRequest;
import com.xiaou.learningasset.dto.request.LearningAssetRejectRequest;
import com.xiaou.learningasset.dto.request.LearningAssetReviewQueryRequest;
import com.xiaou.learningasset.dto.response.LearningAssetReviewCandidateResponse;
import com.xiaou.learningasset.dto.response.LearningAssetStatisticsResponse;
import com.xiaou.learningasset.service.LearningAssetPublishService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端学习资产审核控制器
 */
@RestController
@Validated
@RequestMapping("/admin/learning-assets")
@RequiredArgsConstructor
public class AdminLearningAssetController {

    private final LearningAssetPublishService learningAssetPublishService;

    @PostMapping("/candidates/list")
    @RequireAdmin
    public Result<PageResult<LearningAssetReviewCandidateResponse>> list(@RequestBody(required = false) LearningAssetReviewQueryRequest request) {
        LearningAssetReviewQueryRequest finalRequest = request == null ? new LearningAssetReviewQueryRequest() : request;
        return Result.success(learningAssetPublishService.getReviewList(finalRequest));
    }

    @GetMapping("/candidates/{id}")
    @RequireAdmin
    public Result<LearningAssetReviewCandidateResponse> detail(@PathVariable Long id) {
        return Result.success(learningAssetPublishService.getReviewDetail(id));
    }

    @PutMapping("/candidates/{id}")
    @RequireAdmin
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody LearningAssetAdminCandidateUpdateRequest request) {
        Long adminId = StpAdminUtil.getLoginIdAsLong();
        learningAssetPublishService.updateReviewCandidate(adminId, id, request);
        return Result.success();
    }

    @PostMapping("/candidates/{id}/approve")
    @RequireAdmin
    public Result<Long> approve(@PathVariable Long id, @Valid @RequestBody LearningAssetApproveRequest request) {
        Long adminId = StpAdminUtil.getLoginIdAsLong();
        return Result.success(learningAssetPublishService.approve(adminId, id, request));
    }

    @PostMapping("/candidates/{id}/merge")
    @RequireAdmin
    public Result<Long> merge(@PathVariable Long id, @Valid @RequestBody LearningAssetMergeRequest request) {
        Long adminId = StpAdminUtil.getLoginIdAsLong();
        return Result.success(learningAssetPublishService.merge(adminId, id, request));
    }

    @PostMapping("/candidates/{id}/reject")
    @RequireAdmin
    public Result<Void> reject(@PathVariable Long id, @Valid @RequestBody LearningAssetRejectRequest request) {
        Long adminId = StpAdminUtil.getLoginIdAsLong();
        learningAssetPublishService.reject(adminId, id, request.getNote());
        return Result.success();
    }

    @GetMapping("/statistics")
    @RequireAdmin
    public Result<LearningAssetStatisticsResponse> statistics() {
        return Result.success(learningAssetPublishService.getStatistics());
    }
}
