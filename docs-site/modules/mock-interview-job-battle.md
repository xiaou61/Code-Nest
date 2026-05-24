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

## 验证清单

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

---

## 深度拆解

### 一、模拟面试创建深度分析

`MockInterviewServiceImpl.createInterview` 完整流程：

```text
1. validateCreateRequest → 校验 direction 非空、level 非空、questionCount >= 1
2. directionMapper.selectByCode → 校验方向存在且 status=1
3. sessionMapper.selectOngoingByUserId → 拒绝重复进行中面试
4. questionMode 默认 AI(2)
5. 创建 MockInterviewSession (status=ONGOING, currentQuestionOrder=0)
6. sessionMapper.insert(session)
7. 出题:
   ├─ LOCAL(1): questionSetIds 非空 → selectQuestionsFromSets; 否则 → selectQuestions(direction, level)
   │   └─ questions 为空 → 抛"没有找到符合条件的题目，请尝试AI出题模式"
   └─ AI(2): questionSelectorService.generateQuestionsByAI(direction, level, count)
       └─ generatedQuestions 为空 → 抛"AI出题失败"
8. 构建 MockInterviewQA 列表 (status=PENDING, questionType=MAIN)
9. qaMapper.batchInsert(qaList)
10. updateUserStatsOnCreate → incrementInterviewCount
11. return buildSessionResponse(session)
```

**出题模式分支**：本地题库模式下，用户可通过 `questionSetIds` 指定题单，不指定则按方向+级别自动抽题。AI 模式下调用 `QuestionSelectorService.generateQuestionsByAI`，由 `xiaou-ai` 的 GraphRunner 编排生成。

**并发创建保护**：`selectOngoingByUserId` 查询进行中会话，但 **没有数据库唯一约束保护**——极端并发下可能突破检查创建两个 ONGOING 会话。

### 二、AI 评价与本地兜底深度分析

`AIInterviewerServiceImpl.evaluateAnswer` 的双层策略：

```text
1. 获取面试配置 → style + level
2. try: aiInterviewService.evaluateAnswer(direction, level, style, question, answer, followUpCount)
3. if AI 返回 fallback=true → generateLocalEvaluation
4. if AI 抛异常 → generateLocalEvaluation
5. if AI 成功 → convertToAIEvaluationResult (含风格分数调整)
```

**本地兜底评分 `generateLocalEvaluation`**：

| 答案长度 | baseScore |
| --- | --- |
| 空白 | 0 |
| < 50 字 | 4 |
| 50-149 字 | 6 |
| 150-299 字 | 7 |
| >= 300 字 | 8 |

**风格调整**：`baseScore + style.getScoreAdjustment()`，温和型 +1、标准型 0、压力型 -1，clamp [0, 10]。

**追问决策**：`baseScore >= 4 && followUpCount < 2 && random < followUpRate`
- 温和型 followUpRate=0.3
- 标准型 followUpRate=0.5
- 压力型 followUpRate=0.7

**本地追问模板**（3 档）：

| baseScore | 追问内容 |
| --- | --- |
| >= 7 | "你的回答很好，请进一步说明一下具体的实现原理是什么？" |
| 5-6 | "你提到了一些要点，能否举个具体的例子来说明你的理解？" |
| < 5 | "这个问题的核心概念是什么？请再思考一下。" |

**AI 成功时的分数调整**：`score + style.getScoreAdjustment()`，与本地兜底使用相同的风格偏移量，保证 AI 和本地评分在同一量纲上。

### 三、追问机制深度分析

**自动追问**（submitAnswer 中）：

```text
1. AI 返回 nextAction="followUp" + followUpQuestion 非空
2. 创建 MockInterviewQA:
   ├─ questionType = FOLLOW_UP(2)
   ├─ parentQaId = 当前题是 MAIN ? 当前 qaId : 当前题的 parentQaId
   └─ questionOrder = 继承主问题的 questionOrder
3. response.nextAction = "followUp"
4. response.followUpQuestion = {qaId, questionContent, questionType}
```

**手动追问**（requestFollowUp）：

```text
1. 校验会话进行中 + QA 存在且已回答
2. actualParentId = MAIN ? qaId : parentQaId
3. existingFollowUps = selectFollowUpsByParentId(actualParentId)
4. existingFollowUps.size() >= 2 → 抛"每道题最多可以追问2次"
5. aiInterviewerService.generateFollowUpQuestion → followUpQuestion
6. 创建追问 QA 并返回
```

**追问上限**：自动追问和手动追问共享同一个 parentQaId 下的追问列表，但 **自动追问不检查 2 次上限**——AI 连续返回 followUp 可以无限追问。只有手动追问才检查 `existingFollowUps.size() >= 2`。

### 四、评分计算与维度分深度分析

`calculateAndUpdateScores`：

```text
1. qaList = selectBySessionId(sessionId) → 所有 QA（含追问）
2. totalScore = Σ qa.score (score != null)
3. count = 有分数的 QA 数
4. avgScore = totalScore * 10 / count  (0-10 分 → 0-100 分)
5. totalScore = min(avgScore, 100)
6. 维度分:
   ├─ knowledgeScore = min(avgScore + 5, 100)
   ├─ depthScore = max(avgScore - 5, 0)
   ├─ expressionScore = avgScore
   └─ adaptabilityScore = avgScore
```

**维度分是硬编码偏移**：知识分 +5、深度分 -5、表达和应变等于平均分。这是简化实现，注释说"实际可以通过AI更精细地评估"。

**追问影响总分**：`selectBySessionId` 返回所有 QA 包括追问，追问的 score 也计入 totalScore 和 count。如果追问得分普遍高于主问题，会拉高总分；反之拉低。

**用户统计更新 `updateUserStatsOnComplete`**：

```text
1. completedInterviews += 1
2. newAvg = 本次面试平均分
3. totalAvg = (旧avg * (completedInterviews-1) + newAvg) / completedInterviews
4. highestScore = max(旧, 本次totalScore)
5. totalQuestions += answered, correctQuestions += highScore (>=7 分的题数)
6. 连续天数: 昨天 → streak+1; 今天 → 不变; 其他 → 重置为1
7. maxStreak = max(旧, streak)
```

**平均分增量计算**：`旧avg * (n-1) + newAvg) / n` 是精确的增量公式，但 **浮点精度** 可能导致 avgScore 随时间漂移。

### 五、求职作战台匹配引擎深度分析

`JobBattleServiceImpl.runMatchEngine`：

```text
1. 校验 targets 非空、1-10 个
2. for each target:
   ├─ aiJobBattleService.analyzeTarget → analysisResult (JD解析+简历匹配)
   └─ buildTargetScore → TargetScore
3. 排序: engineScore DESC → estimatedPassRate DESC
4. 设置 rank = 1, 2, 3...
5. bestScore = ranking[0].engineScore
6. averageScore = avg(所有 engineScore)
7. fallbackCount = count(fallback=true)
8. nextActions = buildNextActions(ranking, fallbackCount)
9. saveMatchRecord → 落库 (catch 不阻断)
10. pushLoopEvent → RESUME_MATCHED
```

**引擎评分公式 `calculateEngineScore`**：

```text
weighted = overall * 0.55 + skill * 0.20 + project * 0.15 + architecture * 0.07 + communication * 0.03
gapPenalty = p0GapCount * 4 + p1GapCount * 2 + max(0, totalGapCount - 5)
keywordPenalty = min(8, missingCount)
engineScore = clamp(weighted - gapPenalty - keywordPenalty, 0, 100)
```

**权重分配**：overall(55%) > skill(20%) > project(15%) > architecture(7%) > communication(3%)。Gap 惩罚中 P0 每个扣 4 分、P1 每个扣 2 分、超出 5 个的 gap 每个扣 1 分。缺失关键词每个扣 1 分，最多扣 8 分。

**降级维度分兜底**：当 AI 返回的 dimensionScores 缺少某个维度时，使用基于 overall 的兜底值：

| 维度 | 兜底值 |
| --- | --- |
| skillMatch | overall |
| projectDepth | max(40, overall - 8) |
| architectureAbility | max(35, overall - 12) |
| communicationClarity | max(45, overall + 5) |

### 六、Career Loop 状态机深度分析

`CareerLoopStateMachine.next`：

```text
1. current 为 null → 默认 INIT
2. target 为 null → 返回 current (幂等)
3. current == target → 返回 current (幂等)
4. target.order < current.order → 抛"不允许回退求职闭环阶段"
5. return target (允许向前跳级)
```

**阶段顺序**：INIT(0) → JD_PARSED(1) → RESUME_MATCHED(2) → PLAN_READY(3) → PLAN_EXECUTING(4) → INTERVIEW_DONE(5) → REVIEWED(6) → OFFER_TRACKING(7)

**允许跳级**：从 INIT 直接跳到 PLAN_READY 是合法的——不需要逐步推进。这意味着用户可以先做行动计划，再补 JD 解析。

**健康分递增**：

| 阶段 | gain |
| --- | --- |
| JD_PARSED / RESUME_MATCHED | +4 |
| PLAN_READY | +6 |
| PLAN_EXECUTING / INTERVIEW_DONE | +8 |
| REVIEWED | +10 |
| OFFER_TRACKING | +12 |

初始 healthScore=60，每次阶段推进累加 gain，clamp [0, 100]。**但只在阶段变更时增加**——同一阶段内多次 sync 不增加健康分。

**行动项重置**：每次阶段推进，`resetActionsByStage` 先 `deleteTodoBySessionId`（删所有 todo 项），再按阶段重建。**已完成的 done 项不删除**——但 `deleteTodoBySessionId` 删的是所有 todo，done 项由 `markDoneById` 改状态而非删除。

**自动推断阶段 `inferTargetStage`**：

```text
reviewCount > 0 → REVIEWED
mockCount > 0 → INTERVIEW_DONE
planProgress >= 20 → PLAN_EXECUTING
否则 → null (不推进)
```

### 七、深度发现与坑点

#### 7.1 确认 BUG

| 编号 | BUG | 位置 | 说明 |
| --- | --- | --- | --- |
| BUG-1 | 自动追问无上限 | `MockInterviewServiceImpl.submitAnswer:278` | AI 连续返回 followUp 可无限追问，只有手动追问检查 2 次上限 |
| BUG-2 | 维度分硬编码偏移 | `calculateAndUpdateScores:702` | knowledge=avg+5, depth=avg-5, 不反映真实能力维度 |
| BUG-3 | 追问计入总分 | `calculateAndUpdateScores:679` | selectBySessionId 含追问，追问分数影响总分均值 |
| BUG-4 | 并发创建面试无保护 | `createInterview:123` | selectOngoingByUserId 无 DB 唯一约束，并发可创建多个 ONGOING |
| BUG-5 | 平均分浮点漂移 | `updateUserStatsOnComplete:743` | 增量计算 avgScore，浮点精度随面试次数累积 |
| BUG-6 | 删除面试不清理统计 | `deleteInterview:524` | 删除 session+qa 但不回减 userStats 中的计数和平均分 |
| BUG-7 | 行动项重置删 todo 不删 done | `resetActionsByStage:295` | deleteTodoBySessionId 只删 todo 状态，done 项保留但可能过时 |

#### 7.2 设计风险

| 编号 | 风险 | 说明 |
| --- | --- | --- |
| RISK-1 | 本地兜底评分按长度 | generateLocalEvaluation 仅按答案长度评分，300 字和 300 字废话同分 |
| RISK-2 | AI 评价无超时控制 | aiInterviewService.evaluateAnswer 无显式超时，AI 慢会阻塞答题 |
| RISK-3 | 匹配引擎串行调用 AI | 10 个目标岗位逐个调用 analyzeTarget，无并行 |
| RISK-4 | 计划落库失败静默 | savePlanRecord catch 后只 log.error，用户看不到落库失败 |
| RISK-5 | Career Loop 单会话 | 每用户只一个 active session，无法并行跟踪多个求职方向 |
| RISK-6 | 健康分只增不减 | 即使后续行为负面，healthScore 也不会下降 |

#### 7.3 架构设计亮点

| 编号 | 亮点 | 说明 |
| --- | --- | --- |
| H-1 | AI + 本地双层评价 | AI 失败/降级时无缝切换本地评分，保证面试不中断 |
| H-2 | 风格影响评分和追问 | 温和/标准/压力三档，分数偏移 + 追问概率，模拟真实面试体验 |
| H-3 | 主问题 + 追问树结构 | parentQaId 关联追问，报告页可还原完整对话树 |
| H-4 | 引擎评分公式 | 5 维加权 + gap 惩罚 + 关键词惩罚，科学量化岗位匹配度 |
| H-5 | Career Loop 状态机 | 单向推进 + 允许跳级 + 健康分递增，驱动求职节奏 |
| H-6 | 闭环事件推送 | 所有 Job Battle 动作自动推事件，用户无需手动同步阶段 |
| H-7 | 行动项按阶段生成 | 每个阶段有专属待办，推进后自动重建，引导用户下一步 |
| H-8 | 计划落库容错 | savePlanRecord 失败不阻断 AI 返回，用户体验优先 |
| H-9 | 匹配引擎 nextActions | 自动生成优先冲刺、补齐差距、补全关键词等行动建议 |
| H-10 | 降级维度分兜底 | AI 维度缺失时用基于 overall 的合理默认值，不返回 null |

#### 7.4 源码导航速查

| 想了解 | 读什么 |
| --- | --- |
| 创建面试 | `MockInterviewServiceImpl.java` — createInterview + validateCreateRequest |
| 提交答案 | `MockInterviewServiceImpl.java` — submitAnswer + AI 评价 + 自动追问 |
| 手动追问 | `MockInterviewServiceImpl.java` — requestFollowUp + 2 次上限 |
| 评分计算 | `MockInterviewServiceImpl.java` — calculateAndUpdateScores + 维度分偏移 |
| 结束面试 | `MockInterviewServiceImpl.java` — endInterview + pushLoopInterviewDone |
| AI 总结 | `MockInterviewServiceImpl.java` — generateSummary |
| AI 评价 | `AIInterviewerServiceImpl.java` — evaluateAnswer + 本地兜底 |
| 本地评分 | `AIInterviewerServiceImpl.java` — generateLocalEvaluation (长度→分数) |
| 追问生成 | `AIInterviewerServiceImpl.java` — generateFollowUpQuestion + 本地追问模板 |
| 匹配引擎 | `JobBattleServiceImpl.java` — runMatchEngine + calculateEngineScore |
| 引擎评分公式 | `JobBattleServiceImpl.java` — 0.55/0.20/0.15/0.07/0.03 权重 + gap 惩罚 |
| JD 解析 | `JobBattleServiceImpl.java` — parseJd + pushLoopEvent |
| 简历匹配 | `JobBattleServiceImpl.java` — matchResume + pushLoopEvent |
| 行动计划 | `JobBattleServiceImpl.java` — generatePlan + savePlanRecord |
| 面试复盘 | `JobBattleServiceImpl.java` — reviewInterview |
| 闭环状态机 | `CareerLoopStateMachine.java` — next (单向+跳级) |
| 闭环服务 | `CareerLoopServiceImpl.java` — onEvent + mergeSnapshot + nextHealthScore |
| 闭环启动 | `CareerLoopServiceImpl.java` — start + ensureActiveSession |
| 行动项 | `CareerLoopServiceImpl.java` — resetActionsByStage + buildStageActions |
| 会话域 | `MockInterviewSession.java` — 4 维度分 + aiSummary + aiSuggestion |
| QA 域 | `MockInterviewQA.java` — questionType + parentQaId + score + aiFeedback |
