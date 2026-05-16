# AI Runtime

AI Runtime 是 Code Nest v2.x 的核心治理底座，目标是把模型调用、Prompt、结构化输出、RAG、工作流编排和质量回归统一管理。

## 主要模块

| 模块 | 职责 |
| --- | --- |
| `xiaou-ai` | AI 场景、Prompt、Graph、RAG、结构化输出和指标 |
| `xiaou-system` | 管理端 AI 配置、调试、回归和治理接口 |
| `llamaindex-service` | 知识库导入、检索、召回解释和文档管理 |
| `docker/ai` | AI sidecar Docker 编排示例 |
| `scripts/ai` | 本地 AI 开发脚本 |

## 能力分层

| 层 | 说明 |
| --- | --- |
| 模型层 | 面向 OpenAI Compatible API 的模型配置和连通性测试 |
| Prompt 层 | Prompt 显式命名、版本化、模板化 |
| Graph 层 | 使用 LangGraph4j 编排多步骤 AI 工作流 |
| RAG 层 | 通过 LlamaIndex sidecar 管理知识库检索 |
| Schema 层 | 结构化输出契约和校验 |
| Eval 层 | 黄金样例回归、最近运行结果和质量洞察 |
| Metrics 层 | 运行次数、耗时、失败、场景质量指标 |

## 已接入场景

| 场景 | 说明 |
| --- | --- |
| 社区摘要 | 对社区内容生成结构化摘要 |
| AI 模拟面试 | 出题、追问、评价、总结 |
| 求职作战台 | JD 解析、简历匹配、行动计划、面试复盘 |
| SQL 优化 | SQL 分析、重写、比较和优化建议 |
| 学习驾驶舱 | 成长分、短板诊断、任务闭环和复盘 |

## 管理端入口

| 页面 | 说明 |
| --- | --- |
| `/system/ai-config` | 模型配置、Prompt 调试、RAG 调试、知识库文档和回归样例 |
| `/system/ai-governance` | AI Runtime 质量中心、覆盖矩阵、风险队列和运行洞察 |

## 开发注意

- 新 AI 场景必须先定义输入、输出和失败兜底。
- Prompt 不应散落在业务代码中，优先进入 Prompt catalog。
- 结构化输出要有 Schema 和校验。
- 涉及知识库的场景要定义 RAG profile。
- 高价值场景要补黄金样例回归。

