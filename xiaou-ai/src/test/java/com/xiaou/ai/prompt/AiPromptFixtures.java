package com.xiaou.ai.prompt;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Prompt / RAG Query 渲染样例。
 *
 * @author xiaou
 */
public final class AiPromptFixtures {

    private static final Map<String, Map<String, Object>> PROMPT_FIXTURES = buildPromptFixtures();
    private static final Map<String, Map<String, Object>> RAG_QUERY_FIXTURES = buildRagQueryFixtures();

    private AiPromptFixtures() {
    }

    public static Map<String, Object> promptVariables(AiPromptSpec spec) {
        return PROMPT_FIXTURES.get(spec.promptId());
    }

    public static Map<String, Object> ragQueryVariables(AiRagQuerySpec spec) {
        return RAG_QUERY_FIXTURES.get(spec.queryId());
    }

    private static Map<String, Map<String, Object>> buildPromptFixtures() {
        Map<String, Map<String, Object>> fixtures = new LinkedHashMap<>();

        fixtures.put("community.post_summary:v1", mapOf(
                "title", "SQL 优化经验总结",
                "content", "这是一篇关于 SQL 优化与索引设计的实践复盘。"
        ));

        Map<String, Object> interviewEvaluation = mapOf(
                "direction", "Java后端",
                "level", "高级",
                "style", "压力型",
                "followUpCount", 1,
                "question", "你如何设计高并发库存扣减？",
                "answer", "我会结合 Redis 预扣、幂等校验和最终一致性方案。",
                "ragSection", AiPromptSections.ragSection("库存扣减要关注幂等、防超卖和补偿机制。")
        );
        fixtures.put("mock_interview.evaluate_answer:v1", interviewEvaluation);
        fixtures.put("mock_interview.generate_follow_up:v1", interviewEvaluation);

        fixtures.put("mock_interview.generate_summary:v1", mapOf(
                "direction", "Java后端",
                "level", "高级",
                "questionCount", 8,
                "answeredCount", 7,
                "skippedCount", 1,
                "totalScore", 78,
                "qaListJson", "[{\"question\":\"Redis 为什么快\",\"answer\":\"基于内存与高效数据结构\",\"score\":8}]",
                "ragSection", AiPromptSections.ragSection("复盘建议要覆盖能力模型、短板和下一阶段行动。")
        ));

        fixtures.put("mock_interview.generate_questions:v1", mapOf(
                "direction", "Java后端",
                "level", "高级",
                "count", 5,
                "ragSection", AiPromptSections.ragSection("优先覆盖并发、缓存、消息队列和数据库。")
        ));

        fixtures.put("job_battle.jd_parse:v1", mapOf(
                "targetRole", "Java开发工程师",
                "targetLevel", "高级",
                "city", "上海",
                "jdText", "负责高并发服务设计、缓存治理和分布式系统建设。",
                "ragSection", AiPromptSections.ragSection("岗位画像优先关注架构能力、业务复杂度与稳定性经验。")
        ));

        fixtures.put("job_battle.resume_match:v1", mapOf(
                "parsedJdJson", "{\"jobTitle\":\"Java开发工程师\",\"mustSkills\":[\"Redis\",\"MySQL\"]}",
                "resumeText", "5 年 Java 后端经验，负责订单与营销系统。",
                "projectHighlights", "主导秒杀链路优化，峰值 TPS 提升 3 倍。",
                "targetCompanyType", "互联网",
                "ragSection", AiPromptSections.ragSection("匹配分析要强调关键词缺失、项目深度与证据强弱。")
        ));

        fixtures.put("job_battle.plan_generate:v1", mapOf(
                "gapsJson", "[{\"skill\":\"系统设计\",\"priority\":\"P0\"}]",
                "targetDays", 21,
                "weeklyHours", 12,
                "preferredLearningMode", "项目实战",
                "nextInterviewDate", "2026-05-10",
                "ragSection", AiPromptSections.ragSection("计划应体现冲刺节奏、优先级与产出物。")
        ));

        fixtures.put("job_battle.interview_review:v1", mapOf(
                "targetRole", "Java开发工程师",
                "interviewResult", "二面未通过",
                "nextInterviewDate", "2026-05-02",
                "interviewNotes", "项目深挖不足，分布式事务回答偏泛。",
                "qaTranscriptJson", "[{\"question\":\"分布式事务方案\",\"answer\":\"用了本地消息表\"}]",
                "ragSection", AiPromptSections.ragSection("复盘要关注高影响修复动作和 7 天冲刺计划。")
        ));

        Map<String, Object> sqlAnalyze = mapOf(
                "sql", "select * from orders where user_id = 1 order by create_time desc limit 20",
                "explainFormat", "TRADITIONAL",
                "explainResult", "[{\"table\":\"orders\",\"type\":\"ALL\",\"rows\":120000}]",
                "tableStructures", "[{\"tableName\":\"orders\",\"ddl\":\"CREATE TABLE orders(...)\"}]",
                "mysqlVersion", "8.0",
                "ragSection", AiPromptSections.ragSection("分析要重点考虑全表扫描、排序和覆盖索引。")
        );
        fixtures.put("sql_optimize.analyze:v1", sqlAnalyze);
        fixtures.put("sql_optimize.analyze:v2", sqlAnalyze);

        fixtures.put("sql_optimize.rewrite:v2", mapOf(
                "originalSql", "select * from orders where user_id = 1 order by create_time desc limit 20",
                "diagnoseJson", "{\"score\":52,\"problems\":[{\"type\":\"FULL_TABLE_SCAN\"}]}",
                "tableStructures", "[{\"tableName\":\"orders\",\"ddl\":\"CREATE TABLE orders(...)\"}]",
                "mysqlVersion", "8.0",
                "ragSection", AiPromptSections.ragSection("重写建议要兼顾语义安全、索引维护成本与回表风险。")
        ));

        fixtures.put("sql_optimize.compare:v2", mapOf(
                "explainFormat", "TRADITIONAL",
                "beforeSql", "select * from orders where user_id = 1 order by create_time desc limit 20",
                "beforeExplain", "[{\"type\":\"ALL\",\"rows\":120000}]",
                "afterSql", "select id, create_time from orders force index(idx_user_ctime) where user_id = 1 order by create_time desc limit 20",
                "afterExplain", "[{\"type\":\"range\",\"rows\":120}]"
        ));

        return Map.copyOf(fixtures);
    }

    private static Map<String, Map<String, Object>> buildRagQueryFixtures() {
        Map<String, Map<String, Object>> fixtures = new LinkedHashMap<>();

        fixtures.put("mock_interview.retrieve.generate_questions:v1", mapOf(
                "direction", "Java后端",
                "level", "高级",
                "count", 5
        ));
        fixtures.put("mock_interview.retrieve.generate_summary:v1", mapOf(
                "direction", "Java后端",
                "level", "高级",
                "questionCount", 8,
                "answeredCount", 7,
                "skippedCount", 1,
                "totalScore", 78
        ));
        fixtures.put("mock_interview.retrieve.generate_follow_up:v1", mapOf(
                "direction", "Java后端",
                "level", "高级",
                "style", "压力型",
                "question", "你如何设计高并发库存扣减？",
                "answer", "结合 Redis 预扣与数据库补偿。",
                "followUpCount", 1
        ));
        fixtures.put("mock_interview.retrieve.evaluate_answer:v1", mapOf(
                "direction", "Java后端",
                "level", "高级",
                "style", "压力型",
                "question", "你如何设计高并发库存扣减？",
                "answer", "结合 Redis 预扣与数据库补偿。",
                "followUpCount", 1
        ));

        fixtures.put("job_battle.retrieve.parse_jd:v1", mapOf(
                "targetRole", "Java开发工程师",
                "targetLevel", "高级",
                "city", "上海",
                "jdText", "负责高并发服务设计、缓存治理和分布式系统建设。"
        ));
        fixtures.put("job_battle.retrieve.match_resume:v1", mapOf(
                "parsedJdJson", "{\"jobTitle\":\"Java开发工程师\"}",
                "resumeText", "5 年 Java 后端经验，负责订单与营销系统。",
                "projectHighlights", "主导秒杀链路优化，峰值 TPS 提升 3 倍。",
                "targetCompanyType", "互联网"
        ));
        fixtures.put("job_battle.retrieve.generate_plan:v1", mapOf(
                "gapsJson", "[{\"skill\":\"系统设计\",\"priority\":\"P0\"}]",
                "targetDays", 21,
                "weeklyHours", 12,
                "preferredLearningMode", "项目实战",
                "nextInterviewDate", "2026-05-10"
        ));
        fixtures.put("job_battle.retrieve.review_interview:v1", mapOf(
                "targetRole", "Java开发工程师",
                "interviewResult", "二面未通过",
                "interviewNotes", "项目深挖不足，分布式事务回答偏泛。",
                "qaTranscriptJson", "[{\"question\":\"分布式事务方案\",\"answer\":\"用了本地消息表\"}]",
                "nextInterviewDate", "2026-05-02"
        ));
        fixtures.put("job_battle.retrieve.analyze_target:v1", mapOf(
                "targetRole", "Java开发工程师",
                "targetLevel", "高级",
                "city", "上海",
                "jdText", "负责高并发服务设计、缓存治理和分布式系统建设。",
                "resumeText", "5 年 Java 后端经验，负责订单与营销系统。",
                "projectHighlights", "主导秒杀链路优化，峰值 TPS 提升 3 倍。",
                "targetCompanyType", "互联网"
        ));

        fixtures.put("sql_optimize.retrieve.analyze:v1", mapOf(
                "sql", "select * from orders where user_id = 1",
                "explainResult", "[{\"type\":\"ALL\",\"rows\":120000}]"
        ));
        fixtures.put("sql_optimize.retrieve.rewrite:v1", mapOf(
                "originalSql", "select * from orders where user_id = 1",
                "diagnoseJson", "{\"score\":52,\"problems\":[{\"type\":\"FULL_TABLE_SCAN\"}]}"
        ));

        return Map.copyOf(fixtures);
    }

    private static Map<String, Object> mapOf(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return Map.copyOf(map);
    }
}
