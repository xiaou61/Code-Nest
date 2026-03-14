package com.xiaou.learningasset.controller.user;

import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.satoken.StpUserUtil;
import com.xiaou.learningasset.dto.request.LearningAssetCandidateUpdateRequest;
import com.xiaou.learningasset.dto.request.LearningAssetConfirmRequest;
import com.xiaou.learningasset.dto.request.LearningAssetConvertRequest;
import com.xiaou.learningasset.dto.request.LearningAssetPublishRequest;
import com.xiaou.learningasset.dto.request.LearningAssetRecordQueryRequest;
import com.xiaou.learningasset.dto.response.LearningAssetPublishResponse;
import com.xiaou.learningasset.dto.response.LearningAssetRecordDetailResponse;
import com.xiaou.learningasset.dto.response.LearningAssetRecordSummaryResponse;
import com.xiaou.learningasset.service.LearningAssetPublishService;
import com.xiaou.learningasset.service.LearningAssetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户端学习资产控制器
 */
@RestController
@Validated
@RequestMapping("/user/learning-assets")
@RequiredArgsConstructor
public class UserLearningAssetController {

    private final LearningAssetService learningAssetService;
    private final LearningAssetPublishService learningAssetPublishService;

    @PostMapping("/convert")
    public Result<LearningAssetRecordDetailResponse> convert(@Valid @RequestBody LearningAssetConvertRequest request) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        return Result.success(learningAssetService.convert(userId, request));
    }

    @PostMapping("/records/list")
    public Result<PageResult<LearningAssetRecordSummaryResponse>> list(@RequestBody(required = false) LearningAssetRecordQueryRequest request) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        LearningAssetRecordQueryRequest finalRequest = request == null ? new LearningAssetRecordQueryRequest() : request;
        return Result.success(learningAssetService.getRecordList(userId, finalRequest));
    }

    @GetMapping("/records/{id}")
    public Result<LearningAssetRecordDetailResponse> detail(@PathVariable Long id) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        return Result.success(learningAssetService.getRecordDetail(userId, id));
    }

    @PutMapping("/candidates/{id}")
    public Result<Void> updateCandidate(@PathVariable Long id, @Valid @RequestBody LearningAssetCandidateUpdateRequest request) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        learningAssetService.updateCandidate(userId, id, request);
        return Result.success();
    }

    @PostMapping("/records/{id}/confirm")
    public Result<LearningAssetRecordDetailResponse> confirm(@PathVariable Long id, @Valid @RequestBody LearningAssetConfirmRequest request) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        return Result.success(learningAssetService.confirmCandidates(userId, id, request));
    }

    @PostMapping("/candidates/{id}/discard")
    public Result<LearningAssetRecordDetailResponse> discard(@PathVariable Long id) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        return Result.success(learningAssetService.discardCandidate(userId, id));
    }

    @PostMapping("/records/{id}/publish")
    public Result<LearningAssetPublishResponse> publish(@PathVariable Long id, @RequestBody(required = false) LearningAssetPublishRequest request) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        return Result.success(learningAssetPublishService.publish(userId, id, request == null ? null : request.getCandidateIds()));
    }

    @PostMapping("/records/{id}/retry")
    public Result<LearningAssetRecordDetailResponse> retry(@PathVariable Long id) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        return Result.success(learningAssetService.retry(userId, id));
    }
}
