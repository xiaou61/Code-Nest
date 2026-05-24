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

先读 `InterviewQuestionSetServiceImpl`，理解分类、题单和 Markdown 导入；再读 `InterviewMasteryServiceImpl`，理解掌握度如何变成复习计划；最后看 `LearningCockpitService` 如何消费这些统计。这样就能从"题库 CRUD"理解到"学习闭环数据"的设计。

---

## 深度拆解

### 一、题单创建与权限模型深度分析

`InterviewQuestionSetServiceImpl.createQuestionSet` 的完整流程：

```text
1. 校验分类存在 → categoryMapper.selectById(categoryId)
2. 获取创建人 → 优先 Admin 登录态，fallback User 登录态，双 null 则抛"请先登录"
3. creatorName 空白兜底 → "系统"
4. 构建 InterviewQuestionSet 对象
   - type 默认 2（用户创建），visibility 默认 1（公开），status 默认 0（草稿）
   - questionCount/viewCount/favoriteCount 全部初始化为 0
5. insert → result <= 0 抛"创建题单失败"
6. categoryService.updateQuestionSetCount(categoryId) → 重算分类下题单数
7. 返回 questionSet.getId()
```

**双登录态设计**：`SaTokenUserUtil.getCurrentAdminId()` 优先取管理端 ID，取不到再 `getCurrentUserId()`。这意味着同一个接口既可被管理端调用也可被用户端调用，但 **没有角色校验**——任何登录用户都能创建题单。

**权限检查 `hasAccessPermission`**：委托给 `questionSetMapper.hasAccessPermission(questionSetId, userId)`，SQL 层面判断公开题单或私有题单的创建者。但用户端 Controller 只在获取题目列表和题目详情时检查，**获取题单详情本身不检查权限**——任何登录用户都能看到私有题单的元信息（标题、描述、计数）。

**更新题单的分类迁移**：`updateQuestionSet` 中如果 `categoryId` 变更，会同时更新旧分类和新分类的 `questionSetCount`，保证统计一致。

**删除题单的级联**：`deleteQuestionSet` 先 `questionMapper.deleteByQuestionSetId(id)` 删除所有题目，再删题单，最后更新分类计数。但 **不删除** 学习记录、掌握度记录、收藏记录——会产生悬挂引用。

### 二、Markdown 导入解析器深度分析

`MarkdownParser` 的核心逻辑：

```text
parseMarkdown(markdownContent, questionSetId)
  ├─ 空白检查 → errors.add("Markdown内容不能为空")
  ├─ splitByQuestionHeaders(content)
  │   ├─ 正则 ^## (.+)$ 定位所有 ## 标题位置
  │   ├─ positions 为空 → throw RuntimeException("未找到题目标题")
  │   └─ 按位置切分为 sections[]
  ├─ for each section:
  │   ├─ parseQuestionSection(section, questionSetId, sortOrder)
  │   │   ├─ 提取标题: QUESTION_SPLIT_PATTERN.matcher(section) → group(1).trim()
  │   │   ├─ 提取答案: section.substring(titleMatcher.end()).trim()
  │   │   ├─ 答案空白 → throw RuntimeException("答案内容不能为空")
  │   │   └─ 构建 InterviewQuestion (questionSetId, title, answer, sortOrder)
  │   ├─ 成功 → successCount++, sortOrder++
  │   └─ 异常 → failureCount++, errors.add(失败信息)
  └─ return ParseResult(questions, errors, successCount, failureCount)
```

**导入服务层 `importMarkdownQuestions`**：

```text
1. 校验题单存在
2. MarkdownParser.parseMarkdown → parseResult
3. successCount == 0 → 抛业务异常（全部失败）
4. overwrite == true → questionMapper.deleteByQuestionSetId 先清空
5. 设置 createTime/updateTime → questionMapper.batchInsert 批量插入
6. updateQuestionCount 重算题单的 questionCount
7. 返回 insertCount
```

**关键细节**：
- `validateMarkdown` 方法校验 200 题上限，但 **`importMarkdownQuestions` 没有调用 `validateMarkdown`**——200 题限制形同虚设
- `splitByQuestionHeaders` 抛的是 `RuntimeException`，不是 `BusinessException`，会被外层 catch(Exception) 捕获后加入 errors
- `previewParse` 传入 `questionSetId=null`，预览不实际导入
- 正则 `^## (.+)$` 使用 `Pattern.MULTILINE`，`^` 和 `$` 匹配行首行尾而非字符串首尾——正确

### 三、掌握度与间隔重复算法深度分析

`InterviewMasteryServiceImpl.markMastery` 完整流程：

```text
1. 查询已有记录 → masteryMapper.selectByUserAndQuestion(userId, questionId)
2. isReview = (existingRecord != null)
3. reviewCount = isReview ? existingRecord.reviewCount + 1 : 0
4. calculateNextReviewDays(masteryLevel, reviewCount)
   ├─ baseInterval = BASE_INTERVALS[masteryLevel - 1]  // {1, 2, 4, 7}
   ├─ multiplier = 2^min(reviewCount, 5)
   ├─ days = baseInterval * multiplier
   └─ return min(days, 60)
5. nextReviewTime = now + nextReviewDays 天
6. if 新建:
   ├─ insert mastery_record (reviewCount=0)
   └─ dailyStatsMapper.incrementLearnCount(userId, today)
7. if 更新:
   ├─ update mastery_record (reviewCount=reviewCount)
   └─ dailyStatsMapper.incrementReviewCount(userId, today)
8. insertHistory(userId, questionId, masteryLevel, isReview?1:0)
9. return buildMasteryResponse(record, nextReviewDays)
```

**间隔重复公式详解**：

| masteryLevel | 文案 | baseInterval | reviewCount=0 | reviewCount=1 | reviewCount=2 | reviewCount=3 | reviewCount=4 | reviewCount=5+ |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | 不会 | 1 | 1天 | 2天 | 4天 | 8天 | 16天 | 32天 |
| 2 | 模糊 | 2 | 2天 | 4天 | 8天 | 16天 | 32天 | 60天(封顶) |
| 3 | 熟悉 | 4 | 4天 | 8天 | 16天 | 32天 | 60天(封顶) | 60天(封顶) |
| 4 | 已掌握 | 7 | 7天 | 14天 | 28天 | 56天 | 60天(封顶) | 60天(封顶) |

**封顶逻辑**：`min(days, 60)` 确保最长间隔不超过 60 天。`min(reviewCount, 5)` 限制指数增长上限为 `2^5 = 32` 倍。

**首次标记 vs 重复标记**：
- 首次：`reviewCount=0`，`incrementLearnCount`，历史 `isReview=0`
- 重复：`reviewCount=existing+1`，`incrementReviewCount`，历史 `isReview=1`

**MasteryMarkRequest.isReview 字段**：DTO 中有 `isReview` 字段但 **Service 层完全未使用**——实际判断是否复习靠的是 `existingRecord != null`，前端传入的 `isReview` 被忽略。

### 四、每日统计与热力图深度分析

**DailyStats 的 upsert 机制**：

`incrementLearnCount` 和 `incrementReviewCount` 使用 MySQL `INSERT ... ON DUPLICATE KEY UPDATE`：

```sql
-- incrementLearnCount
INSERT INTO interview_daily_stats (user_id, stat_date, learn_count, review_count, total_count, ...)
VALUES (#{userId}, #{statDate}, 1, 0, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    learn_count = learn_count + 1,
    total_count = total_count + 1,
    update_time = NOW()
```

这依赖 `(user_id, stat_date)` 的唯一约束。如果缺少该唯一索引，会导致重复插入。

**热力图 `getHeatmap` 流程**：

```text
1. dailyStatsMapper.selectByUserAndYear(userId, year) → yearStats
2. 转换为 DailyData 列表（date, count=totalCount, level=calculateLevel, learnCount, reviewCount）
3. totalDays = countTotalDays (totalCount > 0 的天数)
4. currentStreak = calculateCurrentStreak(userId, today)
5. longestStreak = calculateLongestStreak(yearStats)
6. monthStats = 12 次查询 countMonthDays
7. 返回 HeatmapResponse
```

**calculateLevel 热度等级**：

| totalCount | level | 颜色（GitHub 风格） |
| --- | --- | --- |
| 0 | 0 | 无色 |
| 1-5 | 1 | 浅绿 |
| 6-15 | 2 | 中绿 |
| 16-30 | 3 | 深绿 |
| 31+ | 4 | 最深绿 |

**连续学习天数计算**：

`calculateCurrentStreak` 从今天开始往前逐日查询 `dailyStatsMapper.selectByUserAndDate`，直到某天 `totalCount == 0` 或 `stats == null` 停止。**性能风险**：如果用户连续学习 365 天，会执行 365 次单行查询。

`calculateLongestStreak` 在内存中遍历 `yearStats` 列表，按日期连续性计算最长连续段——**但要求 yearStats 按 statDate 升序排列**（SQL 中 `ORDER BY stat_date ASC` 保证）。

**monthStats 的 N+1 问题**：`getHeatmap` 中对 12 个月各调用一次 `countMonthDays`，共 12 次查询。可以合并为一次 `GROUP BY MONTH(stat_date)` 查询。

### 五、复习列表与统计深度分析

**复习统计 `getReviewStats`**：

```text
1. overdueCount = countOverdueReview (nextReviewTime < now, 题单 status=1)
2. todayCount = countTodayReview (todayStart <= nextReviewTime < tomorrowStart, 题单 status=1)
3. weekCount = countWeekReview (todayStart <= nextReviewTime < weekEnd, 题单 status=1)
4. totalLearned = countTotalLearned (所有掌握度记录数)
5. level1~4Count = countByMasteryLevel(userId, 1~4)
```

共 **7 次 SQL 查询**。其中 overdue/today/week 三个查询都 JOIN 了 `interview_question_set` 并过滤 `iqs.status = 1`，确保只统计已发布题单下的待复习题。

**复习列表 `getReviewList`**：

| type | 查询 | 排序 |
| --- | --- | --- |
| overdue | nextReviewTime < now | nextReviewTime ASC（最逾期排最前） |
| today | todayStart <= nextReviewTime < tomorrowStart | nextReviewTime ASC |
| week | todayStart <= nextReviewTime < weekEnd | nextReviewTime ASC |
| all | overdue ∪ week（合并两个列表） | 无额外排序 |

**all 模式的合并问题**：`result.addAll(overdue) + result.addAll(week)` 产生的是逾期在前、本周在后的大致有序列表，但逾期和本周可能有重叠（今天的题既在 overdue 也在 week），导致 **重复出现**。

**SQL 层面**：所有复习查询都 LEFT JOIN 了 `interview_question` 和 `interview_question_set`，获取 `questionTitle`、`questionSetTitle`、`overdueDays`。如果题目或题单被删除，LEFT JOIN 结果为 null——`questionTitle` 和 `questionSetTitle` 为 null，但记录仍然返回。

### 六、收藏系统深度分析

`InterviewFavoriteServiceImpl` 支持两种收藏目标：

| targetType | 目标 | 计数更新 | 通知 |
| --- | --- | --- | --- |
| 1 | 题单 | questionSetMapper.increaseFavoriteCount / decreaseFavoriteCount | 通知题单作者 |
| 2 | 题目 | questionMapper.increaseFavoriteCount / decreaseFavoriteCount | 无 |

**收藏幂等性**：`addFavorite` 先 `favoriteMapper.exists()` 检查，已收藏则抛"已经收藏过了"。但 **没有数据库唯一约束保护**——并发下可能突破 exists 检查导致重复插入。

**取消收藏**：`removeFavorite` 先检查 exists，不存在抛"收藏记录不存在"。

**计数一致性**：add 时 +1，remove 时 -1。如果 `increaseFavoriteCount` 或 `decreaseFavoriteCount` 使用 `UPDATE SET count = count + 1`，并发安全；如果使用 `SET count = 新值`，则不安全。

**通知机制**：收藏题单时通过 `NotificationUtil.sendInterviewMessage` 通知题单创建者，catch 异常只 warn 不回滚。通知内容中用户名使用 `"用户" + userId` 硬编码拼接，**不是真实用户名**。

**批量删除 `batchRemoveFavorites`**：按 ID 列表批量删除，但 **不回减** 对应目标的 favoriteCount——计数会永久偏高。

### 七、学习记录深度分析

`InterviewLearnRecordServiceImpl.recordLearn`：

```text
@Async
recordLearn(userId, questionSetId, questionId)
  ├─ 构建 InterviewLearnRecord
  ├─ learnRecordMapper.insertIgnore(record)  // INSERT IGNORE 防重
  └─ catch(Exception) → log.warn (不阻断主流程)
```

**异步写入**：`@Async` 注解使方法在独立线程执行。调用方（Controller）立即返回成功，学习记录在后台写入。

**INSERT IGNORE**：依赖 `(user_id, question_id)` 唯一约束。如果缺少该约束，INSERT IGNORE 不会忽略重复，会插入多条。

**容错设计**：外层 try-catch 吞掉所有异常，只 warn 日志。这意味着：
- 数据库连接失败 → 静默丢失学习记录
- 唯一约束缺失 → 重复插入但不会报错

**查询方法**：`getLearnedQuestionIds`、`getLearnedCount`、`getTotalLearnedCount`、`isLearned` 都是同步查询，null 参数有防御性返回。

### 八、随机抽题深度分析

`InterviewQuestionServiceImpl.getRandomQuestions`：

```text
1. 校验 questionSetIds 非空、count > 0
2. 逐个校验题单存在（不校验公开状态）
3. questionMapper.selectByQuestionSetIds(ids) → allQuestions
4. allQuestions 为空 → 返回空列表
5. count >= allQuestions.size() → shuffle 全部返回
6. 否则 → shuffle 后 subList(0, count)
```

**性能问题**：将所有题目加载到内存再 shuffle，如果题单下有数千题，内存开销大。数据库层 `ORDER BY RAND() LIMIT n` 更适合大数据量场景。

**校验漏洞**：只校验题单存在，不校验题单是否已发布（status=1）——可以从下线题单抽题。

### 九、深度发现与坑点

#### 9.1 确认 BUG

| 编号 | BUG | 位置 | 说明 |
| --- | --- | --- | --- |
| BUG-1 | Markdown 导入无 200 题限制 | `InterviewQuestionSetServiceImpl.importMarkdownQuestions` | `validateMarkdown` 校验了 200 上限但未被调用，可导入任意数量 |
| BUG-2 | 删除题单不清理关联数据 | `InterviewQuestionSetServiceImpl.deleteQuestionSet` | 不删除 learn_record、mastery_record、favorite，产生悬挂引用 |
| BUG-3 | 复习列表 all 模式重复 | `InterviewMasteryServiceImpl.getReviewList` | overdue 和 week 时间范围重叠，同一天逾期题会出现两次 |
| BUG-4 | 批量删除收藏不回减计数 | `InterviewFavoriteServiceImpl.batchRemoveFavorites` | 只删除记录不 decreaseFavoriteCount，计数永久偏高 |
| BUG-5 | MasteryMarkRequest.isReview 被忽略 | `InterviewMasteryServiceImpl.markMastery` | DTO 有 isReview 字段但 Service 用 existingRecord!=null 判断，前端传入值无效 |
| BUG-6 | 随机抽题不校验题单状态 | `InterviewQuestionServiceImpl.getRandomQuestions` | 只校验题单存在，可从草稿/下线题单抽题 |
| BUG-7 | 收藏通知用户名硬编码 | `InterviewFavoriteServiceImpl.addFavorite` | `"用户" + userId` 不是真实用户名 |
| BUG-8 | calculateNextReviewDays 无越界保护 | `InterviewMasteryServiceImpl.calculateNextReviewDays` | `BASE_INTERVALS[masteryLevel - 1]` 若 masteryLevel=0 或 5 会 ArrayIndexOutOfBoundsException |

#### 9.2 设计风险

| 编号 | 风险 | 说明 |
| --- | --- | --- |
| RISK-1 | 连续学习天数逐日查询 | calculateCurrentStreak 最多 365 次单行 SELECT，应改为范围查询 |
| RISK-2 | 热力图 12 次 monthStats 查询 | 可合并为一次 GROUP BY |
| RISK-3 | 收藏无数据库唯一约束 | 并发下 exists 检查可被突破，导致重复收藏 |
| RISK-4 | INSERT IGNORE 依赖唯一约束 | interview_learn_record 缺少 (user_id, question_id) 唯一约束则防重失效 |
| RISK-5 | @Async 无线程池配置 | 使用 SimpleAsyncTaskExecutor，无上限可能 OOM |
| RISK-6 | 随机抽题全量加载 | selectByQuestionSetIds 加载所有题目到内存，大数据量下性能差 |
| RISK-7 | 题单详情无权限检查 | 用户端 getQuestionSetById 不检查私有题单权限，元信息泄露 |

#### 9.3 架构设计亮点

| 编号 | 亮点 | 说明 |
| --- | --- | --- |
| H-1 | 间隔重复算法 | 基于艾宾浩斯遗忘曲线，4 级掌握度 × 指数增长 × 60 天封顶，科学合理 |
| H-2 | INSERT IGNORE 防重 | 学习记录用 INSERT IGNORE 避免重复，配合异步写入不阻断主流程 |
| H-3 | DailyStats upsert | incrementLearnCount/ReviewCount 用 ON DUPLICATE KEY UPDATE 原子递增 |
| H-4 | Markdown 批量导入 | 管理端一键导入，覆盖/追加双模式，解析容错（部分成功部分失败） |
| H-5 | 复习查询 JOIN 题单状态 | overdue/today/week 查询都过滤 iqs.status=1，下线题单不计入复习 |
| H-6 | 热力图 4 级热度 | calculateLevel 分 0/1-5/6-15/16-30/31+ 五档，GitHub 风格直观 |
| H-7 | 收藏双目标类型 | targetType=1 题单 / 2 题目，一套服务支持两种收藏语义 |
| H-8 | 分类计数实时重算 | updateQuestionSetCount 每次 COUNT 重算，避免增量计数漂移 |
| H-9 | 双登录态创建 | 管理端和用户端共用 createQuestionSet，creatorId 优先取 Admin |
| H-10 | 上下题导航 | getNextQuestion/getPrevQuestion 按 sortOrder 定位，刷题体验流畅 |

#### 9.4 源码导航速查

| 想了解 | 读什么 |
| --- | --- |
| 题单创建 | `InterviewQuestionSetServiceImpl.java` — createQuestionSet + 双登录态 |
| Markdown 导入 | `InterviewQuestionSetServiceImpl.java` — importMarkdownQuestions + `MarkdownParser.java` |
| 掌握度标记 | `InterviewMasteryServiceImpl.java` — markMastery + calculateNextReviewDays |
| 间隔重复算法 | `InterviewMasteryServiceImpl.java` — BASE_INTERVALS + 2^min(reviewCount,5) + 60 天封顶 |
| 复习统计 | `InterviewMasteryServiceImpl.java` — getReviewStats (7 次 SQL) |
| 复习列表 | `InterviewMasteryServiceImpl.java` — getReviewList + overdue/today/week/all |
| 热力图 | `InterviewMasteryServiceImpl.java` — getHeatmap + calculateCurrentStreak + calculateLongestStreak |
| 热度等级 | `HeatmapResponse.java` — calculateLevel (0/1-5/6-15/16-30/31+) |
| 每日统计 upsert | `InterviewDailyStatsMapper.xml` — incrementLearnCount / incrementReviewCount |
| 学习记录 | `InterviewLearnRecordServiceImpl.java` — @Async + INSERT IGNORE |
| 收藏系统 | `InterviewFavoriteServiceImpl.java` — addFavorite + removeFavorite + 通知 |
| 随机抽题 | `InterviewQuestionServiceImpl.java` — getRandomQuestions (shuffle) |
| 题目 CRUD | `InterviewQuestionServiceImpl.java` — createQuestion + batchDeleteQuestions |
| 分类 CRUD | `InterviewCategoryServiceImpl.java` — 名称去重 + 级联检查 |
| 掌握度 Mapper | `InterviewMasteryMapper.xml` — 复习查询 LEFT JOIN 题目+题单 |
| 用户端题单 | `InterviewQuestionSetPublicController.java` — 权限检查 + 浏览计数 |
| 掌握度接口 | `InterviewMasteryController.java` — mark/batch/review/heatmap |
