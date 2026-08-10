package com.xiaou.system.controller;

import com.xiaou.common.annotation.Log;
import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.satoken.StpAdminUtil;
import com.xiaou.system.agent.AgentChatOrchestrator;
import com.xiaou.system.agent.AgentOperatorResolver;
import com.xiaou.system.dto.AgentChatRequest;
import com.xiaou.system.dto.AgentChatResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员智能体统一聊天入口。
 *
 * @author xiaou
 */
@Validated
@RestController
@RequestMapping("/admin/agent")
@RequiredArgsConstructor
@Tag(name = "管理员智能体聊天", description = "通过单一聊天接口完成问答、预览、确认、执行和审计")
public class AgentChatController {

    private final AgentChatOrchestrator agentChatOrchestrator;
    private final AgentOperatorResolver operatorResolver;

    @Operation(summary = "管理员智能体统一聊天")
    @SecurityRequirement(name = "Bearer Token")
    @RequireAdmin(message = "使用管理员智能体需要管理员权限")
    @PostMapping("/chat")
    @Log(module = "系统管理", type = Log.OperationType.OTHER, description = "管理员智能体聊天",
            saveRequestData = false, saveResponseData = false)
    public Result<AgentChatResponse> chat(@Valid @RequestBody AgentChatRequest request) {
        Long adminId = StpAdminUtil.getLoginIdAsLong();
        return Result.success("智能体响应完成",
                agentChatOrchestrator.chat(request, operatorResolver.resolve(adminId)));
    }
}
