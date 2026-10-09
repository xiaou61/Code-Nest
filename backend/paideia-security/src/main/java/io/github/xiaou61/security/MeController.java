package io.github.xiaou61.security;

import io.github.xiaou61.platform.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 返回当前令牌对应的主体。
 *
 * <p>没有任何入参：主体只能来自令牌。这是骨架期验证认证链路的技术端点，
 * 也是前端联调的目标。
 */
@RestController
class MeController {

    @GetMapping("/api/v1/me")
    ApiResponse<SubjectResponse> me() {
        return ApiResponse.ok(new SubjectResponse(CurrentUser.subjectId()));
    }

    record SubjectResponse(String subject) {
    }
}
