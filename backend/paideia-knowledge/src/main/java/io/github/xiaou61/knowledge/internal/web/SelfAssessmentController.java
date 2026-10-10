package io.github.xiaou61.knowledge.internal.web;

import io.github.xiaou61.knowledge.internal.assessment.SelfAssessmentService;
import io.github.xiaou61.platform.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学习者自评端点——**全项目唯一一处学习者可写的数据**。
 *
 * <p>它落在 {@code /api/v1/knowledge/**} 之下（不在 {@code /admin/} 里），因此只需"已登录"，
 * 由 {@code anyRequest().authenticated()} 兜住。**权限靠数据过滤而不是靠路径**：
 * 路径与请求体里都没有用户标识，归属一律取自令牌，并且每条查询都带 {@code user_id} 条件。
 */
@RestController
@RequestMapping("/api/v1/knowledge")
class SelfAssessmentController {

    private final SelfAssessmentService selfAssessmentService;

    SelfAssessmentController(SelfAssessmentService selfAssessmentService) {
        this.selfAssessmentService = selfAssessmentService;
    }

    @PutMapping("/entries/{id}/self-assessment")
    ApiResponse<Void> mark(@PathVariable("id") long id, @Valid @RequestBody MarkRequest body) {
        selfAssessmentService.mark(CurrentUserId.require(), id, body.level());
        return ApiResponse.ok(null);
    }

    /** 取消标记。幂等：没有标记也返回成功，用户点两下不该看到报错。 */
    @DeleteMapping("/entries/{id}/self-assessment")
    ApiResponse<Void> clear(@PathVariable("id") long id) {
        selfAssessmentService.clear(CurrentUserId.require(), id);
        return ApiResponse.ok(null);
    }

    /** 我标记过的条目。**只可能是自己的**。 */
    @GetMapping("/self-assessments")
    ApiResponse<List<SelfAssessmentService.AssessedItem>> mine(
            @RequestParam(name = "level", required = false) String level) {
        return ApiResponse.ok(selfAssessmentService.listMine(CurrentUserId.require(), level));
    }

    record MarkRequest(@NotBlank @Size(max = 16) String level) {
    }
}
