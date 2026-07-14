package com.xiaou.system.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.system.domain.SysAgentSessionContext;
import com.xiaou.system.mapper.SysAgentSessionContextMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库会话上下文实现。用于需要跨实例持久化最近对话窗口的部署。
 *
 * @author xiaou
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "xiaou.admin-agent.session", name = "repository", havingValue = "db")
public class DbAgentSessionContextRepository implements AgentSessionContextRepository {

    private static final TypeReference<List<AgentSessionTurn>> TURN_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final SysAgentSessionContextMapper sessionContextMapper;

    @Override
    public synchronized AgentSessionSnapshot snapshot(String sessionId) {
        AgentSessionSnapshot snapshot = new AgentSessionSnapshot();
        String normalizedSessionId = normalize(sessionId);
        snapshot.setSessionId(normalizedSessionId);
        if (!StringUtils.hasText(normalizedSessionId)) {
            return snapshot;
        }

        SysAgentSessionContext record = sessionContextMapper.selectBySessionId(normalizedSessionId);
        if (record == null || !StringUtils.hasText(record.getTurnsJson())) {
            return snapshot;
        }

        try {
            snapshot.setRecentTurns(objectMapper.readValue(record.getTurnsJson(), TURN_LIST_TYPE));
            return snapshot;
        } catch (JsonProcessingException e) {
            log.warn("读取管理员智能体 DB 会话上下文失败 sessionId={}, message={}", normalizedSessionId, e.getMessage());
            return snapshot;
        }
    }

    @Override
    public synchronized void appendTurn(String sessionId, AgentSessionTurn turn, int maxRecentTurns) {
        String normalizedSessionId = normalize(sessionId);
        if (!StringUtils.hasText(normalizedSessionId) || turn == null) {
            return;
        }

        List<AgentSessionTurn> turns = new ArrayList<>(snapshot(normalizedSessionId).getRecentTurns());
        turns.add(turn);
        turns = limitRecentTurns(turns, maxRecentTurns);

        SysAgentSessionContext record = new SysAgentSessionContext();
        record.setSessionId(normalizedSessionId);
        try {
            record.setTurnsJson(objectMapper.writeValueAsString(turns));
        } catch (JsonProcessingException e) {
            log.warn("序列化管理员智能体 DB 会话上下文失败 sessionId={}, message={}", normalizedSessionId, e.getMessage());
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        record.setCreatedTime(now);
        record.setUpdatedTime(now);
        sessionContextMapper.upsert(record);
    }

    private List<AgentSessionTurn> limitRecentTurns(List<AgentSessionTurn> turns, int maxRecentTurns) {
        int maxTurns = Math.max(maxRecentTurns, 0);
        if (turns.size() <= maxTurns) {
            return turns;
        }
        return new ArrayList<>(turns.subList(turns.size() - maxTurns, turns.size()));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
