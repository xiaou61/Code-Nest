package com.xiaou.ai.prompt;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Prompt 渲染测试样例。
 *
 * @author xiaou
 */
public final class AiPromptFixtures {

    private static final Map<String, Object> DEFAULT_VALUES = buildDefaultValues();

    private AiPromptFixtures() {
    }

    public static Map<String, Object> variables(AiPromptSpec spec) {
        Map<String, Object> variables = new LinkedHashMap<>();
        for (String variable : spec.templateVariables()) {
            variables.put(variable, DEFAULT_VALUES.getOrDefault(variable, "sample-" + variable));
        }
        return variables;
    }

    public static Map<String, Object> ragQueryVariables(AiRagQuerySpec spec) {
        Map<String, Object> variables = new LinkedHashMap<>();
        for (String variable : spec.templateVariables()) {
            variables.put(variable, DEFAULT_VALUES.getOrDefault(variable, "sample-" + variable));
        }
        return variables;
    }

    private static Map<String, Object> buildDefaultValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("message", "查询智能体工具目录");
        values.put("toolsJson", """
                [{"name":"system.agent.tools.list","inputSchema":{},"requiredInputKeys":[]}]
                """);
        values.put("sessionContextJson", """
                {"sessionId":"session-1","recentTurns":[]}
                """);
        values.put("title", "Redis 缓存一致性复盘");
        values.put("content", "本文复盘缓存更新、延迟双删和补偿任务的取舍。");
        values.put("question", "请讲一下缓存一致性方案。");
        values.put("answer", "可以从旁路缓存、延迟双删和消息最终一致性展开。");
        values.put("context", "候选人正在模拟 Java 后端面试。");
        values.put("profile", "5 年 Java 后端开发，熟悉 Redis 和 MySQL。");
        values.put("jobDescription", "负责交易链路稳定性和高并发系统建设。");
        values.put("resume", "参与订单系统重构，优化接口耗时和缓存命中率。");
        values.put("target", "两周内提升系统设计表达。");
        values.put("review", "系统设计表达缺少容量估算。");
        values.put("sql", "SELECT * FROM orders WHERE user_id = 1 ORDER BY create_time DESC LIMIT 20");
        values.put("ddl", "CREATE TABLE orders(id BIGINT PRIMARY KEY, user_id BIGINT, create_time DATETIME)");
        values.put("explain", "type=ALL, rows=500000, Extra=Using filesort");
        values.put("explainResult", "type=ALL, rows=500000, Extra=Using filesort");
        values.put("originalSql", "SELECT * FROM orders WHERE user_id = 1");
        values.put("optimizedSql", "SELECT id FROM orders WHERE user_id = 1");
        values.put("diagnoseJson", "{\"problem\":\"FULL_TABLE_SCAN\"}");
        values.put("direction", "Java 后端");
        values.put("level", "中级");
        values.put("count", 5);
        values.put("questionCount", 5);
        values.put("answeredCount", 4);
        values.put("skippedCount", 1);
        values.put("totalScore", 32);
        values.put("style", "追问式");
        values.put("followUpCount", 1);
        values.put("targetRole", "Java 后端开发");
        values.put("targetLevel", "P6");
        values.put("city", "杭州");
        values.put("jdText", "负责高并发交易链路稳定性建设。");
        values.put("parsedJdJson", "{\"jobTitle\":\"Java 后端\"}");
        values.put("resumeText", "有订单系统和缓存治理经验。");
        values.put("projectHighlights", "接口耗时降低 40%。");
        values.put("targetCompanyType", "互联网平台");
        values.put("gapsJson", "[{\"skill\":\"Kafka\",\"priority\":\"P1\"}]");
        values.put("targetDays", 14);
        values.put("weeklyHours", 12);
        values.put("preferredLearningMode", "项目复盘");
        values.put("nextInterviewDate", "2026-07-20");
        values.put("interviewResult", "未通过");
        values.put("interviewNotes", "系统设计容量估算不足。");
        values.put("qaTranscriptJson", "[{\"q\":\"缓存一致性\",\"a\":\"延迟双删\"}]");
        return Map.copyOf(values);
    }
}
