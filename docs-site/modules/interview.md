# 面试题库

面试题库是 Code Nest 的基础学习模块，负责分类、题单、题目、答案、收藏、学习记录、掌握度、复习列表和热力图。它看起来像一个内容模块，但真正的价值在于把“刷过什么、掌握到什么程度、什么时候该复习”沉淀成可追踪数据。

## 功能入口

| 端 | 页面 |
| --- | --- |
| 用户端 | `/interview`、`/interview/random`、`/interview/question-sets/:id`、`/interview/questions/:setId/:questionId` |
| 用户端复习 | `/interview/favorites`、`/interview/review` |
| 管理端 | `/interview/categories`、`/interview/question-sets`、`/interview/questions` |
| 后端模块 | `xiaou-interview` |

## 推荐学习顺序

第一次看面试题库时，可以把它拆成“内容管理”和“学习闭环”两条线。先理解题目从哪里来，再理解用户学习数据怎么沉淀。

1. 先看分类、题单和题目，知道管理端如何把题库内容发布给用户。
2. 再看用户端题单详情和题目详情，理解学习记录什么时候写入。
3. 接着看收藏和掌握度，区分“我想以后再看”和“系统认为我该复习”。
4. 然后看 Markdown 导入，掌握批量建设题库的最低成本方式。
5. 最后看学习驾驶舱、闪卡、模拟面试这些联动模块，理解题库数据怎样被复用。

如果只想快速跑通一条链路，推荐从“新建分类 -> 新建题单 -> 导入 2 道题 -> 用户学习 -> 标记掌握度 -> 查看复习列表”开始。

## 源码地图

| 文件 | 说明 |
| --- | --- |
| `xiaou-interview/src/main/java/com/xiaou/interview/controller/pub/InterviewCategoryPublicController.java` | 用户端分类列表和详情 |
| `xiaou-interview/src/main/java/com/xiaou/interview/controller/pub/InterviewQuestionSetPublicController.java` | 用户端题单、题目、搜索、随机刷题 |
| `xiaou-interview/src/main/java/com/xiaou/interview/controller/pub/InterviewFavoriteController.java` | 收藏、取消收藏、收藏列表 |
| `xiaou-interview/src/main/java/com/xiaou/interview/controller/pub/InterviewMasteryController.java` | 掌握度、复习列表、热力图 |
| `xiaou-interview/src/main/java/com/xiaou/interview/controller/pub/InterviewLearnRecordController.java` | 学习记录和题单进度 |
| `xiaou-interview/src/main/java/com/xiaou/interview/controller/admin/InterviewCategoryController.java` | 管理端分类维护 |
| `xiaou-interview/src/main/java/com/xiaou/interview/controller/admin/InterviewQuestionSetController.java` | 管理端题单维护和 Markdown 导入 |
| `xiaou-interview/src/main/java/com/xiaou/interview/controller/admin/InterviewQuestionController.java` | 管理端题目维护 |
| `xiaou-interview/src/main/java/com/xiaou/interview/service/impl/InterviewQuestionSetServiceImpl.java` | 题单创建、删除、导入和统计维护 |
| `xiaou-interview/src/main/java/com/xiaou/interview/service/impl/InterviewMasteryServiceImpl.java` | 掌握度、复习时间、热力图和连续学习 |
| `xiaou-interview/src/main/java/com/xiaou/interview/service/impl/InterviewLearnRecordServiceImpl.java` | 用户学习记录写入 |
| `xiaou-interview/src/main/java/com/xiaou/interview/utils/MarkdownParser.java` | Markdown 批量导入解析 |

## 领域模型

| 模型 | 表 | 说明 |
| --- | --- | --- |
| 分类 | `interview_category` | 题库一级分类，统计题单数量 |
| 题单 | `interview_question_set` | 一组面试题，归属分类 |
| 题目 | `interview_question` | 题目标题、Markdown 答案、排序、浏览/收藏计数 |
| 收藏 | `interview_favorite` | 用户收藏题目 |
| 学习记录 | `interview_learn_record` | 用户是否学习过某题 |
| 掌握度 | `interview_mastery_record` | 当前掌握等级、复习次数、下次复习时间 |
| 掌握历史 | `interview_mastery_history` | 每次标记记录 |
| 每日统计 | `interview_daily_stats` | 每日学习数、复习数、热力图来源 |

## 题单状态和权限

题单字段来自 `InterviewQuestionSet`：

| 字段 | 值 | 说明 |
| --- | --- | --- |
| `type` | `1` | 官方题单 |
| `type` | `2` | 用户创建题单，当前管理端创建时也兼容该值 |
| `visibility` | `1` | 公开 |
| `visibility` | `2` | 私有 |
| `status` | `0` | 草稿 |
| `status` | `1` | 发布 |
| `status` | `2` | 下线 |

用户端公共题单只应该展示可访问、已发布的数据。管理端可以维护草稿、发布和下线状态。

创建题单时会校验分类存在；创建人优先取管理端登录态，取不到再兼容普通用户登录态。删除题单会级联删除题单下题目，并更新分类题单数量。

## 用户学习流程

1. 用户进入 `/interview` 查看分类和题单。
2. 用户打开题单详情，加载题目列表。
3. 用户进入题目详情，查看题目和 Markdown 答案。
4. 用户可以收藏题目。
5. 用户学习题目后，`InterviewLearnRecordServiceImpl.recordLearn` 异步写入学习记录。
6. 用户标记掌握度，系统计算下次复习时间。
7. 复习页根据 overdue、today、week、all 查询待复习题。
8. 热力图按 `interview_daily_stats` 展示全年学习/复习分布。

学习记录使用 `INSERT IGNORE` 思路避免重复插入。异步写入失败只记录日志，不阻断用户刷题主流程。

## 掌握度与复习算法

掌握度等级：

| 等级 | 文案 | 基础复习间隔 |
| --- | --- | --- |
| `1` | 不会 | 1 天 |
| `2` | 模糊 | 2 天 |
| `3` | 熟悉 | 4 天 |
| `4` | 已掌握 | 7 天 |

`InterviewMasteryServiceImpl` 会用基础间隔乘以复习次数增长系数：

```text
nextReviewDays = baseInterval[masteryLevel] * 2^min(reviewCount, 5)
nextReviewDays <= 60
```

第一次标记时 `reviewCount = 0`，并增加每日学习数。之后再标记同一题，会把 `reviewCount + 1`，并增加每日复习数。每次标记都会写入 `interview_mastery_history`。

复习统计口径：

| 统计 | 说明 |
| --- | --- |
| 逾期 | `nextReviewTime < now` |
| 今日 | `nextReviewTime` 在今天 00:00 到明天 00:00 |
| 本周 | 今天起 7 天内 |
| 总学习 | 当前用户掌握度记录总数 |
| 热力图等级 | 根据当天学习/复习总数计算 |
| 连续学习 | 从今天往前连续统计 `totalCount > 0` 的日期 |

## Markdown 导入格式

管理端题单支持 Markdown 批量导入。解析器按 `## ` 开头的标题拆分题目，标题后面的全部内容作为答案。

示例：

```markdown
## 什么是 Java volatile？

volatile 主要保证可见性和有序性，但不保证复合操作的原子性。

## synchronized 和 ReentrantLock 有什么区别？

synchronized 是 JVM 内置锁，ReentrantLock 提供可中断、公平锁等更细粒度能力。
```

导入规则：

| 规则 | 说明 |
| --- | --- |
| 标题 | 必须以 `## ` 开头 |
| 答案 | 标题后内容不能为空 |
| 排序 | 按解析顺序从 1 开始 |
| 覆盖导入 | `overwrite=true` 时先删除原题目 |
| 数量限制 | 校验方法限制单次最多 200 题 |
| 失败处理 | 没有任何成功题目时抛业务异常 |

常见导入失败原因：

- 使用了 `#` 或 `###`，没有使用 `## `。
- 题目标题后没有答案内容。
- Markdown 内容为空。
- 题单 ID 不存在。

## 接口分组

用户端：

| 接口域 | 能力 |
| --- | --- |
| `/interview/categories` | 启用分类列表、分类详情 |
| `/interview/question-sets` | 题单分页、详情、题目列表、上下题、搜索、随机题 |
| `/interview/favorites` | 收藏、取消收藏、收藏状态、我的收藏、收藏数量 |
| `/interview/mastery` | 标记掌握度、批量查询、复习统计、复习列表、热力图 |
| `/interview/learn` | 记录学习、题单进度、已学题目、学习总量 |

管理端：

| 接口域 | 能力 |
| --- | --- |
| `/admin/interview/categories` | 分类增删改查 |
| `/admin/interview/question-sets` | 题单增删改查、分页、用户题单、Markdown 导入、浏览量 |
| `/admin/interview/questions` | 题目增删改查、题单题目、上下题、搜索、批量删除 |

## 和其他模块的关系

| 模块 | 关系 |
| --- | --- |
| 模拟面试 | 可以从题库中选题，也可以 AI 出题 |
| 学习驾驶舱 | 聚合总学习数、复习统计和热力图 |
| 成长自动驾驶 | 可把题库复习作为周目标任务 |
| 闪卡 | 面试题可进一步转成可记忆卡片 |
| 知识图谱 | 高频薄弱知识点可以沉淀为图谱节点 |
| 积分 | 当前题库学习本身不直接发积分，驾驶舱会把活跃度纳入成长分 |

## 验证清单

1. 管理端新建分类。
2. 新建题单，状态设为草稿，确认用户端不可见。
3. 发布题单，确认用户端列表可见。
4. 用 Markdown 导入 2 到 3 个题目，确认排序正确。
5. 用户打开题目详情，确认学习记录异步写入。
6. 收藏题目，再进入收藏页确认可见。
7. 标记掌握度为 1、2、3、4，确认下次复习时间按 1/2/4/7 天起步。
8. 重复标记同一题，确认复习次数增加，间隔变长。
9. 查看复习统计和热力图，确认每日统计发生变化。
10. 删除题单，确认题目同步删除，分类题单数量更新。

## 常见坑

| 问题 | 原因 | 处理 |
| --- | --- | --- |
| 用户端看不到题单 | 题单未发布、私有或权限不满足 | 检查 `status`、`visibility` 和访问权限 |
| Markdown 导入 0 题 | 标题不是 `## ` 格式 | 按导入格式调整 |
| 热力图没变化 | 只看题但没有标记学习/掌握度 | 确认学习记录和每日统计写入 |
| 复习列表太多 | 掌握度标记偏低或长期未复习 | 按 overdue/today/week 分批处理 |
| 学习记录重复 | 同一题反复进入详情 | 表层应使用唯一约束或 `insertIgnore` 防重 |
| 删除分类失败 | 分类下还有题单 | 先迁移或删除题单，再删除分类 |

## 学习建议

先读 `InterviewQuestionSetServiceImpl`，理解分类、题单和 Markdown 导入；再读 `InterviewMasteryServiceImpl`，理解掌握度如何变成复习计划；最后看 `LearningCockpitService` 如何消费这些统计。这样就能从“题库 CRUD”理解到“学习闭环数据”的设计。
