# 学习与成长

本模块是 Code Nest 的核心业务域之一，覆盖面试题库、掌握度追踪、闪卡、知识图谱、成长自动驾驶、计划打卡、学习小组等子功能。

## 子模块一览

| 子模块 | 包路径 | 核心 Service | 说明 |
|--------|--------|-------------|------|
| 面试题库 | `xiaou-interview` | `InterviewMasteryServiceImpl` | 题目 CRUD + 掌握度追踪 |
| 闪卡 | `xiaou-flashcard` | `FlashcardUserServiceImpl` | 间隔重复 + 复习调度 |
| 知识图谱 | `xiaou-knowledge` | `KnowledgeGraphServiceImpl` | 节点/边 CRUD + 图谱遍历 |
| 计划打卡 | `xiaou-plan` | `UserPlanServiceImpl` | 日常打卡 + 连续天数 |
| 成长自动驾驶 | `xiaou-plan` | `GrowthAutopilotServiceImpl` | 自动生成学习任务 + 成长分 |
| 学习驾驶舱 | `xiaou-application` | `LearningCockpitServiceImpl` | 跨模块聚合 + safeCall 降级 |
| 学习小组 | `xiaou-team` | `UserTeamServiceImpl` | 排行/贡献/讨论 |

## 面试题库与掌握度

### 题目结构

面试题目存储在 `interview_question` 表中，核心字段：

| 字段 | 说明 |
|------|------|
| `id` | 题目 ID |
| `title` | 题目标题 |
| `category` | 分类（如 Java、数据库、框架等） |
| `difficulty` | 难度等级 |
| `answer` | 参考答案 |
| `analysis` | 解析 |

### 掌握度追踪

用户对每道题的掌握度记录存储在 `interview_mastery_record` 表中，核心字段：

| 字段 | 说明 |
|------|------|
| `userId` | 用户 ID |
| `questionId` | 题目 ID |
| `masteryLevel` | 掌握度等级：0=未学, 1=了解, 2=理解, 3=掌握, 4=精通 |
| `reviewCount` | 复习次数 |
| `nextReviewDate` | 下次复习日期 |
| `lastReviewDate` | 上次复习日期 |

### 复习间隔算法

`InterviewMasteryServiceImpl.calculateNextReviewDays()` 实现间隔重复：

```text
基础间隔表（按 masteryLevel 索引）:
  level 1 → 1 天
  level 2 → 2 天
  level 3 → 4 天
  level 4 → 7 天

实际间隔 = 基础间隔 × 2^min(reviewCount, 5)
最大间隔 = 60 天
```

示例推算：

| 掌握度 | 复习次数 | 计算间隔 |
|--------|---------|---------|
| 1（了解） | 0 次 | 1 × 1 = 1 天 |
| 2（理解） | 1 次 | 2 × 2 = 4 天 |
| 3（掌握） | 3 次 | 4 × 8 = 32 天 |
| 4（精通） | 5 次 | 7 × 32 = 224 → 截断为 60 天 |

### 接口清单

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/user/interview/questions` | 分页查询题目列表 |
| GET | `/user/interview/question/{id}` | 获取题目详情 |
| POST | `/user/interview/mastery` | 更新掌握度 |
| GET | `/user/interview/mastery/stats` | 获取掌握度统计 |
| GET | `/user/interview/review/today` | 获取今日待复习题目 |
| GET | `/user/interview/review/overdue` | 获取逾期未复习题目 |
| GET | `/user/interview/categories` | 获取题目分类列表 |

## 闪卡

### 核心逻辑

`FlashcardUserServiceImpl` 实现基于间隔重复的闪卡复习：

| 方法 | 说明 |
|------|------|
| `createDeck()` | 创建卡组 |
| `addCard()` | 添加闪卡到卡组 |
| `reviewCard()` | 复习单张卡（记录结果，更新下次复习时间） |
| `getDueCards()` | 获取到期待复习卡片 |
| `getDeckStats()` | 获取卡组统计（总数/已掌握/待复习） |

### 复习调度

闪卡复习与面试题掌握度使用类似的间隔重复策略，但独立维护：

- 每张卡有独立的 `easeFactor`（易度因子），初始值 2.5
- 回答正确：`interval = interval × easeFactor`
- 回答错误：`interval = 1`，`easeFactor -= 0.2`（最低 1.3）

### 接口清单

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/user/flashcard/decks` | 创建卡组 |
| GET | `/user/flashcard/decks` | 获取卡组列表 |
| GET | `/user/flashcard/decks/{id}` | 获取卡组详情 |
| POST | `/user/flashcard/decks/{id}/cards` | 添加闪卡 |
| GET | `/user/flashcard/decks/{id}/cards/due` | 获取待复习卡片 |
| POST | `/user/flashcard/cards/{id}/review` | 提交复习结果 |
| DELETE | `/user/flashcard/decks/{id}` | 删除卡组 |

## 知识图谱

### 数据模型

`KnowledgeGraphServiceImpl` 管理知识节点和关系边：

| 实体 | 表 | 核心字段 |
|------|-----|---------|
| 知识节点 | `knowledge_node` | id, name, description, category |
| 知识边 | `knowledge_edge` | id, sourceId, targetId, relationType |

relationType 枚举值：

| 值 | 含义 |
|----|------|
| `PREREQUISITE` | 前置依赖 |
| `RELATED` | 关联知识 |
| `EXTENDS` | 扩展深入 |

### 图谱操作

| 方法 | 说明 |
|------|------|
| `createNode()` | 创建知识节点 |
| `createEdge()` | 创建关系边 |
| `getSubgraph()` | 获取子图（从指定节点出发，指定深度） |
| `getPath()` | 获取两节点间路径 |
| `getPrerequisites()` | 获取前置知识链 |

## 计划打卡

### 打卡机制

| 方法 | 说明 |
|------|------|
| `createPlan()` | 创建学习计划（设定目标和周期） |
| `checkIn()` | 每日打卡 |
| `getCheckInStatus()` | 获取打卡状态（连续天数、本周完成情况） |
| `getStreakDays()` | 计算连续打卡天数 |

连续天数计算逻辑：从今天往前逐日检查，遇到未打卡日即停止。

### 接口清单

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/user/plan/plans` | 创建学习计划 |
| GET | `/user/plan/plans` | 获取计划列表 |
| GET | `/user/plan/plans/{id}` | 获取计划详情 |
| POST | `/user/plan/plans/{id}/checkin` | 打卡 |
| GET | `/user/plan/plans/{id}/status` | 获取打卡状态 |
| PUT | `/user/plan/plans/{id}` | 更新计划 |
| DELETE | `/user/plan/plans/{id}` | 删除计划 |

## 成长自动驾驶

成长自动驾驶（Autopilot）是本模块最复杂的子系统，负责根据用户当前状态自动生成学习任务并计算成长分。

### 架构

```text
UserGrowthAutopilotController
  └─ GrowthAutopilotServiceImpl
       ├─ buildAutopilotDashboard()   → 聚合各模块状态
       ├─ buildGrowthScore()          → 计算综合成长分
       ├─ buildDailyTasks()           → 生成每日任务
       ├─ buildWeeklyTasks()          → 生成每周任务
       └─ buildWeaknesses()           → 识别薄弱项
```

### 成长分计算公式

`GrowthAutopilotServiceImpl.buildGrowthScore()` 实现：

```text
总分 = completionScore + activeScore + balanceScore + rankingScore + trendScore - riskPenalty
```

各分项计算规则：

| 分项 | 最大值 | 计算方式 |
|------|--------|---------|
| completionScore | ~58 | completionRate × 0.58（完成率权重最高） |
| activeScore | 20 | min(activeDays, 7) × 20 / 7 |
| balanceScore | 12 | 按最低完成率分档：≥ 90% → 12, ≥ 70% → 10, ≥ 50% → 7, ≥ 30% → 4, else → 1 |
| rankingScore | 10 | 上升 → 10, 持平 → 8, 在榜 → 6, else → 2 |
| trendScore | 10 | min(活跃天数, 7) × 10 / 7 |
| riskPenalty | 无上限 | 每个 severity != LOW 的 weakness 扣 min(impactScore / 12, 5) |

### 趋势分（Trend Score）计算

每日活跃分计算：

```text
dailyScore = interviewCount × 8 + flashcardCount × 2 + (积分打卡 ? 16 : 0)
```

近 7 天的 dailyScore 求和即为趋势分基础值。

### 模块权重配置

成长自动驾驶对不同学习模块设置权重，用于计算完成率和生成任务：

| 模块 | 通用权重 | 后端岗权重 | 前端岗权重 | 测试岗权重 |
|------|---------|-----------|-----------|-----------|
| OJ 刷题 | 0.26 | 0.33 | 0.20 | 0.18 |
| 面试题 | 0.22 | 0.24 | 0.27 | 0.28 |
| 闪卡 | 0.16 | 0.14 | 0.17 | 0.16 |
| 计划 | 0.14 | 0.12 | 0.13 | 0.14 |
| 模拟面试 | 0.14 | 0.12 | 0.15 | 0.16 |
| 积分 | 0.08 | 0.05 | 0.08 | 0.08 |

**目标岗位调整**：当用户设定目标岗位（后端/前端/测试）时，权重会按上表偏移，强化与岗位关联度更高的模块。

### 薄弱项识别

`buildWeaknesses()` 方法根据各模块完成率和排名，自动识别薄弱项：

| severity | 触发条件 |
|----------|---------|
| HIGH | 完成率 &lt; 30% 且持续 7 天无改善 |
| MEDIUM | 完成率 &lt; 50% 或排名持续下降 |
| LOW | 完成率 &lt; 70% 但有改善趋势 |

每个薄弱项包含 `impactScore`（影响分），用于成长分扣减计算。

### 聚合数据流

`buildAutopilotDashboard()` 跨模块聚合数据，数据来源：

| 数据 | 来源 Service | 方法 |
|------|-------------|------|
| OJ 完成数 | OJ 统计 | 获取用户提交统计 |
| 面试掌握度统计 | InterviewMasteryService | 获取掌握度统计 |
| 闪卡掌握数 | FlashcardUserService | 获取卡组统计 |
| 计划完成率 | UserPlanService | 获取打卡状态 |
| 积分余额 | PointsService | 获取积分余额 |
| 排名信息 | TeamRankService | 获取个人排名 |

### 接口清单

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/user/plan/autopilot/dashboard` | 获取自动驾驶仪表盘 |
| GET | `/user/plan/autopilot/tasks/daily` | 获取每日任务 |
| GET | `/user/plan/autopilot/tasks/weekly` | 获取每周任务 |
| GET | `/user/plan/autopilot/weaknesses` | 获取薄弱项 |
| POST | `/user/plan/autopilot/target-role` | 设置目标岗位 |
| GET | `/user/plan/autopilot/growth-score` | 获取成长分详情 |

## 学习驾驶舱

学习驾驶舱（Learning Cockpit）是跨模块聚合层，位于 `xiaou-application` 中，通过 safeCall 降级保证任一模块不可用时仪表盘仍可展示。

### 架构

```text
LearningCockpitController
  └─ LearningCockpitServiceImpl
       ├─ getOverview()           → 聚合总览数据
       ├─ safeCall() × 10         → 10 个跨模块调用点
       └─ buildDangerModules()    → 收集失败模块列表
```

### safeCall 降级明细

`LearningCockpitServiceImpl` 中的每个 safeCall 调用都有独立的降级默认值：

| 聚合项 | 调用目标 | 降级默认值 | 意义 |
|--------|---------|-----------|------|
| OJ 统计 | OJ 模块 | `{solvedCount: 0, submitCount: 0}` | 已解决/提交数归零 |
| 面试掌握度 | Interview 模块 | `{masteredCount: 0, totalCount: 0}` | 已掌握/总数归零 |
| 闪卡统计 | Flashcard 模块 | `{dueCount: 0, masteredCount: 0}` | 待复习/已掌握归零 |
| 积分余额 | Points 模块 | `{balance: 0}` | 积分归零 |
| 排名 | Team 模块 | `{ranking: 0, totalMembers: 0}` | 排名/总人数归零 |
| 计划状态 | Plan 模块 | `{streakDays: 0, weeklyCompletion: 0.0}` | 连续天数/周完成率归零 |
| 成长分 | Autopilot 模块 | `{totalScore: 0, level: "UNKNOWN"}` | 分数归零/等级未知 |
| 知识图谱 | Knowledge 模块 | `{nodeCount: 0, edgeCount: 0}` | 节点/边数归零 |
| 近期动态 | Activity 模块 | `emptyList` | 动态为空列表 |
| 学习建议 | AI 建议 | `emptyList` | 建议为空列表 |

当某个 safeCall 失败时，该模块出现在 `dangerModules` 列表中，前端可据此显示"数据暂时不可用"而非空白或报错。

### 总览响应结构

`LearningCockpitOverviewResponse` 核心字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `ojStats` | OjStats | OJ 刷题统计 |
| `interviewStats` | InterviewStats | 面试掌握度统计 |
| `flashcardStats` | FlashcardStats | 闪卡统计 |
| `pointsBalance` | Integer | 积分余额 |
| `ranking` | RankingInfo | 排名信息 |
| `planStatus` | PlanStatus | 计划打卡状态 |
| `growthScore` | GrowthScoreInfo | 成长分信息 |
| `knowledgeStats` | KnowledgeStats | 知识图谱统计 |
| `recentActivities` | List&lit;ActivityItem&gt; | 近期动态 |
| `suggestions` | List&lit;SuggestionItem&gt; | 学习建议 |
| `dangerModules` | List&lit;String&gt; | 降级模块列表 |

### 接口清单

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/user/learning/cockpit/overview` | 获取驾驶舱总览 |
| GET | `/user/learning/cockpit/oj-stats` | 获取 OJ 统计 |
| GET | `/user/learning/cockpit/interview-stats` | 获取面试统计 |
| GET | `/user/learning/cockpit/flashcard-stats` | 获取闪卡统计 |
| GET | `/user/learning/cockpit/growth-trend` | 获取成长趋势 |

## 学习小组

学习小组功能在 [计划与团队](/modules/plan-team) 中详细说明。与学习成长相关的核心交互：

| 交互 | 说明 |
|------|------|
| 打卡贡献 | 成员每日打卡增加团队贡献值 |
| 排行榜 | 团队内/团队间学习排行 |
| 讨论区 | 小组内讨论面试题、学习资源 |
| 共享卡组 | 团队内共享闪卡组 |

## 跨模块数据流

### 学习行为 → 积分

| 学习行为 | 积分规则 | 触发方式 |
|---------|---------|---------|
| 每日打卡 | 固定积分 | `UserPlanService.checkIn()` → 积分事件 |
| 面试题掌握度提升 | 按掌握度等级 | `InterviewMasteryService.updateMastery()` → 积分事件 |
| 闪卡复习完成 | 按每日复习量 | `FlashcardUserService.reviewCard()` → 积分事件 |
| OJ 首次 AC | 首次 AC 积分 | `OJJudgeService` → 积分回流 |

### 数据聚合路径

```text
学习驾驶舱总览
  ├─ LearningCockpitServiceImpl.getOverview()
  │   ├─ safeCall(OJ模块)          → OjStats
  │   ├─ safeCall(Interview模块)   → InterviewStats
  │   ├─ safeCall(Flashcard模块)   → FlashcardStats
  │   ├─ safeCall(Points模块)      → PointsBalance
  │   ├─ safeCall(Team模块)        → RankingInfo
  │   ├─ safeCall(Plan模块)        → PlanStatus
  │   ├─ safeCall(Autopilot模块)   → GrowthScoreInfo
  │   ├─ safeCall(Knowledge模块)   → KnowledgeStats
  │   ├─ safeCall(Activity模块)    → RecentActivities
  │   └─ safeCall(AI模块)          → Suggestions
  └─ 聚合 → LearningCockpitOverviewResponse + dangerModules
```

### 成长分 → 驾驶舱 → 前端

```text
GrowthAutopilotServiceImpl.buildGrowthScore()
  → GrowthScoreInfo (totalScore, level, breakdown)
  → LearningCockpitServiceImpl.safeCall()
  → LearningCockpitOverviewResponse.growthScore
  → 前端驾驶舱页面
```

## 新增功能自检

新增学习成长相关功能时，至少验证：

| 检查项 | 预期 |
|--------|------|
| 掌握度更新 | 正确计算下次复习日期，间隔符合算法 |
| 闪卡复习 | easeFactor 不低于 1.3，间隔合理增长 |
| 打卡连续天数 | 中断后归零，补签仅计当天 |
| 成长分 | 各分项计算正确，riskPenalty 不使总分为负 |
| safeCall | 单模块失败不影响其他模块数据展示 |
| 积分回流 | 学习行为产生的积分事件正确触发 |
| 目标岗位 | 切换岗位后权重偏移生效 |
| 薄弱项 | 识别逻辑与 severity 分档一致 |

## 推荐阅读

| 继续看什么 | 为什么 |
|-----------|--------|
| [计划与团队](/modules/plan-team) | 学习小组排行与贡献机制 |
| [积分与抽奖](/modules/points) | 积分发放规则与回流 |
| [OJ 判题](/modules/oj) | OJ 刷题统计与首次 AC 积分 |
| [AI Runtime](/modules/ai-runtime) | AI 学习建议与结构化输出 |
| [API 路由索引](/reference/api-routes) | 完整接口清单 |
| [权限与安全边界](/guide/security-boundaries) | 学习数据属于当前用户，需从 Token 取 ID |
