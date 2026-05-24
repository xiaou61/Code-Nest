# AI Runtime

AI Runtime 是 Code Nest v2.x 的 AI 治理底座。它不是某一个“调用大模型”的工具类，而是一套把模型配置、Prompt、结构化输出、RAG、工作流、回归样例和运行指标统一起来的工程体系。

学习这个模块时，可以按一句话理解：业务模块只关心“我要完成什么 AI 任务”，AI Runtime 负责把提示词、上下文、结构化解析、失败兜底和质量观测都管起来。

## 推荐学习顺序

AI Runtime 的目录很多，最容易迷路。推荐按“一次 AI 调用是怎样被治理的”来读，而不是按包名从上到下扫。

1. 先看 Prompt Catalog，知道项目里有哪些 AI 场景，每个场景的 `promptId`、模板变量和版本是什么。
2. 再看结构化输出契约，理解为什么模型返回不能直接当字符串用。
3. 接着看一个 Graph Runner 或业务 Service，观察 Prompt、RAG、模型调用、解析和 fallback 怎样串起来。
4. 然后看 Metrics 和 Regression，理解项目怎样发现失败率、解析失败、兜底率和回归样例问题。
5. 最后看管理端 `/system/ai-config` 和 `/system/ai-governance`，把源码里的配置、指标和治理分数对上页面。

学习时不要一开始就改模型配置。先用已有的 SQL 优化或模拟面试场景跑一遍，再新增自己的 Prompt/Schema，会更容易看出哪些步骤是必须的。

## 源码地图

| 模块 | 职责 |
| --- | --- |
| `xiaou-ai` | Prompt Catalog、结构化输出、RAG Profile、Graph Runner、AI 服务、指标、回归 |
| `xiaou-system` | 管理端 AI 配置、调试、RAG 文档、回归、运行指标接口 |
| `xiaou-application` | 应用启动配置，注入 `xiaou.ai.*` 运行参数 |
| `llamaindex-service` | RAG sidecar，负责知识库文档导入、检索和健康检查 |
| `docker/ai` | AI 联调 Docker Compose，包含 MySQL、Redis、RAG sidecar 和后端 |
| `vue3-admin-front` | `/system/ai-config` 和 `/system/ai-governance` 管理页面 |

关键源码入口：

| 文件 | 说明 |
| --- | --- |
| `xiaou-ai/src/main/java/com/xiaou/ai/prompt/AiPromptCatalog.java` | Prompt 总目录 |
| `xiaou-ai/src/main/java/com/xiaou/ai/structured/AiStructuredOutputCatalog.java` | 结构化输出契约总目录 |
| `xiaou-ai/src/main/java/com/xiaou/ai/prompt/AiRagQueryCatalog.java` | RAG 查询规格目录 |
| `xiaou-ai/src/main/java/com/xiaou/ai/rag/AiRagRetrievalCatalog.java` | RAG 检索画像目录 |
| `xiaou-ai/src/main/java/com/xiaou/ai/metrics/AiRuntimeMetricsCollector.java` | 运行指标聚合 |
| `xiaou-ai/src/main/java/com/xiaou/ai/regression/AiRegressionServiceImpl.java` | 黄金样例回归服务 |
| `xiaou-ai/src/main/resources/ai-evals/scene-regression-cases.json` | 回归样例数据 |
| `xiaou-system/src/main/java/com/xiaou/system/controller/AiConfigController.java` | AI 配置、调试、RAG、回归、指标接口 |
| `xiaou-ai/src/main/java/com/xiaou/ai/controller/AiGovernanceController.java` | AI 治理总览接口 |

## 能力分层

| 层 | 解决的问题 | 代表代码 |
| --- | --- | --- |
| 模型层 | 连接 OpenAI Compatible API、配置模型和价格 | `AiModelFactory`、`AiProperties` |
| Prompt 层 | 让提示词显式命名、版本化、模板化 | `AiPromptSpec`、各业务 `*PromptSpecs` |
| RAG Query 层 | 描述某个场景要问知识库什么问题 | `AiRagQuerySpec`、`*RagQuerySpecs` |
| RAG Retrieval 层 | 描述检索数量、阈值、场景和召回策略 | `AiRagRetrievalProfile` |
| Graph 层 | 编排多步骤 AI 工作流 | `InterviewGraphRunner`、`JobBattleGraphRunner`、`SqlOptimizeGraphRunner` |
| Schema 层 | 约束模型输出结构并解析为 DTO | `AiStructuredOutputSpec`、`AiStructuredOutputValidator` |
| Eval 层 | 用固定样例测试 Prompt 和解析兜底 | `AiRegressionServiceImpl` |
| Metrics 层 | 统计调用次数、耗时、失败率、兜底率、Token 和成本 | `AiRuntimeMetricsCollector` |
| Governance 层 | 汇总覆盖率、风险和质量分 | `AiGovernanceServiceImpl` |

## 已登记的 AI 场景

Prompt Catalog 当前收集四组场景：

| 场景域 | Prompt 来源 | 典型能力 |
| --- | --- | --- |
| 社区内容 | `CommunityPromptSpecs` | 帖子 AI 摘要 |
| 模拟面试 | `InterviewPromptSpecs` | 出题、追问、回答评价、总结 |
| 求职作战台 | `JobBattlePromptSpecs` | JD 解析、简历匹配、计划生成、面试复盘、目标分析 |
| SQL 优化 | `SqlOptimizePromptSpecs` | SQL 分析、重写、优化前后对比 |

结构化输出契约当前覆盖 13 个结果类型，包括社区摘要、模拟面试 4 类输出、求职作战台 4 类输出、SQL 优化 4 类输出。治理页面会用 Prompt Catalog 和 Schema Catalog 做覆盖率对照。

## 运行配置

配置入口在 `xiaou-application/src/main/resources/application.yml` 的 `xiaou.ai`。

| 配置 | 说明 |
| --- | --- |
| `xiaou.ai.enabled` | 是否启用 AI 能力 |
| `xiaou.ai.provider` | 当前按 OpenAI Compatible API 设计 |
| `xiaou.ai.base-url` | 模型服务 base URL |
| `xiaou.ai.api-key` | 模型 API Key，生产环境必须走环境变量或私有配置 |
| `xiaou.ai.model.chat` | 对话模型名 |
| `xiaou.ai.model.embedding` | embedding 模型名 |
| `xiaou.ai.pricing.*` | 成本估算价格 |
| `xiaou.ai.metrics.persistence.enabled` | 是否把运行指标持久化到 Redis |
| `xiaou.ai.metrics.persistence.redis-key` | 默认 `xiaou:ai:runtime:metrics` |
| `xiaou.ai.timeout.*` | 连接和读取超时 |
| `xiaou.ai.retry.*` | 重试次数和退避 |
| `xiaou.ai.rag.enabled` | 是否启用 RAG |
| `xiaou.ai.rag.endpoint` | LlamaIndex sidecar 地址 |
| `xiaou.ai.rag.api-key` | RAG sidecar 鉴权 key |
| `xiaou.ai.rag.default-top-k` | 默认召回数量 |

AI 联调 compose 会把后端的 `XIAOU_AI_RAG_ENDPOINT` 指向 `http://llamaindex-service:18080`。

## 管理端接口

AI 配置接口统一在 `/admin/ai/config`：

| 接口 | 用途 |
| --- | --- |
| `GET /runtime` | 查看当前运行配置、模型、RAG、指标持久化状态 |
| `GET /schema-catalog` | 查看 Prompt、Schema、RAG Profile 覆盖情况 |
| `GET /regression/cases` | 查看黄金样例清单 |
| `GET /regression/latest` | 查看最近一次回归结果 |
| `GET /regression/history` | 查看回归历史 |
| `GET /regression/scenario-health` | 查看分场景回归健康度 |
| `POST /regression/run` | 手动执行回归 |
| `POST /prompt-debug` | 调试 Prompt |
| `POST /rag-debug` | 调试 RAG 检索 |
| `GET /rag-service/health` | 检查 RAG sidecar 健康 |
| `GET /rag-service/documents` | 查看 RAG 文档 |
| `GET /rag-service/documents/export` | 导出 RAG 文档 |
| `POST /rag-service/sample-import` | 导入样例知识 |
| `POST /rag-service/documents/import` | 导入自定义知识 |
| `DELETE /rag-service/documents` | 删除单个 RAG 文档 |
| `POST /rag-service/documents/batch-delete` | 批量删除 RAG 文档 |
| `GET /metrics` | 查看运行指标 |
| `DELETE /metrics` | 清空运行指标 |
| `POST /test` | 测试模型配置 |

治理总览接口在 `/admin/ai/governance/overview`，管理页面是 `/system/ai-governance`。

## 治理总览如何评分

`AiGovernanceServiceImpl` 会把 Prompt Catalog、Schema Catalog、RAG Catalog 和运行指标组合起来，生成治理视图。

| 维度 | 口径 |
| --- | --- |
| Prompt 配置 | Prompt 是否有系统提示词和用户模板 |
| Schema 覆盖 | Prompt 是否绑定结构化输出契约 |
| RAG 覆盖 | Prompt 是否绑定 RAG Query 或 Retrieval Profile |
| 运行观测 | 近期指标里是否出现过该场景 |
| 失败率 | 运行调用中 error 占比 |
| 兜底率 | 触发 fallback 的比例 |
| 解析失败率 | 结构化输出解析失败的比例 |
| 耗时 | 平均耗时，超过 8000ms 会进入风险队列 |

质量分的核心权重：

| 分项 | 权重 |
| --- | --- |
| Prompt 完整性 | 25% |
| Schema 覆盖 | 25% |
| RAG 覆盖 | 15% |
| 运行覆盖 | 10% |
| 运行质量 | 25% |

高风险项会扣分，例如 Prompt 缺失、运行失败率过高、兜底率过高、结构化解析失败、耗时过长。

## 运行指标

`AiRuntimeMetricsCollector` 记录两类数据：

| 数据 | 说明 |
| --- | --- |
| 分场景聚合 | 调用次数、成功、失败、兜底、解析失败、Token、成本、平均耗时、最近模型 |
| 最近调用 | 最近 50 次调用，包含场景、Prompt、模型、结果、耗时、Token 和成本 |

指标默认在内存中聚合。如果开启 Redis 持久化，重启后会尝试从 Redis 恢复。持久化失败不会中断业务，日志会提示并继续保留内存数据。

适合排查的问题：

- 某个场景是不是从来没有真实跑过。
- 某个模型是否突然失败率上升。
- 结构化解析失败是不是集中在某个 Prompt。
- RAG 开启后平均耗时是否明显变长。
- 成本估算是否符合预期。

## 黄金样例回归

回归样例文件是 `xiaou-ai/src/main/resources/ai-evals/scene-regression-cases.json`。每条样例包含：

| 字段 | 说明 |
| --- | --- |
| `id` | 用例 ID |
| `scenario` | 场景名，例如 `mock_interview_evaluate` |
| `description` | 用例说明 |
| `input` | 业务输入 |
| `response` 或 `responses` | 模拟模型输出 |
| `useFallback` | 是否强制走兜底 |
| `fallbackSequence` | 多步骤图工作流的兜底序列 |
| `expect` | 断言规则 |

断言支持精确字符串、整数范围、文本包含、列表长度和列表包含。回归服务不会真的要求外部模型稳定输出，而是复用固定响应去验证解析、兜底和业务 DTO 映射是否可靠。

## RAG 调试路径

RAG 能力分三层：

1. `AiRagQuerySpec` 描述“要检索什么问题”。
2. `AiRagRetrievalProfile` 描述“怎么检索、取多少、场景是什么”。
3. `LlamaIndexClient` 调用 sidecar，拿回召回节点和解释。

管理端 `/system/ai-config` 可以：

- 检查 RAG sidecar 健康。
- 导入样例知识。
- 导入自定义知识文档。
- 查看、导出、删除知识文档。
- 调试某个场景的 RAG 召回。

如果 `xiaou.ai.rag.enabled=false`，场景仍应有本地兜底，不能因为知识库不可用阻断核心业务。

## 新增一个 AI 场景怎么做

推荐按这个顺序开发：

1. 定义业务输入和输出 DTO。
2. 在对应 `*PromptSpecs` 中新增 `AiPromptSpec`，写清 `promptId`、`key`、`version` 和模板变量。
3. 如果需要结构化输出，在对应 `*StructuredOutputSpecs` 中新增契约，并加入 `AiStructuredOutputCatalog`。
4. 如果需要知识库，在对应 `*RagQuerySpecs` 和 `*RagRetrievalProfiles` 中登记 Query/Profile。
5. 在业务服务里通过 `AiExecutionSupport` 或现有 SceneSupport 调用模型。
6. 解析失败必须有 fallback，不能直接把异常暴露给用户。
7. 在 `scene-regression-cases.json` 中补成功样例和失败兜底样例。
8. 在管理端 AI 配置页试跑 Prompt、RAG 和回归。
9. 在 AI 治理页确认 Prompt、Schema、RAG 和运行观测都能看到。
10. 更新 [AI Schema 与治理](/reference/ai-schemas) 和对应业务模块文档。

## 验证清单

1. 启动后端，确认 `/api/admin/ai/config/runtime` 能返回配置。
2. 在 `/system/ai-config` 测试模型连通性。
3. 如果启用 RAG，访问 `/api/admin/ai/config/rag-service/health`。
4. 导入样例知识，再用 RAG Debug 查询一次。
5. 执行一次 AI 回归，确认 latest run 有通过/失败统计。
6. 触发一个真实业务场景，例如社区摘要或 SQL 优化。
7. 打开 `/system/ai-governance`，确认运行观测从“近期暂无样本”变为有调用数据。
8. 清空 metrics，再触发业务调用，确认最近调用重新累计。

## 常见坑

| 问题 | 典型原因 | 排查 |
| --- | --- | --- |
| 配置测试失败 | base URL、API Key、模型名错误 | 先看 `/runtime`，再用 `/test` |
| 治理页显示 Prompt 风险 | Prompt Catalog 里模板为空 | 检查对应 `*PromptSpecs` |
| Schema 覆盖率低 | 新 Prompt 没有加入结构化输出契约 | 补 `AiStructuredOutputSpec` 并加入 Catalog |
| RAG 覆盖率低 | 场景未登记 Query/Profile | 判断该场景是否需要知识库，需要则补 RAG 目录 |
| 兜底率过高 | 模型不可用、解析失败、Prompt 输出不稳定 | 看 metrics 的 fallback 和 parse failure |
| 重启后指标丢失 | Redis 指标持久化未开启或 Redis 不可用 | 检查 `xiaou.ai.metrics.persistence.*` |
| 回归失败 | DTO 字段改了但样例断言没改 | 同步更新 `scene-regression-cases.json` |

## 学习建议

先从 `AiPromptCatalog` 看有哪些场景，再看一个具体场景的 Prompt、Schema、Graph Runner 和业务 Service。理解一条链路后，再看治理页如何把所有场景汇总成覆盖率、质量分和风险队列。这样学比直接读所有 AI 类更快，也不容易被目录规模吓住。

---

## AI Runtime 模块深度拆解

> 以下内容基于 `xiaou-ai` 全部源码逐行拆解，覆盖 1 个 ModelFactory、3 个 GraphRunner、1 个 MetricsCollector（867 行）、1 个 GovernanceServiceImpl、1 个 RegressionServiceImpl、1 个 LlamaIndexClient、1 个 CommunityServiceImpl、1 个 StructuredOutputCatalog、3 个 SceneSupport、1 个 ExecutionSupport + 1 个 RegressionExecutionSupport 内部类。

### 一、模型工厂与延迟初始化

**源码**：`AiModelFactory.java`（97 行）

```
AiModelFactory:
  字段: volatile ChatModel chatModel
  依赖: AiProperties aiProperties

  isChatAvailable():
    aiProperties.enabled && hasApiKey && hasBaseUrl && model.chat 非空

  getChatModel():
    双检锁: localRef = chatModel → 非空返回
    synchronized(this): chatModel == null → createChatModel()
    创建后缓存到 volatile 字段

  chat(systemPrompt, userPrompt):
    getChatModel().chat([SystemMessage, UserMessage])
    null → defaultText(value) 转为空字符串
    返回 AiChatResult(content, modelName, tokenUsage)

  createChatModel():
    provider 只允许 "openai" 或 "openai-compatible"
    OpenAiChatModel.builder()
      .baseUrl / .apiKey / .modelName
      .timeout(readMs) / .maxRetries
      .logRequests(false) / .logResponses(false)
```

**关键发现 1**：`chatModel` 使用 volatile + synchronized 双检锁延迟初始化。一旦创建就不会重新创建——即使运行时修改了 `AiProperties` 的 `baseUrl` 或 `apiKey`，已缓存的 `chatModel` 不会更新。这意味着**配置变更需要重启应用**才能生效。

**关键发现 2**：`defaultText(null)` 返回空字符串 `""`。这意味着如果业务层没有传 `systemPrompt` 或 `userPrompt`，模型会收到一个空字符串消息而不是 null。空字符串作为 SystemMessage 不会导致异常，但可能导致模型行为不符合预期——模型可能会忽略空的系统提示词，导致输出偏离业务要求。

**关键发现 3**：`logRequests(false)` 和 `logResponses(false)` 禁用了 LangChain4j 的请求/响应日志。这避免了 API Key 和完整 Prompt 在日志中泄露（安全利好），但也意味着**无法通过日志排查模型调用问题**——如果模型返回格式错误或内容异常，只能依赖 MetricsCollector 的 outcome 和 latency 记录。

### 二、LangGraph4j 图编排架构详解

项目使用 LangGraph4j 的 `StateGraph` + `CompiledGraph` 模式编排多步骤 AI 工作流。每个 Graph Runner 维护多个编译后的图实例，同样使用 volatile + synchronized 双检锁延迟初始化。

#### 2.1 InterviewGraphRunner（4 个图实例）

```
questionGraph:   __START__ → retrieve_context → generate_questions → __END__
evaluationGraph: __START__ → retrieve_context → evaluate_answer → ensureFollowUpQuestion → __END__
summaryGraph:    __START__ → retrieve_context → generate_summary → __END__
followUpGraph:   __START__ → retrieve_context → generate_follow_up → __END__
```

**关键发现 1**：`evaluationGraph` 在 `evaluate_answer` 之后增加了一个 `ensureFollowUpQuestion` 补偿节点。如果模型评价结果标记了 `nextAction = "followUp"` 但 `followUpQuestion` 为空，这个节点会额外调用 `sceneSupport.generateFollowUpQuestion()` 补一个追问问题。这是一个**业务补偿设计**——模型有时会标记"应该追问"但不生成追问内容，补偿节点确保追问场景不漏。

**关键发现 2**：所有图的第一个节点都是 `retrieve_context`，执行 RAG 检索。如果 `LlamaIndexClient.isAvailable()` 返回 false，`ragContext` 被设为空字符串，后续节点继续纯模型链路。如果检索异常，只 warn 并设空上下文。这意味着 **RAG 不可用时不会阻断任何 AI 场景**——降级到纯模型是设计如此。

#### 2.2 JobBattleGraphRunner（5 个图实例）

```
parseJdGraph:          __START__ → parse_jd → __END__
matchResumeGraph:      __START__ → match_resume → __END__
generatePlanGraph:     __START__ → generate_plan → __END__
reviewInterviewGraph:  __START__ → review_interview → __END__
analyzeTargetGraph:    __START__ → analyze_target_jd → analyze_target_resume_match → analyze_target_plan → riskCheck → __END__
```

**关键发现**：`analyzeTargetGraph` 是唯一的多节点串行图——它串联了 JD 解析、简历匹配和计划生成三个步骤，最后加一个 `riskCheck` 节点做安全审查。其他图都是单节点直线图（retrieve_context + 业务节点）。

#### 2.3 SqlOptimizeGraphRunner（4 个图实例）

```
analyzeGraph:          __START__ → retrieve_context → analyze → riskCheck → __END__
analyzeRewriteGraph:   __START__ → retrieve_context → analyze → rewrite → riskCheck → __END__
rewriteGraph:          __START__ → retrieve_context → rewrite → riskCheck → __END__
compareGraph:          __START__ → compare → __END__
```

**关键发现 1**：所有 SQL 优化图（除 compare）在业务节点后都加了 `riskCheck` 后安全审查。这是**AI 输出安全防线**——SQL 优化建议可能包含危险操作（如 DROP、DELETE 全表），riskCheck 会检查并拦截。

**关键发现 2**：`compareGraph` 不包含 `retrieve_context` 节点。SQL 对比场景不需要 RAG 知识库——它只需要比较两条 SQL 的 explain 结果，所以直接走 compare 节点。

### 三、指标聚合器深度分析

**源码**：`AiRuntimeMetricsCollector.java`（867 行）

```
AiRuntimeMetricsCollector:
  字段:
    sceneAccumulators: LinkedHashMap&lt;sceneKey, SceneAccumulator&gt;
    recentCalls: ArrayDeque&lt;AiRuntimeRecentCall&gt; (MAX=50)
    persistence: AiRuntimeMetricsPersistence (可选 Redis)

  recordInvocation(sceneName, promptSpec, outcome, durationNanos, modelName, tokens, cost):
    synchronized(this)
    → computeIfAbsent sceneAccumulator
    → accumulator.recordInvocation(...)
    → recentCalls.addFirst + 淘汰超 50 条
    → persistState()

  recordFallback / recordStructuredParseFailure:
    synchronized(this)
    → computeIfAbsent + recordFallback/recordParseFailure
    → persistState()

  snapshot(sceneKeyword, outcome, modelKeyword, recentLimit):
    synchronized(this)
    → 遍历 sceneAccumulators 做筛选和聚合
    → 生成 AiRuntimeMetricsSnapshot

  clear():
    synchronized(this)
    → sceneAccumulators.clear() + recentCalls.clear() + clearPersistence()

SceneAccumulator:
  内部结构:
    totalAccumulator / successAccumulator / errorAccumulator: InvocationAccumulator
    totalModelAccumulators / successModelAccumulators / errorModelAccumulators: LinkedHashMap
    totalModelFallbackCounts / errorModelFallbackCounts: LinkedHashMap
    totalModelParseFailureCounts / errorModelParseFailureCounts: LinkedHashMap
    fallbackCount / errorFallbackCount / structuredParseFailureCount / errorStructuredParseFailureCount: long

  recordInvocation(outcome, latencyMs, modelName, tokens, cost, timestamp):
    → totalAccumulator.record(...)
    → totalModelAccumulators.computeIfAbsent(modelName).record(...)
    → outcome=="success" → successAccumulator + successModelAccumulators
    → 否则 → errorAccumulator + errorModelAccumulators

  recordFallback(modelName):
    → fallbackCount++ + errorFallbackCount++
    → totalModelFallbackCounts.merge(modelName, 1, Long::sum)
    → errorModelFallbackCounts.merge(modelName, 1, Long::sum)

  recordParseFailure(modelName):
    → structuredParseFailureCount++ + errorStructuredParseFailureCount++
    → totalModelParseFailureCounts.merge + errorModelParseFailureCounts.merge

InvocationAccumulator:
  invocations / totalLatencyMs / totalInputTokens / totalOutputTokens / totalTokens / estimatedCost / lastModelName / lastInvocationAt
```

**关键发现 1**：所有公共方法都是 `synchronized`。这意味着高并发 AI 调用场景下，`recordInvocation` 和 `snapshot` 会串行执行。如果 AI 场景调用频繁（比如社区摘要每秒多次），MetricsCollector 会成为瓶颈。

**关键发现 2**：`buildSceneKey` 使用 `sceneName|promptKey|promptVersion` 组合。这意味着**同一个场景的不同 Prompt 版本会创建不同的 SceneAccumulator**。当 Prompt 版本迭代时，旧版本的指标数据不会被合并——历史版本的指标独立存在，直到被 clear 清空。

**关键发现 3**：每次 `recordInvocation`、`recordFallback` 和 `recordParseFailure` 都会调用 `persistState()`，把完整状态序列化写入 Redis。这意味着**每次 AI 调用都会触发一次 Redis 写入**。如果调用频繁，Redis 写入压力不小。这是为了"重启后指标恢复"做的代价。

**关键发现 4**：`sceneAccumulators` 使用 `LinkedHashMap`，没有容量上限。如果项目不断增加新 AI 场景或 Prompt 版本频繁迭代，内存占用会持续增长——没有淘汰机制。

### 四、治理评分体系深度分析

**源码**：`AiGovernanceServiceImpl.java`（428 行）

```
buildQualityScore(total, configured, schemaCovered, ragCovered, runtimeObserved, runtimeInsight, riskItems):
  promptScore     = calcRate(configured, total) * 0.25
  schemaScore     = calcRate(schemaCovered, total) * 0.25
  ragScore        = calcRate(ragCovered, total) * 0.15
  runtimeCoverage = calcRate(runtimeObserved, total) * 0.10
  runtimeQuality  = totalInvocations == 0 → 82 (默认值)
                   | totalInvocations > 0 → successRate - fallbackRate*0.35 - parseFailureRate*0.45
  runtimeScore    = runtimeQuality * 0.25
  riskPenalty     = riskItems 中 HIGH→10, MEDIUM→5, LOW→1 的累计
  finalScore      = clamp(promptScore + schemaScore + ragScore + runtimeCoverage + runtimeScore - min(riskPenalty, 28), 0, 100)

resolveQualityGrade(score):
  ≥90 → A
  ≥75 → B
  ≥60 → C
  <60 → D

resolveRiskLevel(configured, fallbackCovered):
  !configured → HIGH
  fallbackCovered → LOW
  否则 → MEDIUM

buildRiskItems(workflows, snapshot):
  静态风险:
    !configured → HIGH, score=96, "Prompt 模板未完整配置"
    !schemaCovered → MEDIUM, score=72, "缺少结构化输出契约"
    !ragCovered → LOW, score=38
    !runtimeObserved → LOW, score=30
  动态风险 (基于运行指标):
    errorRate ≥ 20% → HIGH, score=92
    errorRate ≥ 8% → MEDIUM, score=68
    fallbackRate ≥ 20% → MEDIUM, score=64
    parseFailureRate > 0 → MEDIUM, score=62
    averageLatency ≥ 8000ms → MEDIUM, score=58
  最多展示 10 条风险，按 score 降序排列
```

**关键发现 1**：当 `totalInvocations == 0`（没有运行样本）时，`runtimeQuality` 默认为 82。这是一个"善意默认值"——系统刚上线时没有运行数据不应该被惩罚太重。但这也意味着**一个从未跑过的 AI 场景，runtimeScore 会拿到 82 * 0.25 = 20.5 分**，与实际运行质量无关。

**关键发现 2**：评分权重和风险分数全部硬编码在源码中。如果业务优先级变化（比如 RAG 覆盖比 Schema 覆盖更重要），需要改代码而不是改配置。

**关键发现 3**：风险扣分上限为 28 分（`min(riskPenalty, 28)`）。这意味着即使有大量高风险项，质量分最多扣 28 分。如果基础分很高（比如 85），扣 28 后还有 57 分（C 级），不会跌到 D 级。

### 五、黄金样例回归体系深度分析

**源码**：`AiRegressionServiceImpl.java`（548 行）

```
AiRegressionServiceImpl:
  构造: 只注入 AiProperties，不注入其他 Spring Bean

  run(scenario, caseId):
    加载样例 → 按场景/ID 筛选 → 逐条 runSingleCase → 汇总

  runSingleCase(testCase):
    resolveDiagnostics → 记录图名和 Prompt ID
    executeScenario → switch 14 个场景路由 → 调用对应图 Runner 或 Service
    AiRegressionAssertionSupport.validate → 断言

  buildInterviewGraphRunner(testCase):
    new InterviewSceneSupport(buildExecutionSupport, metricsRecorder)
    new InterviewGraphRunner(sceneSupport, buildUnavailableLlamaIndexClient)

  buildExecutionSupport(testCase, metricsRecorder):
    new AiRegressionExecutionSupport(testCase, metricsRecorder)

AiRegressionExecutionSupport (内部类，继承 AiExecutionSupport):
  字段:
    responses: Deque&lt;String&gt; ← 从 testCase.getResponses() 初始化
    fallbackSequence: Deque&lt;Boolean&gt; ← 从 testCase.getFallbackSequence() 初始化

  chatWithFallback(sceneName, promptSpec, variables, parser, fallbackSupplier):
    shouldFallback = fallbackSequence.isEmpty() → testCase.isUseFallback()
                   | 否则 → fallbackSequence.removeFirst()
    shouldFallback → fallbackSupplier.get()
    否则 → parser.apply(responses.isEmpty() ? testCase.getResponse() : responses.removeFirst())
```

**关键发现 1**：`AiRegressionServiceImpl` 不注入任何业务 Spring Bean——它自己手动创建 `InterviewGraphRunner`、`JobBattleGraphRunner`、`SqlOptimizeGraphRunner`、`AiCommunityServiceImpl`。这意味着回归服务绕过了 Spring 的依赖注入体系，**新建的实例与线上运行的实例不同**。如果线上 Service 有 Spring AOP（如操作日志切面）或额外的依赖，回归测试不会覆盖这些。

**关键发现 2**：所有图 Runner 构建时使用 `buildUnavailableLlamaIndexClient()`——传入一个没有 endpoint 配置的 LlamaIndexClient。这意味着**回归测试永远不测试 RAG 链路**——它只测试纯模型链路和兜底逻辑。

**关键发现 3**：`fallbackSequence` 是一个 `Deque<Boolean>`，多步骤图工作流（如 analyzeTargetGraph 有 3 个步骤）需要为每个步骤指定"是否走兜底"。如果 `fallbackSequence` 为空，退回到 `testCase.isUseFallback()` 作为全局默认值。这允许精细控制每一步的兜底行为。

### 六、RAG 客户端与降级策略

**源码**：`LlamaIndexClient.java`（254 行）

```
LlamaIndexClient:
  字段: volatile RestClient restClient
  依赖: AiProperties, RestClient.Builder

  isAvailable():
    rag.enabled && rag.endpoint 非空

  retrieve(request):
    !isAvailable → throw AiRetrievalException
    request.query 非空 → getRestClient().post().uri(RETRIEVE_PATH).body(prepareRequest)
    prepareRequest → topK=null/≤0 时设为 rag.defaultTopK

  getRestClient():
    双检锁: localRef = restClient → 非空返回
    synchronized(this): restClient == null → build
    builder.baseUrl(rag.endpoint) + Accept:application/json
    rag.apiKey 非空 → Authorization:Bearer apiKey
```

**关键发现 1**：与 `AiModelFactory` 一样，`restClient` 使用 volatile + synchronized 双检锁。创建后不会重建——**RAG endpoint 或 API Key 变更同样需要重启应用**。

**关键发现 2**：`retrieve()` 在 `isAvailable()` 返回 false 时直接抛 `AiRetrievalException`。但 Graph Runner 的 `retrieveContext` 方法先检查 `isAvailable()`，不满足时直接设空上下文而不调用 `retrieve()`。这意味着 `retrieve()` 方法中的异常抛出路径在正常流程中不会被触发——它只在管理端 RAG Debug 等显式调用场景中才可能抛出。

**关键发现 3**：`prepareRequest` 方法直接修改了入参 `request.setTopK()`。这是**对入参对象的隐含副作用**——调用方如果后续还使用 request 对象，topK 可能已被修改。

### 七、结构化输出契约体系

**源码**：`AiStructuredOutputCatalog.java`（39 行）

```
AiStructuredOutputCatalog:
  SPECS = List.of(13 个契约):
    社区: CommunityStructuredOutputSpecs.POST_SUMMARY
    面试: EVALUATE_ANSWER / GENERATE_SUMMARY / GENERATE_QUESTIONS / GENERATE_FOLLOW_UP
    求职: JD_PARSE / RESUME_MATCH / PLAN_GENERATE / INTERVIEW_REVIEW
    SQL:  ANALYZE / ANALYZE_V2 / REWRITE / COMPARE
```

每个 `AiStructuredOutputSpec` 包含 `promptId`、`outputClass`、`jsonSchema` 和 `validateObject` 方法。

**关键发现**：`validateObject` 方法在 `AiCommunityServiceImpl.parseSummaryResult` 中被显式调用——先用 `AiJsonResponseParser.parse` 解析 JSON，再用 `validateObject(json)` 校验字段。这意味着结构化输出验证是在**JSON 解析之后**做的，而不是在模型调用层面约束输出格式。如果模型返回的 JSON 格式正确但内容不符合 Schema，`validateObject` 会返回 `valid=false` 并触发兜底。

### 八、社区摘要服务的 keywords 解析容错

**源码**：`AiCommunityServiceImpl.java`（99 行）

```
generatePostSummary(title, content):
  aiExecutionSupport.chatWithFallback(
    sceneName="community_summary",
    promptSpec=CommunityPromptSpecs.POST_SUMMARY,
    variables=buildPromptVariables(title, content),
    parser=this::parseSummaryResult,
    fallback=() -> PostSummaryResult.fallbackResult(title)
  )

parseSummaryResult(response):
  AiJsonResponseParser.parse → null 或 error → fallbackResult + recordParseFailure
  CommunityStructuredOutputSpecs.POST_SUMMARY.validateObject → invalid → fallbackResult + recordParseFailure
  正常 → parseKeywords(json.get("keywords"))

parseKeywords(keywordsObj):
  keywordsObj == null → List.of()
  keywordsObj instanceof String → 按逗号分割
  keywordsObj instanceof List → 逐项 toString
```

**关键发现**：`parseKeywords` 方法同时处理 String 和 List 两种格式。这是因为模型返回 keywords 时格式不确定——有时返回 `"keywords": "Java,Spring,微服务"`（字符串），有时返回 `"keywords": ["Java", "Spring", "微服务"]`（数组）。这种双格式兼容是模型输出不稳定的典型应对策略。

### 九、深度发现与坑点

#### 9.1 已确认的代码问题

| 编号 | 问题 | 位置 | 影响 |
| --- | --- | --- | --- |
| BUG-1 | AiModelFactory 配置变更需重启 | `AiModelFactory.getChatModel` volatile+synchronized 双检锁 | 运行时修改 base-url/apiKey/model 无法生效 |
| BUG-2 | LlamaIndexClient 配置变更需重启 | `LlamaIndexClient.getRestClient` 同上 | RAG endpoint/apiKey 变更无法热更新 |
| BUG-3 | MetricsCollector fallback 记入 errorFallback | `SceneAccumulator.recordFallback` | fallbackCount 和 errorFallbackCount 同时+1，兜底被计入"错误侧" |
| BUG-4 | MetricsCollector 每次调用写 Redis | `AiRuntimeMetricsCollector.recordInvocation:104` | 高频调用场景下 Redis 写入压力大 |
| BUG-5 | sceneKey 包含 Prompt 版本 | `AiRuntimeMetricsCollector.buildSceneKey` | Prompt 版本迭代后旧指标独立存在，无法合并 |
| BUG-6 | 回归测试不覆盖 RAG 链路 | `AiRegressionServiceImpl.buildInterviewGraphRunner` | 所有图 Runner 使用 unavailable LlamaIndexClient |
| BUG-7 | null Prompt 变为空字符串 | `AiModelFactory.defaultText(null)` → `""` | 空 SystemMessage 可能导致模型行为偏离 |
| BUG-8 | LangChain4j 请求/响应日志被禁用 | `AiModelFactory.createChatModel:89-90` | 无法通过应用日志排查模型调用问题 |

#### 9.2 设计层面的潜在风险

| 编号 | 风险 | 说明 |
| --- | --- | --- |
| RISK-1 | MetricsCollector synchronized 瓶颈 | 所有记录和查询操作串行化，高频 AI 调用时可能成为瓶颈 |
| RISK-2 | 治理评分权重硬编码 | Prompt/Schema/RAG/Runtime 权重 25/25/15/10/25 不可配置 |
| RISK-3 | 风险分数硬编码 | HIGH=10, MEDIUM=5, LOW=1 以及各风险项的具体分数不可配置 |
| RISK-4 | 多节点图串行依赖传播 | analyzeTargetGraph 3 个节点串行，任意节点失败导致整条链路中断 |
| RISK-5 | 回归服务脱离 Spring 依赖注入 | 手动创建 Runner/Service，不经过 Spring AOP 和 Bean 生命周期 |
| RISK-6 | MetricsCollector 无容量上限 | sceneAccumulators 和 recentCalls 没有淘汰机制 |
| RISK-7 | AI 输出仅作为辅助建议 | 治理规则明确"关键业务状态推进仍由服务端规则校验"，但实际没有强制拦截机制 |

#### 9.3 架构设计亮点

| 编号 | 亮点 | 说明 |
| --- | --- | --- |
| H-1 | LangGraph4j 图编排多步骤工作流 | retrieve_context → 业务节点 → 补偿/安全审查，支持串行和分支 |
| H-2 | RAG 降级不阻断核心链路 | Graph Runner 检查 isAvailable 后设空上下文，纯模型继续执行 |
| H-3 | volatile + synchronized 双检锁 | AiModelFactory 和 LlamaIndexClient 延迟初始化，避免启动时依赖外部服务 |
| H-4 | 结构化输出契约 + validateObject | 13 个契约覆盖 4 个场景域，JSON 解析后二次校验字段 |
| H-5 | 黄金样例回归 fallbackSequence | 多步骤图工作流可精细控制每步兜底行为，`Deque<Boolean>` 支持逐步指定 |
| H-6 | SQL riskCheck 后安全审查 | SQL 优化建议后加安全检查，防止模型输出危险 SQL |
| H-7 | ensureFollowUpQuestion 补偿节点 | 面试评价图在评价后检查追问内容缺失，自动补充追问问题 |
| H-8 | 治理评分 5 维度加权体系 | Prompt + Schema + RAG + RuntimeCoverage + RuntimeQuality，扣分上限 28 |
| H-9 | 无运行样本时 runtimeQuality 默认 82 | 新场景不因缺乏运行数据而被过度惩罚 |
| H-10 | keywords 双格式解析 | 同时兼容模型返回 String 和 List 格式的 keywords |
| H-11 | fallbackSequence 逐步控制 | 多步骤图工作流中每步可以独立指定是否走兜底 |
| H-12 | logRequests/logResponses 关闭 | API Key 和完整 Prompt 不在日志中泄露 |

#### 9.4 源码导航速查

| 想了解 | 读什么 |
| --- | --- |
| 模型初始化 | `AiModelFactory.java` — volatile+synchronized 双检锁 + OpenAiChatModel |
| 图编排 | `InterviewGraphRunner.java` / `JobBattleGraphRunner.java` / `SqlOptimizeGraphRunner.java` — 4+5+4 图实例 |
| 指标聚合 | `AiRuntimeMetricsCollector.java` — 867 行，synchronized，SceneAccumulator，Redis 持久化 |
| 治理评分 | `AiGovernanceServiceImpl.java` — 5 维度加权 + 风险扣分 + 覆盖矩阵 |
| 黄金回归 | `AiRegressionServiceImpl.java` — 14 场景路由 + 手动构建依赖 + fallbackSequence |
| RAG 客户端 | `LlamaIndexClient.java` — volatile+synchronized + 7 个外部 API 方法 |
| 结构化契约 | `AiStructuredOutputCatalog.java` — 13 契约 + validateObject |
| 社区摘要 | `AiCommunityServiceImpl.java` — chatWithFallback + keywords 双格式 |
| 场景支撑 | `InterviewSceneSupport.java` / `JobBattleSceneSupport.java` / `SqlOptimizeSceneSupport.java` |
| 执行支撑 | `AiExecutionSupport.java` — chatWithFallback 通用模式 |
