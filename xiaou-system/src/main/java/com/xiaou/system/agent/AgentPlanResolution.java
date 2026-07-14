package com.xiaou.system.agent;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 智能体计划解析结果，可表达候选工具调用或需要澄清的原因。
 *
 * @author xiaou
 */
@Data
public class AgentPlanResolution {

    private AgentResolvedToolCall resolvedCall;

    private AgentChatErrorCode errorCode;

    private String message;

    private List<String> nextActions = new ArrayList<>();

    public boolean isResolved() {
        return resolvedCall != null;
    }

    public Optional<AgentResolvedToolCall> resolved() {
        return Optional.ofNullable(resolvedCall);
    }

    public static AgentPlanResolution resolved(AgentResolvedToolCall resolvedCall) {
        AgentPlanResolution resolution = new AgentPlanResolution();
        resolution.setResolvedCall(resolvedCall);
        return resolution;
    }

    public static AgentPlanResolution empty() {
        return new AgentPlanResolution();
    }

    public static AgentPlanResolution clarification(String message, List<String> nextActions) {
        AgentPlanResolution resolution = new AgentPlanResolution();
        resolution.setErrorCode(AgentChatErrorCode.PLAN_CLARIFICATION_REQUIRED);
        resolution.setMessage(message);
        if (nextActions != null) {
            resolution.getNextActions().addAll(nextActions);
        }
        return resolution;
    }
}
