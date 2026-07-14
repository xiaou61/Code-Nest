package com.xiaou.system.agent;

import com.xiaou.system.dto.AgentChatRequest;
import com.xiaou.system.dto.AgentChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 后端会话上下文门面，负责把聊天请求/响应转换为可持久化的会话摘要。
 *
 * @author xiaou
 */
@Component
public class AgentSessionContextStore {

    private final AgentSessionContextRepository repository;
    private final AgentSessionProperties properties;

    @Autowired
    public AgentSessionContextStore(AgentSessionContextRepository repository, AgentSessionProperties properties) {
        this.repository = repository;
        this.properties = properties == null ? new AgentSessionProperties() : properties;
    }

    public AgentSessionContextStore(AgentSessionContextRepository repository) {
        this(repository, new AgentSessionProperties());
    }

    public AgentSessionContextStore() {
        this(new InMemoryAgentSessionContextRepository(), new AgentSessionProperties());
    }

    public AgentSessionSnapshot snapshot(String sessionId) {
        return snapshot(sessionId, normalize(sessionId));
    }

    public AgentSessionSnapshot snapshot(String sessionId, Long operatorId) {
        return snapshot(sessionId, scopedSessionId(sessionId, operatorId));
    }

    private AgentSessionSnapshot snapshot(String sessionId, String repositorySessionId) {
        AgentSessionSnapshot snapshot = new AgentSessionSnapshot();
        String normalizedSessionId = normalize(sessionId);
        snapshot.setSessionId(normalizedSessionId);
        if (!StringUtils.hasText(normalizedSessionId) || !StringUtils.hasText(repositorySessionId)) {
            return snapshot;
        }

        AgentSessionSnapshot storedSnapshot = repository.snapshot(repositorySessionId);
        if (storedSnapshot == null) {
            return snapshot;
        }
        storedSnapshot.setSessionId(normalizedSessionId);
        return storedSnapshot;
    }

    public void record(AgentChatRequest request, AgentChatResponse response) {
        String sessionId = request == null ? "" : normalize(request.getSessionId());
        record(request, response, sessionId);
    }

    public void record(AgentChatRequest request, AgentChatResponse response, Long operatorId) {
        String sessionId = request == null ? "" : request.getSessionId();
        record(request, response, scopedSessionId(sessionId, operatorId));
    }

    private void record(AgentChatRequest request, AgentChatResponse response, String repositorySessionId) {
        String sessionId = request == null ? "" : normalize(request.getSessionId());
        if (!StringUtils.hasText(sessionId) || !StringUtils.hasText(repositorySessionId) || response == null) {
            return;
        }

        AgentSessionTurn turn = new AgentSessionTurn();
        turn.setMessage(request.getMessage());
        turn.setStatus(response.getStatus());
        turn.setAnswer(response.getAnswer());
        turn.setToolName(response.getToolName());
        turn.setAuditId(response.getAuditId());
        turn.setTraceId(response.getTraceId());

        repository.appendTurn(repositorySessionId, turn, maxRecentTurns());
    }

    private int maxRecentTurns() {
        return Math.max(properties.getMaxRecentTurns(), 0);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String scopedSessionId(String sessionId, Long operatorId) {
        String normalizedSessionId = normalize(sessionId);
        if (!StringUtils.hasText(normalizedSessionId)) {
            return "";
        }
        String operatorNamespace = operatorId == null ? "anonymous" : String.valueOf(operatorId);
        return "operator:" + operatorNamespace + ":" + normalizedSessionId;
    }
}
