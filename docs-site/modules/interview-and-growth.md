# 题库与成长闭环

题库与成长闭环把面试题、OJ、模拟面试、求职作战台、学习资产、闪卡、计划、学习小组、知识图谱和积分串在一起。它的目标不是堆功能，而是让用户从“看内容”走向“有目标、有反馈、有复习、有沉淀”的能力提升。

如果你是第一次学习 Code Nest 的业务，可以先读这篇导览，再进入每个模块的详细文档。

## 推荐学习顺序

这篇是学习成长域的总入口，建议按“内容 -> 练习 -> 反馈 -> 复习 -> 计划”的顺序读：

1. 先读面试题库，理解最基础的题目、题单、掌握度和复习记录。
2. 再读 OJ，理解代码提交如何变成可统计的 AC、排名和积分奖励。
3. 接着读模拟面试和 Job Battle，理解 AI 如何把问答、JD、简历和计划串起来。
4. 然后读学习资产、闪卡和知识图谱，理解内容如何沉淀为可复习材料。
5. 最后读计划、学习小组和学习驾驶舱，理解行为数据如何被聚合成成长反馈。

## 源码地图

| 能力 | 源码入口 |
| --- | --- |
| 学习驾驶舱 | `xiaou-application/src/main/java/com/xiaou/web/learning/controller/LearningCockpitController.java` |
| 驾驶舱聚合服务 | `xiaou-application/src/main/java/com/xiaou/web/learning/service` |
| 成长自动驾驶 | `xiaou-plan/src/main/java/com/xiaou/plan` |
| 面试题库 | `xiaou-interview/src/main/java/com/xiaou/interview` |
| OJ 判题 | `xiaou-oj/src/main/java/com/xiaou/oj` |
| 模拟面试和 Career Loop | `xiaou-mock-interview/src/main/java/com/xiaou/mockinterview` |
| 学习资产转化 | `xiaou-learning-asset/src/main/java/com/xiaou/learningasset` |
| 闪卡学习 | `xiaou-flashcard/src/main/java/com/xiaou/flashcard` |
| 知识图谱 | `xiaou-knowledge/src/main/java/com/xiaou/knowledge` |
| 用户端路由 | `vue3-user-front/src/router/index.js` |

## 功能地图

| 功能 | 用户端入口 | 管理端入口 | 后端模块 | 深入文档 |
| --- | --- | --- | --- | --- |
| 面试题库 | `/interview` | `/interview/*` | `xiaou-interview` | [面试题库](/modules/interview) |
| OJ 判题 | `/oj/*` | `/oj/*` | `xiaou-oj` | [OJ 判题系统](/modules/oj) |
| AI 模拟面试 | `/mock-interview/*` | `/mock-interview/*` | `xiaou-mock-interview`、`xiaou-ai` | [模拟面试与求职作战台](/modules/mock-interview-job-battle) |
| 求职作战台 | `/job-battle`、`/job-match-engine` | `/system/ai-config`、`/system/ai-governance` | `xiaou-mock-interview`、`xiaou-ai` | [模拟面试与求职作战台](/modules/mock-interview-job-battle) |
| Career Loop | `/career-loop` | AI 治理相关页面 | `xiaou-mock-interview` | [模拟面试与求职作战台](/modules/mock-interview-job-battle) |
| 学习驾驶舱 | `/learning-cockpit` | 暂无独立后台 | `xiaou-application`、`xiaou-plan`、多个业务模块 | 本页 |
| 成长自动驾驶 | `/learning-cockpit` 子视图 | 暂无独立后台 | `xiaou-plan` | [计划与学习小组](/modules/plan-team) |
| 学习资产 | `/learning-assets` | `/learning-assets/*` | `xiaou-learning-asset` | [学习资产](/modules/learning-assets) |
| 闪卡 | `/flashcard/*` | 后续可扩展审核 | `xiaou-flashcard` | [闪卡](/modules/flashcard) |
| 计划打卡 | `/plan` | 后续可扩展统计 | `xiaou-plan` | [计划与学习小组](/modules/plan-team) |
| 学习小组 | `/team/*` | 后续可扩展运营 | `xiaou-team` | [计划与学习小组](/modules/plan-team) |
| 知识图谱 | `/knowledge` | `/knowledge/maps` | `xiaou-knowledge` | [知识图谱](/modules/knowledge) |

## 一条完整学习路径

一个典型用户可以这样使用：

1. 在 `/interview` 选题单，刷基础面试题。
2. 对每道题标记掌握度，系统生成复习时间。
3. 在 `/oj` 做算法题，用 AC 数和排名检验编码能力。
4. 在 `/mock-interview` 做 AI 模拟面试，暴露表达和知识短板。
5. 把模拟面试报告、博客、社区帖子等内容转成学习资产。
6. 把高频知识点转成闪卡或知识图谱节点。
7. 在 `/plan` 建立每日计划，保持学习节奏。
8. 在 `/team` 加入学习小组，用任务、打卡和排行增加外部反馈。
9. 在 `/job-battle` 解析 JD、匹配简历、生成求职计划。
10. 在 `/learning-cockpit` 查看成长分、能力雷达、短板和今日任务。
11. 用成长自动驾驶把本周目标拆成具体任务。

这条路径里，内容消费只是起点。真正重要的是后面的记录、复习、反馈和再计划。

## 学习驾驶舱聚合了什么

学习驾驶舱入口是 `/user/learning-cockpit/overview`，控制器位于 `xiaou-application/src/main/java/com/xiaou/web/learning/controller/LearningCockpitController.java`。

`LearningCockpitService` 会聚合多个模块：

| 来源 | 数据 |
| --- | --- |
| 计划 | 今日完成数、待完成数、本周打卡数 |
| 积分 | 当前积分、今日是否签到、连续签到天数 |
| 闪卡 | 今日学习、连续学习、近 21 天热力图 |
| 面试题库 | 复习统计、年度热力图、总学习数 |
| OJ | 提交/AC 统计、周榜、总榜 |
| Career Loop | 当前求职阶段、行动项 |
| 排名快照 | 本周排名和历史排名变化 |

服务里大量使用 `safeCall`。这意味着某个模块接口异常时，驾驶舱会用默认值降级，不会让整个概览页崩掉。这个设计适合聚合页：聚合页追求“尽量展示”，不能被单个模块拖死。

## 成长分怎么理解

成长分不是简单的“积分”，而是学习行为的综合评分。当前口径主要来自：

| 维度 | 含义 |
| --- | --- |
| 完成率 | 本周目标完成情况 |
| 活跃天数 | 本周是否持续学习 |
| 均衡性 | OJ、题库、闪卡、计划、积分是否过于偏科 |
| 排名表现 | OJ 周榜、总榜和上周变化 |
| 趋势 | 近几天活跃曲线 |
| 风险扣分 | 逾期复习、计划未完成、排名下滑等 |

因此成长分适合用来回答“我这周学习状态怎么样”，不适合当作严格的业务结算分。

## 成长自动驾驶

成长自动驾驶在 `xiaou-plan` 模块，用户端 API 在 `/user/plan/autopilot/**`。

| 能力 | 接口 |
| --- | --- |
| 查看仪表盘 | `GET /user/plan/autopilot/dashboard` |
| 生成周计划 | `POST /user/plan/autopilot/generate` |
| 重新规划 | `POST /user/plan/autopilot/replan` |
| 完成单个任务 | `POST /user/plan/autopilot/tasks/{taskId}/complete` |
| 完成今日任务 | `POST /user/plan/autopilot/tasks/today/complete` |
| 推迟任务 | `POST /user/plan/autopilot/tasks/{taskId}/postpone` |

它和普通计划的区别是：普通计划由用户自己写任务，自动驾驶会根据目标岗位、每周可投入时间和模块模板生成任务，再把任务状态推进为 `todo`、`done`、`missed`。

## 各模块如何互相喂数据

| 起点 | 终点 | 数据/动作 |
| --- | --- | --- |
| 面试题库 | 学习驾驶舱 | 学习数、复习数、热力图、待复习 |
| OJ | 学习驾驶舱 | 提交、AC、排名 |
| 闪卡 | 学习驾驶舱 | 今日学习、连续天数、热力图 |
| 计划 | 学习驾驶舱 | 今日计划、本周打卡 |
| 积分签到 | 学习驾驶舱 | 今日活跃和连续签到 |
| 模拟面试 | 学习资产 | 面试报告可转化为复习材料 |
| 博客/社区/CodePen | 学习资产 | 高价值内容可转成闪卡、练习计划、知识节点候选 |
| 学习资产 | 闪卡/知识图谱 | 审核后发布成可复习材料 |
| Career Loop | 学习驾驶舱 | 求职阶段和行动项 |
| 求职作战台 | Career Loop | JD、简历差距、行动计划 |

## 推荐阅读顺序

新同学读文档时，可以按这个顺序：

1. [面试题库](/modules/interview)：先理解最基础的学习内容模型。
2. [OJ 判题系统](/modules/oj)：理解代码能力如何被判题和统计。
3. [模拟面试与求职作战台](/modules/mock-interview-job-battle)：理解 AI 如何参与问答和求职计划。
4. [学习资产](/modules/learning-assets)：理解内容如何转成可复习资产。
5. [闪卡](/modules/flashcard)：理解复习算法和卡片学习。
6. [计划与学习小组](/modules/plan-team)：理解行为闭环和协作学习。
7. [知识图谱](/modules/knowledge)：理解知识结构化沉淀。
8. [AI Runtime](/modules/ai-runtime)：回头理解所有 AI 场景的治理底座。

## 验证清单

| 场景 | 目的 |
| --- | --- |
| 刷面试题并标记掌握度 | 验证题库、掌握度、每日统计 |
| 做一次 OJ 提交 | 验证 OJ 统计和排名 |
| 做一轮模拟面试 | 验证 AI 问答、报告、弱点识别 |
| 从内容转学习资产 | 验证学习资产候选和审核 |
| 学习闪卡 | 验证复习算法和热力图 |
| 完成计划打卡 | 验证计划统计和成长驾驶舱 |
| 创建学习小组并打卡 | 验证小组任务、打卡、排行 |
| 打开学习驾驶舱 | 验证聚合降级和成长分 |
| 生成成长自动驾驶计划 | 验证任务模板和周目标 |

## 常见设计取舍

| 取舍 | 当前选择 | 原因 |
| --- | --- | --- |
| 聚合页异常 | 单模块失败时降级 | 避免驾驶舱整体不可用 |
| 学习记录写入 | 异步记录 | 不阻断刷题体验 |
| 成长分 | 综合评分 | 比单一积分更能体现学习状态 |
| 学习资产发布 | 直接发布和审核发布并存 | 闪卡/计划可以直接发布，知识节点/题目更需要审核 |
| AI 输出 | 辅助决策 | 关键状态推进仍由后端规则校验 |

## 常见坑

| 问题 | 原因 | 处理 |
| --- | --- | --- |
| 学习驾驶舱数据看起来偏少 | 某个模块没有产生数据或 safeCall 降级 | 分别访问题库、OJ、闪卡、计划接口验证 |
| 成长分变动不明显 | 本周目标太低或行为集中在单模块 | 调整目标岗位、周学习时长和任务类型 |
| 学习资产无法发布 | 目标类型需要审核或字段不完整 | 查看 [学习资产](/modules/learning-assets) 的状态表 |
| AI 计划质量不稳定 | Prompt、Schema、RAG 或回归样例不足 | 进入 [AI Runtime](/modules/ai-runtime) 排查 |
| 排名趋势没有上周对比 | 排名快照需要先积累基线 | 让系统跑过至少一周 |

## 文档维护提醒

以后新增任何学习类功能，都要同时回答三个问题：

1. 它产生什么学习数据？
2. 它是否应该进入学习驾驶舱或成长自动驾驶？
3. 它能否转化为学习资产、闪卡、知识节点或计划任务？

只要这三个问题写清楚，Code Nest 的学习闭环就不会变成一堆孤立页面。
