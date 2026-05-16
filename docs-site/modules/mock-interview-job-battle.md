# 模拟面试与求职作战台

该模块把 AI 模拟面试、JD 解析、简历匹配、行动计划、面试复盘和求职闭环串成一条求职增长链路。

## 功能入口

| 端 | 页面 |
| --- | --- |
| 用户端 | `/mock-interview`、`/mock-interview/config`、`/mock-interview/session`、`/mock-interview/report`、`/mock-interview/history` |
| 用户端 | `/job-battle`、`/job-match-engine`、`/career-loop` |
| 管理端 | `/mock-interview/sessions`、`/mock-interview/directions`、`/system/ai-config`、`/system/ai-governance` |
| 后端模块 | `xiaou-mock-interview`、`xiaou-ai` |

## 模拟面试接口域

| 接口域 | 能力 |
| --- | --- |
| `/user/mock-interview` | 方向、配置、题单、创建面试、历史、报告、统计、AI 总结 |
| `/user/mock-interview/session` | 开始、作答、跳过、下一题、结束、状态、追问 |
| `/admin/mock-interview` | 方向配置、会话列表、会话详情、统计 |

## 求职作战台接口域

| 接口域 | 能力 |
| --- | --- |
| `/user/job-battle/jd/parse` | JD 解析 |
| `/user/job-battle/resume/match` | 简历匹配 |
| `/user/job-battle/plan/generate` | 行动计划生成 |
| `/user/job-battle/plan/history` | 历史计划 |
| `/user/job-battle/match-engine/run` | 匹配引擎运行 |
| `/user/job-battle/interview/review` | 面试复盘 |
| `/user/career-loop` | 求职闭环启动、当前状态、时间线、行动项、资料同步 |

## AI 编排

AI 能力由 `xiaou-ai` 提供，核心点包括：

- Prompt catalog 管理提示词。
- Graph runner 编排多步骤任务。
- RAG profile 提供知识库增强。
- Structured output schema 约束输出。
- Regression eval 保护高价值场景质量。

## 核心流程

1. 用户选择面试方向和配置。
2. 系统创建面试会话。
3. AI 生成问题，用户作答。
4. AI 对答案评分、反馈并生成追问。
5. 用户结束面试后生成报告。
6. 报告可转学习资产或进入求职闭环。
7. 求职作战台读取 JD、简历和历史结果生成行动计划。

## 文档深化点

- 面试会话状态机。
- AI 评分维度和报告字段。
- 追问生成规则。
- JD 解析结构。
- 简历匹配分口径。
- Career Loop 阶段枚举和行动项生命周期。

