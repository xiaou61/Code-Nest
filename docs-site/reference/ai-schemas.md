# AI Schema 与治理

AI Runtime 位于 `xiaou-ai`，管理入口位于 `xiaou-system` 的 `AiConfigController` 和 `AiGovernanceController`。本页整理 Prompt、RAG、结构化输出、Graph 和回归验证的源码入口。

## 能力分层

| 层 | 源码入口 | 说明 |
| --- | --- | --- |
| 模型客户端 | `xiaou-ai/client` | 模型工厂、聊天结果封装 |
| Prompt catalog | `xiaou-ai/prompt` | Prompt 规格、变量、版本、治理元数据 |
| RAG query catalog | `xiaou-ai/prompt/*RagQuerySpecs.java` | 按场景定义检索问题 |
| RAG profile | `xiaou-ai/rag` | LlamaIndex 客户端、检索 profile、文档导入导出 |
| 结构化输出 | `xiaou-ai/structured` | JSON Schema 生成、校验、导出 |
| Graph 编排 | `xiaou-ai/graph` | 面试、求职、SQL 优化场景工作流 |
| 场景服务 | `xiaou-ai/service` | 面向业务模块的 AI service facade |
| 回归验证 | `xiaou-ai/regression`、`xiaou-ai/src/main/resources/ai-evals` | 场景用例、期望、运行结果 |
| 指标观测 | `xiaou-ai/metrics` | 调用次数、模型、场景、成功失败、最近调用 |

## 场景清单

| 场景 | Prompt | RAG | 结构化输出 | Graph |
| --- | --- | --- | --- | --- |
| 面试答题评估、追问、总结、出题 | `prompt/interview` | `prompt/interview`、`rag/interview` | `structured/interview` | `graph/interview` |
| 求职 JD 解析、简历匹配、计划、复盘、目标分析 | `prompt/jobbattle` | `prompt/jobbattle`、`rag/jobbattle` | `structured/jobbattle` | `graph/jobbattle` |
| SQL 分析、改写、对比、分析改写 | `prompt/sql` | `prompt/sql`、`rag/sql` | `structured/sql` | `graph/sql` |
| 社区帖子摘要 | `prompt/community` | 暂无独立 profile | `structured/community` | 直接 service 调用 |

## 管理接口

| 接口 | 说明 |
| --- | --- |
| `GET /admin/ai/config/runtime` | 查看模型和 RAG 运行配置，敏感值脱敏 |
| `GET /admin/ai/config/schema-catalog` | 汇总 Prompt、RAG query、RAG profile、结构化 Schema |
| `POST /admin/ai/config/prompt-debug` | 按 `promptId` 和变量渲染 Prompt 并执行调用 |
| `POST /admin/ai/config/rag-debug` | 按 `profileId`、query、scene、topK 执行检索 |
| `GET /admin/ai/config/rag-service/health` | 检查 LlamaIndex sidecar 健康状态 |
| `GET /admin/ai/config/rag-service/documents` | 查询 RAG 文档 |
| `GET /admin/ai/config/rag-service/documents/export` | 导出 RAG 文档 |
| `POST /admin/ai/config/rag-service/sample-import` | 导入内置样例知识 |
| `POST /admin/ai/config/rag-service/documents/import` | 导入自定义知识 |
| `DELETE /admin/ai/config/rag-service/documents` | 删除单条知识 |
| `POST /admin/ai/config/rag-service/documents/batch-delete` | 批量删除知识 |
| `GET /admin/ai/config/regression/cases` | 查看回归用例 catalog |
| `POST /admin/ai/config/regression/run` | 运行 AI 回归 |
| `GET /admin/ai/config/regression/latest`、`/history`、`/scenario-health` | 查看最新、历史和场景健康 |
| `GET /admin/ai/config/metrics`、`DELETE /admin/ai/config/metrics` | 查看或清空 Runtime 指标 |
| `GET /admin/ai/governance/overview` | AI 治理概览 |

## 结构化输出规范

| 规范 | 说明 |
| --- | --- |
| Schema 必须登记到 `AiStructuredOutputCatalog` | 管理端才能在 schema catalog 中看到 |
| Prompt 与 Schema 通过 `promptId` 关联 | `debugPrompt` 会按 Prompt 查找结构化输出 |
| 输出必须经过 `AiStructuredOutputValidator` | 避免模型返回缺字段或类型错误 |
| 业务 DTO 放在 `xiaou-ai/dto` | 不要让业务模块重复定义 AI 输出结构 |
| 失败兜底在场景 service 或 Graph runner 中处理 | 页面不直接依赖模型原始文本 |

## 回归用例

| 位置 | 说明 |
| --- | --- |
| `xiaou-ai/src/main/resources/ai-evals/scene-regression-cases.json` | 运行时回归用例 |
| `xiaou-ai/src/test/java/com/xiaou/ai/eval/scenario` | 测试侧场景定义 |
| `AiRegressionServiceImpl` | 运行回归、记录结果、生成 summary |

新增 AI 能力时，至少补齐 Prompt、结构化输出、测试或回归样例、管理端 schema catalog 可见性，再更新本页和 [AI Runtime](/modules/ai-runtime)。
