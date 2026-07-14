package com.xiaou.system.agent;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理员智能体会话上下文快照。
 *
 * @author xiaou
 */
@Data
public class AgentSessionSnapshot {

    private String sessionId;

    private List<AgentSessionTurn> recentTurns = new ArrayList<>();
}
