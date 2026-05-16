# 模拟面试与求职作战台

该模块把 AI 模拟面试、JD 解析、简历匹配、行动计划、面试复盘和求职闭环串成一条求职增长链路。

## 功能入口

| 端 | 页面 |
| --- | --- |
| 用户端 | `/mock-interview`、`/mock-interview/config`、`/mock-interview/session`、`/mock-interview/report`、`/mock-interview/history` |
| 用户端 | `/job-battle`、`/job-match-engine`、`/career-loop` |
| 管理端 | `/mock-interview/sessions`、`/mock-interview/directions`、`/system/ai-config`、`/system/ai-governance` |
| 后端模块 | `xiaou-mock-interview`、`xiaou-ai` |

## 推荐学习顺序

这个模块横跨面试、求职和 AI 编排，新人建议这样看：

1. 先看模拟面试会话，理解 `session -> QA -> report` 的主链路。
2. 再看题目来源，区分本地题库模式和 AI 出题模式。
3. 接着看回答评分和追问，理解 AI 失败时为什么有本地兜底。
4. 然后看 Job Battle，理解 JD 解析、简历匹配、计划生成和复盘如何推事件。
5. 最后看 Career Loop，理解这些事件如何推进求职阶段和行动项。

## 源码地图

| 目标 | 文件 |
| --- | --- |
| 模拟面试用户接口 | `xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/controller/MockInterviewController.java` |
| 面试会话接口 | `xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/controller/MockInterviewSessionController.java` |
| 后台面试运营 | `xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/controller/admin/AdminMockInterviewController.java` |
| 面试主服务 | `xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/service/impl/MockInterviewServiceImpl.java` |
| 题目选择 | `xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/service/impl/QuestionSelectorServiceImpl.java` |
| AI 面试官 | `xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/service/impl/AIInterviewerServiceImpl.java` |
| 求职作战台 | `xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/service/impl/JobBattleServiceImpl.java` |
| 求职闭环 | `xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/service/impl/CareerLoopServiceImpl.java` |
| 阶段状态机 | `xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/service/CareerLoopStateMachine.java` |
| AI 能力目录 | `xiaou-ai/src/main/java/com/xiaou/ai` |

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

## 配置枚举

这些枚举是前后端联调时最容易写错的字段：

| 维度 | 枚举 | 说明 |
| --- | --- | --- |
| 出题模式 | `1 LOCAL`、`2 AI` | 创建面试时未传默认使用 AI 出题 |
| 面试级别 | `1 JUNIOR`、`2 MIDDLE`、`3 SENIOR` | 对应初级、中级、高级 |
| 面试类型 | `1 TECHNICAL`、`2 COMPREHENSIVE`、`3 SPECIALIZED` | 技术面、综合面、专项突破 |
| AI 风格 | `1 GENTLE`、`2 STANDARD`、`3 PRESSURE` | 温和、标准、压力 |
| 会话状态 | `0 ONGOING`、`1 COMPLETED`、`2 INTERRUPTED` | 只有进行中允许作答 |
| QA 状态 | `0 PENDING`、`1 ANSWERED`、`2 SKIPPED` | 只有待回答可以提交答案 |

AI 风格会影响本地兜底评分和追问概率：温和型加 1 分、追问率 0.3；标准型不调整、追问率 0.5；压力型减 1 分、追问率 0.7。

## 核心流程

1. 用户选择面试方向和配置。
2. 系统创建面试会话。
3. AI 生成问题，用户作答。
4. AI 对答案评分、反馈并生成追问。
5. 用户结束面试后生成报告。
6. 报告可转学习资产或进入求职闭环。
7. 求职作战台读取 JD、简历和历史结果生成行动计划。

## 一次模拟面试如何完成

| 步骤 | 说明 |
| --- | --- |
| 读取配置 | `/user/mock-interview/config` 返回方向、级别、类型、风格和题数选项 |
| 创建会话 | `createInterview` 校验方向启用、题数合法、用户没有进行中会话 |
| 生成题目 | 本地题库按方向/级别抽题，AI 模式调用 AI 生成；本地无题会提示切换 AI |
| 写入 QA | 每道主问题写入 `mock_interview_qa`，状态为 `PENDING` |
| 开始面试 | `startInterview` 设置开始时间和当前题序 |
| 提交答案 | 只有 `PENDING` 可回答，AI 评分后保存分数和反馈 JSON |
| 自动追问 | AI 返回 `followUp` 时创建追问 QA |
| 结束面试 | 计算总分、维度分、报告，并推送 `INTERVIEW_DONE` 事件 |

会话时长默认按题数估算：`questionCount * 4` 分钟。这个字段更像产品体验上的预计时长，不等同于实际答题耗时。

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

## 求职作战台主线

Job Battle 不是单独的 AI 页面，它的每个关键动作都会推动 Career Loop：

| 动作 | 接口 | 推进阶段 |
| --- | --- | --- |
| JD 解析 | `/user/job-battle/jd/parse` | `JD_PARSED` |
| 简历匹配 | `/user/job-battle/resume/match` | `RESUME_MATCHED` |
| 行动计划生成 | `/user/job-battle/plan/generate` | `PLAN_READY` |
| 匹配引擎运行 | `/user/job-battle/match-engine/run` | 通常复用简历匹配结果 |
| 面试复盘 | `/user/job-battle/interview/review` | `REVIEWED` |

匹配引擎最多处理 10 个目标岗位，排序先看引擎评分，再看预估通过率。行动计划会保存到 `job_battle_plan_record`，即使落库失败也不会阻断 AI 返回，排查历史记录缺失时要先看服务日志。

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

## 核心数据表

| 表 | 作用 | 排查时重点看 |
| --- | --- | --- |
| `mock_interview_direction` | 面试方向配置 | `status` 是否启用 |
| `mock_interview_session` | 面试会话 | 状态、当前题序、总分、维度分、AI 总结 |
| `mock_interview_qa` | 主问题和追问 | `question_type`、`parent_qa_id`、`status`、评分和反馈 |
| `mock_interview_user_stats` | 用户面试统计 | 总场次、平均分、题目数 |
| `job_battle_plan_record` | 行动计划历史 | AI 返回内容、计划天数、生成时间 |
| `career_loop_session` | 当前求职闭环 | 当前阶段、健康分、周投入小时 |
| `career_loop_stage_log` | 阶段变更记录 | 阶段推进来源和时间 |
| `career_loop_action` | 当前阶段待办 | 是否完成、行动类型 |
| `career_loop_snapshot` | 求职画像快照 | 计划进度、面试次数、复盘次数和风险 |

## 常见坑

| 问题 | 常见原因 | 排查方式 |
| --- | --- | --- |
| 创建面试失败 | 用户已有 `ONGOING` 会话 | 查 `mock_interview_session.status` |
| 本地题库模式无题 | 方向、级别或题单没有可用题 | 切换 AI 模式或补充题库 |
| AI 出题失败 | AI 服务异常且没有生成结果 | 看 `QuestionSelectorServiceImpl` 日志 |
| 回答接口失败 | QA 已回答或已跳过 | 查 `mock_interview_qa.status` |
| 追问数量超限 | 每道主问题最多 2 个手动追问 | 查同一 `parent_qa_id` 下追问数量 |
| 报告没有 AI 总结 | 总结需要手动调用 `/{id}/summary` | 查会话的 AI 总结字段 |
| Career Loop 不推进 | 事件目标阶段为空或试图回退 | 查 `career_loop_stage_log` 和状态机异常 |
| 行动项突然被重置 | 阶段推进后会按阶段重建待办 | 这是当前设计，不是数据丢失 |

## 验证点

| 场景 | 预期 |
| --- | --- |
| 同一用户重复创建面试 | 第二次创建被拒绝，提示先完成当前面试 |
| 本地题库模式无题 | 提示切换 AI 出题模式 |
| 回答已回答问题 | 返回“该问题已回答或已跳过” |
| 追问超过 2 次 | 返回每道题最多追问 2 次 |
| 已完成会话重复结束 | 幂等返回报告 |
| Career Loop 回退阶段 | 抛出“不允许回退求职闭环阶段” |
| JD 解析成功 | Career Loop 推进到 `JD_PARSED` |
| 生成行动计划 | 写入计划历史并推进到 `PLAN_READY` |
| 完成面试复盘 | Career Loop 推进到 `REVIEWED` |
