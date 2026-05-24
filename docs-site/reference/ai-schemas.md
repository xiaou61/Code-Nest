# AI 结构化输出 Schema 索引

本页索引 AI 模块所有结构化输出 DTO（Structured Output）的 Schema 定义。AI 模块使用 Spring AI 的 `BeanOutputConverter` + JSON Schema 驱动，将大模型输出反序列化为强类型 Java 对象。每条 Schema 都绑定一个 Prompt 模板，构成「Prompt + Schema = 任务单元」的组合。

## 架构总览

```
AiStructuredOutputSpec (枚举注册)
  ├── scene → Prompt 模板 ID
  ├── outputClass → Schema DTO 类
  └── converter → BeanOutputConverter&lt;T&gt; (自动生成 JSON Schema)

AiPromptSpec (枚举注册)
  ├── id → Prompt 模板 ID
  ├── systemPrompt → 系统提示词
  └── userPromptTemplate → 用户提示词模板 (Velocity)
```

**调用链路：**
1. 前端请求 → Controller → `AiSceneService`
2. `AiSceneService` 从 `AiStructuredOutputSpec` 获取 `outputClass` + `converter`
3. 从 `AiPromptSpec` 获取 Prompt 模板，注入变量
4. 调用 LLM，输出 JSON → `converter.convert()` 反序列化 → 强类型 DTO
5. DTO 返回给调用方

## 面试模块 Schema

### GeneratedQuestion — AI 生成面试题

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| questionText | String | 题目正文 |
| questionType | String | 题目类型：TECHNICAL / BEHAVIORAL / SYSTEM_DESIGN |
| difficulty | String | 难度：EASY / MEDIUM / HARD |
| category | String | 分类标签 |
| keyPoints | List&lt;String&gt; | 考察要点列表 |
| sampleAnswer | String | 参考答案 |
| followUpQuestions | List&lt;String&gt; | 追问方向 |
| estimatedTimeMinutes | int | 建议答题时间（分钟） |

**使用场景：** 模拟面试中 AI 根据方向和难度动态生成面试题

### AnswerEvaluationResult — 答案评估

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| score | int | 评分 0-100 |
| strengths | List&lt;String&gt; | 答案优点 |
| weaknesses | List&lt;String&gt; | 答案不足 |
| suggestions | List&lt;String&gt; | 改进建议 |
| keyPointsCovered | List&lt;String&gt; | 已覆盖的考察要点 |
| keyPointsMissed | List&lt;String&gt; | 遗漏的考察要点 |
| overallFeedback | String | 综合评语 |
| depthLevel | String | 回答深度：SURFACE / MODERATE / DEEP |
| communicationScore | int | 表达清晰度 0-100 |

**使用场景：** 模拟面试中 AI 评估用户回答

### InterviewSummaryResult — 面试总结

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| overallScore | int | 综合评分 0-100 |
| categoryScores | Map&lt;String, Integer&gt; | 分类维度评分 |
| summary | String | 综合评价 |
| strengths | List&lt;String&gt; | 整体优势 |
| areasForImprovement | List&lt;String&gt; | 待提升领域 |
| recommendations | List&lt;String&gt; | 学习建议 |
| estimatedLevel | String | 建议等级：JUNIOR / MID / SENIOR / STAFF |
| nextSteps | List&lt;String&gt; | 下一步行动建议 |

**使用场景：** 模拟面试结束后生成综合报告

## 求职作战台 Schema

### JobBattleJdParseResult — JD 解析

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| jobTitle | String | 岗位名称 |
| company | String | 公司名称 |
| requiredSkills | List&lt;String&gt; | 必备技能 |
| preferredSkills | List&lt;String&gt; | 加分技能 |
| experience | String | 经验要求 |
| education | String | 学历要求 |
| responsibilities | List&lt;String&gt; | 工作职责 |
| salaryRange | String | 薪资范围 |
| location | String | 工作地点 |
| keywords | List&lt;String&gt; | 关键词提取 |

**使用场景：** 用户输入 JD 原文，AI 提取结构化信息

### JobBattleResumeMatchResult — 简历匹配分析

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| matchScore | int | 匹配度 0-100 |
| matchedSkills | List&lt;String&gt; | 已匹配技能 |
| missingSkills | List&lt;String&gt; | 缺失技能 |
| experienceMatch | String | 经验匹配评估 |
| suggestions | List&lt;String&gt; | 优化建议 |
| riskAreas | List&lt;String&gt; | 风险点 |
| highlightItems | List&lt;String&gt; | 可突出展示项 |

**使用场景：** JD + 简历匹配，分析差距和优化方向

### JobBattlePlanResult — 备战计划

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| planTitle | String | 计划标题 |
| duration | String | 建议准备周期 |
| phases | List&lt;PlanPhase&gt; | 分阶段计划 |
| dailyTasks | List&lt;String&gt; | 每日任务清单 |
| resources | List&lt;String&gt; | 推荐学习资源 |
| milestones | List&lt;String&gt; | 里程碑节点 |

**内嵌 PlanPhase：**
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| phaseName | String | 阶段名称 |
| duration | String | 阶段时长 |
| focus | String | 重点方向 |
| tasks | List&lt;String&gt; | 任务列表 |

**使用场景：** 基于匹配差距生成定制化备战计划

### JobBattleTargetAnalysisResult — 目标岗位分析

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| targetCompany | String | 目标公司 |
| targetRole | String | 目标岗位 |
| industryAnalysis | String | 行业分析 |
| companyCulture | String | 公司文化特点 |
| interviewProcess | String | 面试流程 |
| keyCompetencies | List&lt;String&gt; | 核心能力要求 |
| preparationStrategy | String | 备考策略 |
| timeline | String | 建议时间线 |

**使用场景：** 对目标岗位进行深度分析

### JobBattleInterviewReviewResult — 面试复盘

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| reviewSummary | String | 复盘摘要 |
| performanceHighlights | List&lt;String&gt; | 表现亮点 |
| criticalMoments | List&lt;String&gt; | 关键时刻 |
| lessonsLearned | List&lt;String&gt; | 经验教训 |
| improvementActions | List&lt;String&gt; | 改进行动 |
| nextInterviewTips | List&lt;String&gt; | 下次面试建议 |

**使用场景：** 面试结束后 AI 辅助复盘

## OJ 判题 Schema

### CodeReviewResult — AI 代码审查

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| overallQuality | String | 代码质量：EXCELLENT / GOOD / AVERAGE / POOR |
| readability | int | 可读性评分 0-100 |
| maintainability | int | 可维护性评分 0-100 |
| efficiency | int | 效率评分 0-100 |
| issues | List&lt;CodeIssue&gt; | 问题列表 |
| suggestions | List&lt;String&gt; | 优化建议 |

**内嵌 CodeIssue：**
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| severity | String | 严重程度：CRITICAL / WARNING / INFO |
| location | String | 位置描述 |
| description | String | 问题描述 |
| suggestion | String | 修改建议 |

**使用场景：** OJ 提交代码后 AI 自动审查

### ComplexityAnalysisResult — 复杂度分析

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| timeComplexity | String | 时间复杂度（如 O(n log n)） |
| spaceComplexity | String | 空间复杂度 |
| timeExplanation | String | 时间复杂度推导 |
| spaceExplanation | String | 空间复杂度推导 |
| isOptimal | boolean | 是否最优解 |
| optimalComplexity | String | 最优复杂度 |
| bottleneck | String | 性能瓶颈说明 |

**使用场景：** 分析用户提交代码的时空复杂度

### HintGenerationResult — 提示生成

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| hintLevel1 | String | 一级提示（方向性） |
| hintLevel2 | String | 二级提示（方法论） |
| hintLevel3 | String | 三级提示（接近答案） |
| keyInsight | String | 核心思路点拨 |
| relatedConcepts | List&lt;String&gt; | 相关概念 |

**使用场景：** 用户做题卡住时，分级给提示（避免直接给答案）

### TestCaseGenerationResult — 测试用例生成

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| testCases | List&lt;TestCase&gt; | 测试用例列表 |
| edgeCases | List&lt;TestCase&gt; | 边界用例列表 |
| coverage | String | 覆盖率评估 |

**内嵌 TestCase：**
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| input | String | 输入 |
| expectedOutput | String | 期望输出 |
| description | String | 用例说明 |
| category | String | 用例分类 |

**使用场景：** 为题目自动生成测试用例

### AlternativeSolutionResult — 多解法推荐

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| solutions | List&lt;SolutionDetail&gt; | 解法列表 |

**内嵌 SolutionDetail：**
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| approach | String | 解法思路 |
| timeComplexity | String | 时间复杂度 |
| spaceComplexity | String | 空间复杂度 |
| codeTemplate | String | 代码模板 |
| pros | List&lt;String&gt; | 优点 |
| cons | List&lt;String&gt; | 缺点 |

**使用场景：** 展示同一题目的多种解法

### ProblemAnalysisResult — 题目分析

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| problemType | String | 题目类型分类 |
| difficulty | String | 难度评估 |
| keyConcepts | List&lt;String&gt; | 核心概念 |
| approach | String | 推荐思路 |
| commonPitfalls | List&lt;String&gt; | 常见陷阱 |
| relatedProblems | List&lt;String&gt; | 关联题目 |

**使用场景：** 分析 OJ 题目，帮助理解出题意图

## 简历系统 Schema

### ResumeOptimizationResult — 简历优化建议

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| overallScore | int | 综合评分 0-100 |
| sectionScores | Map&lt;String, Integer&gt; | 各板块评分 |
| optimizations | List&lt;OptimizationItem&gt; | 优化项列表 |

**内嵌 OptimizationItem：**
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| section | String | 所属板块 |
| priority | String | 优先级：HIGH / MEDIUM / LOW |
| currentContent | String | 当前内容 |
| suggestedContent | String | 建议内容 |
| reason | String | 优化理由 |

**使用场景：** AI 分析简历并提出针对性优化建议

### ResumeHealthCheckResult — 简历健康巡检

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| healthScore | int | 健康度 0-100 |
| issues | List&lt;HealthIssue&gt; | 问题列表 |
| passedChecks | List&lt;String&gt; | 通过的检查项 |
| warnings | List&lt;String&gt; | 警告项 |

**内嵌 HealthIssue：**
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| severity | String | 严重程度：CRITICAL / WARNING / INFO |
| category | String | 问题分类 |
| description | String | 问题描述 |
| fix | String | 修复建议 |

**使用场景：** 简历上传后自动健康巡检，管理端可批量查看

## 知识图谱 Schema

### KnowledgeMapResult — 知识图谱生成

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| title | String | 图谱标题 |
| nodes | List&lt;KnowledgeNode&gt; | 知识节点 |
| edges | List&lt;KnowledgeEdge&gt; | 关联关系 |

**内嵌 KnowledgeNode：**
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | String | 节点 ID |
| label | String | 节点名称 |
| category | String | 分类 |
| description | String | 说明 |
| importance | String | 重要度：CORE / IMPORTANT / SUPPLEMENTARY |

**内嵌 KnowledgeEdge：**
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| source | String | 起始节点 ID |
| target | String | 目标节点 ID |
| relation | String | 关系类型：DEPENDS_ON / RELATED_TO / PREREQUISITE |
| label | String | 关系说明 |

**使用场景：** 输入主题，AI 生成可交互的知识图谱数据

## 学习成长 Schema

### StudyPlanResult — 学习计划生成

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| planTitle | String | 计划标题 |
| duration | String | 计划周期 |
| dailySchedule | List&lt;DailyTask&gt; | 每日安排 |
| weeklyGoals | List&lt;String&gt; | 周目标 |
| resources | List&lt;String&gt; | 推荐资源 |

**内嵌 DailyTask：**
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| day | int | 第几天 |
| topic | String | 学习主题 |
| tasks | List&lt;String&gt; | 任务清单 |
| estimatedHours | double | 预计时长 |

**使用场景：** 成长闭环驾驶舱生成个性化学习计划

### GrowthAnalysisResult — 成长分析

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| currentLevel | String | 当前等级评估 |
| growthRate | String | 成长速率评估 |
| strongAreas | List&lt;String&gt; | 优势领域 |
| weakAreas | List&lt;String&gt; | 薄弱领域 |
| trend | String | 成长趋势：UPWARD / STABLE / DECLINING |
| recommendations | List&lt;String&gt; | 提升建议 |

**使用场景：** 综合用户行为数据，AI 生成成长分析报告

### GrowthAutopilotResult — 成长自动驾驶

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| dailyPlan | String | 今日计划概要 |
| focusArea | String | 今日重点 |
| tasks | List&lt;String&gt; | 推荐任务 |
| reviewItems | List&lt;String&gt; | 复习项目 |
| motivationQuote | String | 激励语 |
| estimatedCompletionTime | String | 预计完成时间 |

**使用场景：** 每日自动推送个性化成长任务（xaiou-plan 模块调用）

## SQL 优化 Schema

### SqlAnalyzeResult — SQL 分析

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| originalSql | String | 原始 SQL |
| analysisType | String | 分析模式：PERFORMANCE / CORRECTNESS / SECURITY |
| issues | List&lt;SqlIssue&gt; | 问题列表 |
| overallAssessment | String | 总体评估 |
| performanceScore | int | 性能评分 0-100 |

**内嵌 SqlIssue：**
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| severity | String | 严重程度 |
| type | String | 问题类型 |
| description | String | 问题描述 |
| line | int | 行号 |
| suggestion | String | 修复建议 |

**使用场景：** SQL 优化工作台分析输入的 SQL 语句

### SqlRewriteResult — SQL 重写

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| rewrittenSql | String | 重写后的 SQL |
| explanation | String | 重写说明 |
| performanceImprovement | String | 性能提升描述 |
| beforeCost | String | 优化前代价评估 |
| afterCost | String | 优化后代价评估 |
| rewriteRules | List&lt;String&gt; | 应用的重写规则 |

**使用场景：** 基于分析结果提供 SQL 重写建议

## 社区 Schema

### PostSummaryResult — 帖子摘要

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| summary | String | 内容摘要 |
| keyPoints | List&lt;String&gt; | 核心要点 |
| tags | List&lt;String&gt; | 推荐标签 |
| readingTimeMinutes | int | 预计阅读时间 |
| difficulty | String | 内容难度 |

**使用场景：** 长帖自动生成摘要和标签

## LangGraph 状态 Schema

AI 模块使用 LangGraph4j 实现多轮对话的状态机，每个场景有独立的 State 定义。

### InterviewState — 模拟面试状态

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| sessionId | String | 会话 ID |
| taskType | InterviewTaskType | 任务类型枚举 |
| direction | String | 面试方向 |
| difficulty | String | 难度 |
| currentQuestion | GeneratedQuestion | 当前题目 |
| userAnswer | String | 用户回答 |
| evaluation | AnswerEvaluationResult | 评估结果 |
| questionCount | int | 已出题数 |
| maxQuestions | int | 最大题目数 |
| history | List&lt;QaPair&gt; | 对话历史 |

**InterviewTaskType 枚举：**
`GENERATE_QUESTION` | `EVALUATE_ANSWER` | `SUMMARIZE` | `HINT`

### JobBattleState — 求职作战状态

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| sessionId | String | 会话 ID |
| taskType | JobBattleTaskType | 任务类型枚举 |
| jdText | String | JD 原文 |
| resumeText | String | 简历原文 |
| jdParseResult | JobBattleJdParseResult | JD 解析结果 |
| matchResult | JobBattleResumeMatchResult | 匹配结果 |
| planResult | JobBattlePlanResult | 备战计划 |
| analysisResult | JobBattleTargetAnalysisResult | 岗位分析 |
| reviewResult | JobBattleInterviewReviewResult | 面试复盘 |

**JobBattleTaskType 枚举：**
`PARSE_JD` | `MATCH_RESUME` | `GENERATE_PLAN` | `ANALYZE_TARGET` | `REVIEW_INTERVIEW`

### SqlOptimizeState — SQL 优化状态

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| sessionId | String | 会话 ID |
| originalSql | String | 原始 SQL |
| analyzeMode | SqlAnalyzeMode | 分析模式枚举 |
| analyzeResult | SqlAnalyzeResult | 分析结果 |
| rewriteResult | SqlRewriteResult | 重写结果 |
| iterationCount | int | 迭代次数 |

**SqlAnalyzeMode 枚举：**
`PERFORMANCE` | `CORRECTNESS` | `SECURITY`

## AI 治理 Schema

### AiGovernanceOverviewResponse — 治理总览

管理端 `/ai/governance/overview` 返回的结构体，聚合 AI 系统全量质量指标。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| totalCalls | long | 调用总量 |
| successRate | double | 成功率 |
| avgLatencyMs | double | 平均延迟 |
| p99LatencyMs | double | P99 延迟 |
| totalTokensUsed | long | Token 消耗总量 |
| estimatedCost | double | 估算费用 |
| errorDistribution | Map&lt;String, Long&gt; | 错误分类分布 |
| topScenes | List&lt;SceneMetric&gt; | 高频场景指标 |

## API 路由与 Schema 映射

### 用户端 API

| 路由 | 方法 | 请求体 | 响应 Schema |
| --- | --- | --- | --- |
| /ai/interview/generate | POST | direction, difficulty | GeneratedQuestion |
| /ai/interview/evaluate | POST | question, answer | AnswerEvaluationResult |
| /ai/interview/summary | POST | sessionId | InterviewSummaryResult |
| /ai/interview/hint | POST | question, level | HintGenerationResult |
| /ai/job-battle/parse-jd | POST | jdText | JobBattleJdParseResult |
| /ai/job-battle/match | POST | jdText, resumeText | JobBattleResumeMatchResult |
| /ai/job-battle/plan | POST | jdText, resumeText | JobBattlePlanResult |
| /ai/job-battle/analyze-target | POST | targetCompany, targetRole | JobBattleTargetAnalysisResult |
| /ai/job-battle/review | POST | reviewText | JobBattleInterviewReviewResult |
| /ai/oj/code-review | POST | code, language | CodeReviewResult |
| /ai/oj/complexity | POST | code, language | ComplexityAnalysisResult |
| /ai/oj/hint | POST | problemId, level | HintGenerationResult |
| /ai/oj/test-cases | POST | problemId | TestCaseGenerationResult |
| /ai/oj/alternatives | POST | problemId | AlternativeSolutionResult |
| /ai/oj/analyze | POST | problemId | ProblemAnalysisResult |
| /ai/resume/optimize | POST | resumeText, jdText | ResumeOptimizationResult |
| /ai/resume/health-check | POST | resumeText | ResumeHealthCheckResult |
| /ai/knowledge/generate | POST | topic | KnowledgeMapResult |
| /ai/learning/plan | POST | target, duration | StudyPlanResult |
| /ai/growth/analyze | POST | userId | GrowthAnalysisResult |
| /ai/growth/autopilot | POST | userId | GrowthAutopilotResult |
| /ai/sql/analyze | POST | sql, mode | SqlAnalyzeResult |
| /ai/sql/rewrite | POST | sql | SqlRewriteResult |
| /ai/community/summarize | POST | postContent | PostSummaryResult |

### 管理端 API

| 路由 | 方法 | 说明 |
| --- | --- | --- |
| /ai/admin/config | GET | 获取 AI 配置（模型、参数） |
| /ai/admin/config | PUT | 更新 AI 配置 |
| /ai/governance/overview | GET | 治理总览 |
| /ai/governance/metrics | GET | 详细的调用指标 |
| /ai/governance/regression | GET | 回归测试结果 |

## Prompt 模板注册机制

所有 Prompt 通过 `AiPromptSpec` 枚举注册，运行时自动加载：

```java
public enum AiPromptSpec {
    INTERVIEW_GENERATE("interview.generate", systemPrompt, userPromptTemplate),
    INTERVIEW_EVALUATE("interview.evaluate", systemPrompt, userPromptTemplate),
    OJ_CODE_REVIEW("oj.codeReview", systemPrompt, userPromptTemplate),
    // ...
    ;
}
```

每个枚举值对应：
- **id** — 全局唯一标识，与前端请求中的 scene 参数一致
- **systemPrompt** — 系统角色设定
- **userPromptTemplate** — Velocity 模板，支持 ${variable} 占位符

## Schema 验证与容错

AI 模块的 Schema 验证链路：

1. **输出约束** — `BeanOutputConverter` 将 DTO 类自动转为 JSON Schema 注入 Prompt
2. **格式校验** — LLM 返回 JSON 后，Jackson 反序列化时校验字段类型
3. **降级处理** — 反序列化失败时返回 `Result.error("AI 输出解析失败")`
4. **AI 回归测试** — 18 条用例覆盖所有 Schema 的解析正确性

## 维护检查清单

新增 AI Schema 时：

1. 在 `xiaou-ai/src/main/java/com/xiaou/ai/dto/` 下创建 DTO 类
2. 在 `AiStructuredOutputSpec` 枚举中注册 scene → outputClass 映射
3. 在 `AiPromptSpec` 枚举中注册对应的 Prompt 模板
4. 如果是多轮对话场景，在对应 `graph/` 包下创建 State 类
5. 在 `AiSceneService` 中添加场景路由逻辑
6. 编写 AI 回归测试用例
7. 更新本页 Schema 索引
8. 更新对应的模块功能文档

## 源码导航

| 文件 | 说明 |
| --- | --- |
| `xiaou-ai/.../structured/AiStructuredOutputSpec.java` | Schema 注册枚举 |
| `xiaou-ai/.../prompt/AiPromptSpec.java` | Prompt 模板注册枚举 |
| `xiaou-ai/.../dto/interview/` | 面试模块 DTO |
| `xiaou-ai/.../dto/jobbattle/` | 求职作战 DTO |
| `xiaou-ai/.../dto/oj/` | OJ 模块 DTO |
| `xiaou-ai/.../dto/resume/` | 简历模块 DTO |
| `xiaou-ai/.../dto/knowledge/` | 知识图谱 DTO |
| `xiaou-ai/.../dto/learning/` | 学习计划 DTO |
| `xiaou-ai/.../dto/growth/` | 成长分析 DTO |
| `xiaou-ai/.../dto/sql/` | SQL 优化 DTO |
| `xiaou-ai/.../dto/community/` | 社区摘要 DTO |
| `xiaou-ai/.../dto/governance/` | AI 治理 DTO |
| `xiaou-ai/.../graph/interview/InterviewState.java` | 面试状态机 |
| `xiaou-ai/.../graph/jobbattle/JobBattleState.java` | 求职状态机 |
| `xiaou-ai/.../graph/sql/SqlOptimizeState.java` | SQL 优化状态机 |
