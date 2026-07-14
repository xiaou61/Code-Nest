package com.xiaou.system.agent;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进程内会话上下文实现。生产可替换为 Redis/DB 实现而不影响运行时主链路。
 *
 * @author xiaou
 */
@Component
@ConditionalOnProperty(prefix = "xiaou.admin-agent.session", name = "repository", havingValue = "memory", matchIfMissing = true)
public class InMemoryAgentSessionContextRepository implements AgentSessionContextRepository {

    private final Map<String, Deque<AgentSessionTurn>> sessions = new ConcurrentHashMap<>();

    @Override
    public AgentSessionSnapshot snapshot(String sessionId) {
        AgentSessionSnapshot snapshot = new AgentSessionSnapshot();
        String normalizedSessionId = normalize(sessionId);
        snapshot.setSessionId(normalizedSessionId);
        if (!StringUtils.hasText(normalizedSessionId)) {
            return snapshot;
        }

        Deque<AgentSessionTurn> turns = sessions.get(normalizedSessionId);
        if (turns == null) {
            return snapshot;
        }
        synchronized (turns) {
            snapshot.setRecentTurns(new ArrayList<>(turns));
        }
        return snapshot;
    }

    @Override
    public void appendTurn(String sessionId, AgentSessionTurn turn, int maxRecentTurns) {
        String normalizedSessionId = normalize(sessionId);
        if (!StringUtils.hasText(normalizedSessionId) || turn == null) {
            return;
        }

        Deque<AgentSessionTurn> turns = sessions.computeIfAbsent(normalizedSessionId, key -> new ArrayDeque<>());
        synchronized (turns) {
            turns.addLast(turn);
            int maxTurns = Math.max(maxRecentTurns, 0);
            while (turns.size() > maxTurns) {
                turns.removeFirst();
            }
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
