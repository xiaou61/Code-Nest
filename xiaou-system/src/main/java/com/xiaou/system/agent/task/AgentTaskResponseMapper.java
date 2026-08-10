package com.xiaou.system.agent.task;

import com.xiaou.system.domain.SysAgentTask;
import com.xiaou.system.domain.SysAgentTaskStep;
import com.xiaou.system.dto.AgentTaskConfirmationResponse;
import com.xiaou.system.dto.AgentTaskResponse;
import com.xiaou.system.dto.AgentTaskStepResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;

/**
 * Converts persistence records into the bounded owner-facing task contract.
 */
@Component
public class AgentTaskResponseMapper {

    private final AgentTaskProperties properties;

    public AgentTaskResponseMapper(AgentTaskProperties properties) {
        this.properties = properties;
    }

    public AgentTaskResponse toResponse(SysAgentTask task, List<SysAgentTaskStep> steps) {
        if (task == null) {
            return null;
        }
        List<SysAgentTaskStep> safeSteps = steps == null ? List.of() : steps.stream()
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(SysAgentTaskStep::getStepOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        AgentTaskResponse response = new AgentTaskResponse();
        response.setTaskId(task.getTaskId());
        response.setGoal(task.getGoal());
        response.setSessionId(task.getSessionId());
        response.setStatus(task.getStatus());
        response.setMaxSteps(task.getMaxSteps());
        response.setCompletedSteps(task.getCompletedSteps());
        response.setCurrentStepOrder(task.getCurrentStepOrder());
        response.setTerminalCode(task.getTerminalCode());
        response.setTerminalReason(task.getTerminalReason());
        response.setCancelReason(task.getCancelReason());
        response.setCancelledAt(task.getCancelledAt());
        response.setCompletedAt(task.getCompletedAt());
        response.setCreatedTime(task.getCreatedTime());
        response.setUpdatedTime(task.getUpdatedTime());
        response.setSteps(safeSteps.stream().map(this::toStepResponse).toList());

        if (AgentTaskStatus.WAITING_CONFIRMATION.name().equals(task.getStatus())) {
            safeSteps.stream()
                    .filter(step -> AgentTaskStepStatus.WAITING_CONFIRMATION.name().equals(step.getStatus()))
                    .filter(step -> StringUtils.hasText(step.getAuditId()))
                    .findFirst()
                    .ifPresent(step -> response.setConfirmation(toConfirmation(step)));
        }
        return response;
    }

    private AgentTaskStepResponse toStepResponse(SysAgentTaskStep step) {
        AgentTaskStepResponse response = new AgentTaskStepResponse();
        response.setStepOrder(step.getStepOrder());
        response.setToolName(step.getToolName());
        response.setInputSummary(limit(step.getInputSummary(), 500));
        response.setRiskLevel(step.getRiskLevel());
        response.setRiskCategory(step.getRiskCategory());
        response.setStatus(step.getStatus());
        response.setAuditId(step.getAuditId());
        response.setTraceId(step.getTraceId());
        response.setResultSummary(limit(step.getResultSummary(), properties.normalizedMaxSummaryChars()));
        response.setResultJson(limit(step.getResultJson(), properties.normalizedMaxResultJsonChars()));
        response.setErrorMessage(limit(step.getErrorMessage(), 1000));
        response.setStartedAt(step.getStartedAt());
        response.setCompletedAt(step.getCompletedAt());
        return response;
    }

    private AgentTaskConfirmationResponse toConfirmation(SysAgentTaskStep step) {
        AgentTaskConfirmationResponse response = new AgentTaskConfirmationResponse();
        response.setAuditId(step.getAuditId());
        response.setRequiredText(step.getConfirmationText());
        response.setPrompt(StringUtils.hasText(step.getConfirmationText())
                ? "这是写入/破坏性动作。确认无误后请输入：" + step.getConfirmationText()
                : "这是写入/破坏性动作，需要管理员确认。"
        );
        return response;
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
