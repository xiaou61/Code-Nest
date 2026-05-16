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
