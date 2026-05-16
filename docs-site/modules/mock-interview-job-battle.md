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

## 面试会话状态

会话状态定义在 `SessionStatusEnum`：

| 状态码 | 状态 | 说明 |
| --- | --- | --- |
| `0` | `ONGOING` | 创建后进入，只有进行中会话允许开始、答题、跳过、追问和获取当前状态 |
| `1` | `COMPLETED` | 用户结束面试或生成总结时写入，报告页读取该状态 |
| `2` | `INTERRUPTED` | 预留中断态，后续可用于异常退出或超时恢复 |

`createInterview` 会拒绝同一用户同时存在进行中面试。`startInterview` 设置开始时间并把当前题序推进到 1；`endInterview` 会计算总分和四个维度分，之后推送一次求职闭环事件。

## QA 状态和追问

问答状态定义在 `QAStatusEnum`：

| 状态码 | 状态 | 进入条件 |
| --- | --- | --- |
| `0` | `PENDING` | 创建主问题或追问时写入 |
| `1` | `ANSWERED` | 用户提交答案并完成 AI 评分 |
| `2` | `SKIPPED` | 用户主动跳过问题 |

主问题可以来自本地题库，也可以由 AI 生成。回答后，`AIInterviewerService.evaluateAnswer` 返回评分、优点、改进项、下一步动作和追问问题；如果 `nextAction` 是 `followUp`，系统会创建追问记录。手动追问每道主问题最多 2 次，超过会返回业务错误。

## 报告字段

报告由 `getReport` 聚合：

| 字段 | 来源 |
| --- | --- |
| 总分 | 所有已评分 QA 的均值换算到 0 到 100 |
| 知识、深度、表达、应变 | 基于总分做当前版本的简化估算 |
| AI 总结和建议 | 用户手动触发 `/{id}/summary` 后写入 |
| QA 明细 | 主问题和对应追问树 |
| 面试时长 | `startTime` 到 `endTime` 的秒数 |

## Career Loop 阶段

求职闭环阶段定义在 `CareerLoopStageEnum`，状态机只允许幂等或向前推进，不允许回退。

| 阶段 | 说明 | 典型触发 |
| --- | --- | --- |
| `INIT` | 初始化 | 创建闭环会话 |
| `JD_PARSED` | 已完成 JD 解析 | JD 解析事件 |
| `RESUME_MATCHED` | 已完成简历匹配 | 简历匹配事件 |
| `PLAN_READY` | 计划已生成 | 行动计划生成 |
| `PLAN_EXECUTING` | 计划执行中 | `planProgress >= 20` |
| `INTERVIEW_DONE` | 面试已完成 | 模拟面试结束或总结 |
| `REVIEWED` | 复盘已完成 | `reviewCount > 0` |
| `OFFER_TRACKING` | 投递与 Offer 跟踪 | 后续投递推进 |

阶段推进后会写入 `career_loop_stage_log`，并按阶段重置 `career_loop_action` 的待办项。画像快照保存在 `career_loop_snapshot`，包含计划进度、模拟面试次数、最新模拟分、复盘次数、风险标记和下一步建议。

## 验证点

| 场景 | 预期 |
| --- | --- |
| 同一用户重复创建面试 | 第二次创建被拒绝，提示先完成当前面试 |
| 本地题库模式无题 | 提示切换 AI 出题模式 |
| 回答已回答问题 | 返回“该问题已回答或已跳过” |
| 追问超过 2 次 | 返回每道题最多追问 2 次 |
| 已完成会话重复结束 | 幂等返回报告 |
| Career Loop 回退阶段 | 抛出“不允许回退求职闭环阶段” |
