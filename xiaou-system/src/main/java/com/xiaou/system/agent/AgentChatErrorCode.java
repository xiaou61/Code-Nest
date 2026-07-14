package com.xiaou.system.agent;

/**
 * 管理员智能体统一错误码。
 *
 * @author xiaou
 */
public enum AgentChatErrorCode {

    EMPTY_MESSAGE,
    TOOL_NOT_FOUND,
    PLAN_CLARIFICATION_REQUIRED,
    POLICY_REJECTED,
    PREVIEW_BLOCKED,
    SCHEMA_VALIDATION_FAILED,
    AUDIT_NOT_FOUND,
    AUDIT_OPERATOR_MISMATCH,
    AUDIT_STATUS_NOT_PREVIEW,
    AUDIT_PAYLOAD_INVALID,
    CONFIRMATION_MISMATCH,
    TOOL_EXECUTION_FAILED;

    public String code() {
        return name();
    }
}
