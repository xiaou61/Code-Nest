package com.xiaou.system.agent;

/**
 * 管理员智能体会话上下文存储端口，可由内存、Redis 或数据库实现。
 *
 * @author xiaou
 */
public interface AgentSessionContextRepository {

    AgentSessionSnapshot snapshot(String sessionId);

    void appendTurn(String sessionId, AgentSessionTurn turn, int maxRecentTurns);
}
