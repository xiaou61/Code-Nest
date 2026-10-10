package io.github.xiaou61.security;

import io.github.xiaou61.platform.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 返回当前令牌对应的主体与角色。
 *
 * <p>没有任何入参：主体只能来自令牌。前端在启动时可用它确认令牌是否仍然有效
 * （access 过期或后端换过签名密钥时这里会返回 401）。
 */
@RestController
class MeController {

    @GetMapping("/api/v1/me")
    ApiResponse<SubjectResponse> me() {
        return ApiResponse.ok(new SubjectResponse(CurrentUser.subjectId(), CurrentUser.role()));
    }

    record SubjectResponse(String subject, String role) {
    }
}
