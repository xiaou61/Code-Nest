package com.xiaou.ai.structured;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 结构化输出契约样例。
 *
 * @author xiaou
 */
final class AiStructuredOutputFixtures {

    private static final Map<String, String> JSON_BY_SPEC_ID = buildFixtures();

    private AiStructuredOutputFixtures() {
    }

    static String json(AiStructuredOutputSpec spec) {
        return JSON_BY_SPEC_ID.get(spec.specId());
    }

    private static Map<String, String> buildFixtures() {
        Map<String, String> fixtures = new LinkedHashMap<>();

        fixtures.put("community.post_summary:v1",
                """
                        {"summary":"帖子复盘了 Redis 缓存一致性问题，并给出补偿方案。","keywords":["Redis","缓存一致性","补偿机制"]}
                        """);

        fixtures.put("mock_interview.evaluate_answer:v1",
                """
                        {"score":8,"feedback":{"strengths":["回答有条理"],"improvements":["可以补充边界条件"]},"nextAction":"followUp","followUpQuestion":"如果发生超卖你会怎么兜底？","referencePoints":["并发控制","幂等","补偿"]}
                        """);
        fixtures.put("mock_interview.generate_summary:v1",
                """
                        {"summary":"候选人在并发与缓存方面基础较好，但系统设计表达还需更结构化。","overallLevel":"良好","suggestions":["补充容量估算过程","回答时先总后分","增加故障复盘案例"]}
                        """);
        fixtures.put("mock_interview.generate_questions:v1",
                """
                        [{"question":"请讲一下缓存一致性方案。","answer":"可以从旁路缓存、延迟双删和消息一致性展开。","knowledgePoints":"缓存一致性,延迟双删,最终一致性"}]
                        """);
        fixtures.put("mock_interview.generate_follow_up:v1",
                """
                        {"followUpQuestion":"如果双删的第二次删除失败，你会如何补偿？"}
                        """);

        fixtures.put("job_battle.jd_parse:v1",
                """
                        {"jobTitle":"高级Java后端开发","level":"P6","mustSkills":["Java","Redis"],"niceSkills":["Kafka"],"responsibilities":["负责交易链路"],"seniorityYears":"5年以上","keywords":["高并发","稳定性"],"riskPoints":["需要承担线上值班"],"summary":"岗位重点在交易链路和稳定性建设。"}
                        """);
        fixtures.put("job_battle.resume_match:v1",
                """
                        {"overallScore":84,"dimensionScores":{"skillMatch":88,"projectDepth":82,"architectureAbility":80,"communicationClarity":85},"strengths":["技术栈匹配"],"missingKeywords":["Kafka"],"estimatedPassRate":76,"gaps":[{"skill":"消息队列","priority":"P1","why":"缺少削峰解耦证据","suggestedAction":"补充 MQ 项目案例"}],"resumeRewriteSuggestions":[{"section":"项目经历","advice":"补充流量规模与优化收益"}]}
                        """);
        fixtures.put("job_battle.plan_generate:v1",
                """
                        {"planName":"两周冲刺计划","totalDays":14,"weeklyGoals":["第1周完成系统设计梳理"],"dailyTasks":[{"day":1,"task":"梳理缓存架构案例","durationMinutes":90,"taskType":"learn","deliverable":"缓存架构脑图"}],"milestones":[{"day":7,"goal":"完成专题首轮整理"}],"riskAndFallback":["工作繁忙时优先保留 P0 任务"]}
                        """);
        fixtures.put("job_battle.interview_review:v1",
                """
                        {"overallConclusion":"系统设计表达和指标量化仍需加强。","rootCauses":["缺少容量估算"],"highImpactFixes":[{"issue":"系统设计表达不成体系","action":"重写高频系统设计题","deadline":"D+3","metric":"5 分钟讲清方案"}],"questionTypeWeakness":[{"type":"系统设计","suggestion":"按容量、链路、扩展性、容灾展开"}],"next7DayPlan":["Day1：整理模板"],"confidenceScore":82}
                        """);

        fixtures.put("sql_optimize.analyze:v1",
                """
                        {"score":42,"optimizedSql":"SELECT id FROM orders WHERE user_id = 1","knowledgePoints":["联合索引","避免回表"],"problems":[{"type":"FULL_TABLE_SCAN","severity":"HIGH","description":"未命中过滤索引","location":"orders"}],"explainAnalysis":[{"table":"orders","type":"ALL","typeExplain":"全表扫描","key":"NULL","keyExplain":"未命中索引","rows":500000,"extra":"Using filesort","extraExplain":"需要额外排序","issue":"扫描和排序成本较高"}],"suggestions":[{"type":"ADD_INDEX","title":"补充联合索引","ddl":"ALTER TABLE orders ADD INDEX idx_user_ctime(user_id, create_time DESC);","sql":"","reason":"让过滤与排序尽量命中同一索引","expectedImprovement":"显著减少扫描与排序开销"}]}
                        """);
        fixtures.put("sql_optimize.analyze:v2", fixtures.get("sql_optimize.analyze:v1"));
        fixtures.put("sql_optimize.rewrite:v2",
                """
                        {"optimizedSql":"SELECT id, create_time FROM orders WHERE user_id = 1 ORDER BY create_time DESC LIMIT 20","indexDdls":["ALTER TABLE orders ADD INDEX idx_user_ctime(user_id, create_time DESC);"],"rewriteReason":"通过覆盖索引减少回表与排序。","riskWarnings":["需要评估新增索引带来的写入成本"],"expectedImprovement":"减少回表与 filesort。"}
                        """);
        fixtures.put("sql_optimize.compare:v2",
                """
                        {"improvementScore":88,"deltaRows":"扫描行数由 500000 降至 20","deltaType":"访问类型从 ALL 优化到 range","deltaExtra":"Extra 从 Using filesort 优化为 Using index condition","summary":"优化后基本消除了全表扫描和额外排序。","attention":["确认新增索引对写入放大的影响"]}
                        """);

        return Map.copyOf(fixtures);
    }
}
