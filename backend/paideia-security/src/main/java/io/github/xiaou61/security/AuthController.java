package io.github.xiaou61.security;

import io.github.xiaou61.platform.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 骨架期的令牌签发端点。
 *
 * <p><strong>安全边界</strong>：这个端点不校验任何凭据，可以为任意主体签发令牌。它只在
 * {@code dev} 与 {@code local} profile 下存在，用于验证认证链路与端到端测试。
 * 引入真实登录之前，**绝不能让它出现在任何面向他人的环境里**。
 */
@RestController
@RequestMapping("/api/v1/auth")
@Profile({"dev", "local"})
class AuthController {

    private final AuthPort authPort;

    AuthController(AuthPort authPort) {
        this.authPort = authPort;
    }

    @PostMapping("/token")
    ApiResponse<TokenResponse> issue(@Valid @RequestBody TokenRequest request) {
        AuthPort.Token token = authPort.issue(request.subject());
        // 时间以 ISO-8601 字符串输出，不依赖 Jackson 的日期序列化配置
        return ApiResponse.ok(new TokenResponse(token.value(), token.expiresAt().toString()));
    }

    record TokenRequest(
            @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9._@-]+") String subject) {
    }

    record TokenResponse(String token, String expiresAt) {
    }
}
