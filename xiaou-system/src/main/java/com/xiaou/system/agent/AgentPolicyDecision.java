package com.xiaou.system.agent;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 智能体策略决策。
 *
 * @author xiaou
 */
@Data
public class AgentPolicyDecision {

    private boolean allowed;

    private boolean confirmationRequired;

    private String confirmationText;

    private AgentChatErrorCode errorCode;

    private String rejectionReason;

    private List<String> nextActions = new ArrayList<>();

    public static AgentPolicyDecision allowReadonly() {
        AgentPolicyDecision decision = new AgentPolicyDecision();
        decision.setAllowed(true);
        decision.setConfirmationRequired(false);
        return decision;
    }

    public static AgentPolicyDecision requireConfirmation(String confirmationText) {
        AgentPolicyDecision decision = new AgentPolicyDecision();
        decision.setAllowed(true);
        decision.setConfirmationRequired(true);
        decision.setConfirmationText(confirmationText);
        decision.getNextActions().add("确认无误后，按要求输入强确认文本。");
        return decision;
    }

    public static AgentPolicyDecision reject(String reason) {
        return reject(AgentChatErrorCode.POLICY_REJECTED, reason);
    }

    public static AgentPolicyDecision reject(AgentChatErrorCode errorCode, String reason) {
        AgentPolicyDecision decision = new AgentPolicyDecision();
        decision.setAllowed(false);
        decision.setErrorCode(errorCode);
        decision.setRejectionReason(reason);
        decision.getNextActions().add("换一种更明确的管理员请求，或先查询当前状态。");
        return decision;
    }
}
