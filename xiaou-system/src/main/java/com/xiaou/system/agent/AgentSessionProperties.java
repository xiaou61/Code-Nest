package com.xiaou.system.agent;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 管理员智能体会话上下文配置。
 *
 * @author xiaou
 */
@Data
@Component
@ConfigurationProperties(prefix = "xiaou.admin-agent.session")
public class AgentSessionProperties {

    /**
     * 会话上下文存储实现：memory / redis / db。
     */
    private String repository = "memory";

    /**
     * 每个会话保留的最近轮数。
     */
    private int maxRecentTurns = 6;

    /**
     * Redis Key 前缀。
     */
    private String redisKeyPrefix = "xiaou:admin:agent:session";

    /**
     * Redis 会话上下文 TTL，单位秒。小于等于 0 表示不设置过期时间。
     */
    private long ttlSeconds = 86400L;
}
