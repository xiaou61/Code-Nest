# 闪卡

闪卡模块用于把碎片知识转成可复习的卡片，并通过间隔重复算法安排下一次复习。它和面试题库、学习资产、成长自动驾驶都有联动空间：题库可以导入闪卡，学习资产可以生成闪卡，自动驾驶可以把闪卡复习排进今日任务。

## 功能入口

| 页面 | 说明 |
| --- | --- |
| `/flashcard` | 闪卡首页、公开卡组、学习概览 |
| `/flashcard/deck/:id` | 卡组详情 |
| `/flashcard/study` | 今日学习 |
| `/flashcard/study/:deckId` | 指定卡组学习 |
| `/flashcard/my` | 我的卡组 |
| `/flashcard/deck/create` | 创建卡组 |
| `/flashcard/deck/:id/edit` | 编辑卡组 |
| `/flashcard/deck/:deckId/cards` | 编辑卡片 |

## 推荐学习顺序

闪卡模块可以按“内容从哪里来，复习怎么排，统计怎么沉淀”来学。它和面试题库很像，但重点不是题目展示，而是下一次复习时间。

1. 先看卡组和卡片，理解私有、公开、Fork 和卡片编辑权限。
2. 再看从题库导入，理解跨模块内容如何变成可复习卡片。
3. 接着看今日学习和下一张卡，掌握“到期复习优先，新卡补足”的推荐规则。
4. 然后看 `SM2Algorithm`，理解评分如何影响间隔、难度因子和掌握度。
5. 最后看每日统计和热力图，确认学习行为如何进入长期数据。

如果要快速验证，建议先创建一个私有卡组、手动加两张卡，再连续提交几次不同评分，比直接从公开卡组 Fork 更容易看清算法变化。

## 源码地图

| 层级 | 文件或目录 |
| --- | --- |
| 前端页面 | `vue3-user-front/src/views/flashcard/` |
| 前端 API | `vue3-user-front/src/api/flashcard.js` |
| Controller | `xiaou-flashcard/src/main/java/com/xiaou/flashcard/controller/` |
| Service | `xiaou-flashcard/src/main/java/com/xiaou/flashcard/service/impl/` |
| SM-2 算法 | `xiaou-flashcard/src/main/java/com/xiaou/flashcard/algorithm/SM2Algorithm.java` |
| 枚举 | `xiaou-flashcard/src/main/java/com/xiaou/flashcard/enums/` |

## 接口分组

| 接口域 | 能力 |
| --- | --- |
| `/pub/flashcard/deck` | 公开卡组列表、公开卡组详情、公开卡片 |
| `/flashcard/deck` | 创建、更新、删除、我的卡组、Fork |
| `/flashcard/card` | 新增、批量新增、更新、删除、按卡组查询、从题库导入 |
| `/flashcard/study` | 今日学习、下一张卡、提交学习结果、统计、热力图 |

## 核心概念

| 概念 | 表 | 说明 |
| --- | --- | --- |
| 卡组 | `flashcard_deck` | 一组闪卡，可私有或公开 |
| 卡片 | `flashcard` | 正面问题、反面答案、内容类型、标签 |
| 学习记录 | `flashcard_study_record` | 用户对某张卡的掌握情况和下次复习时间 |
| 每日统计 | `flashcard_daily_stats` | 每天新学、复习、正确数和学习时长 |

卡片内容类型：

| 值 | 含义 |
| --- | --- |
| `1` | 文本 |
| `2` | Markdown |
| `3` | 代码 |

掌握度：

| 值 | 含义 |
| --- | --- |
| `1` | 新卡 |
| `2` | 学习中 |
| `3` | 已掌握 |

## 卡组生命周期

1. 用户创建卡组，系统初始化 `card_count = 0`、`study_count = 0`、`fork_count = 0`。
2. 用户新增、批量新增或从题库导入卡片。
3. 卡组可以保持私有，也可以设置为公开。
4. 公开卡组能被其他用户查看和 Fork。
5. Fork 会复制卡组和卡片，新的卡组归当前用户所有，并记录 `source_deck_id`。
6. Fork 后源卡组 `fork_count` 增加。
7. 删除卡组时，会先删除卡组下所有卡片，再删除卡组。

权限边界：

| 操作 | 权限 |
| --- | --- |
| 查看公开卡组 | 任意登录用户或公开接口读者 |
| 查看私有卡组 | 卡组创建者 |
| 编辑卡组 | 卡组创建者 |
| 删除卡组 | 卡组创建者 |
| 编辑卡片 | 卡片所属卡组创建者 |
| Fork 私有卡组 | 仅创建者自己 |

## 从题库导入

`FlashcardServiceImpl#importFromQuestionBank` 的流程：

1. 校验卡组归属。
2. 遍历题目 ID。
3. 如果题目已经导入过同一卡组，跳过。
4. 调用 `InterviewQuestionService#getQuestionById` 读取题目。
5. 题目标题作为闪卡正面。
6. 题目答案作为闪卡反面。
7. 内容类型设置为 Markdown。
8. 写入 `source_question_id`，方便追溯来源。
9. 批量插入后更新卡组卡片数量。

这是一条很典型的“跨模块内容转化”链路，适合对照学习资产模块一起看。

## 学习推荐规则

今日学习的顺序是：

1. 优先取到期复习卡，也就是 `next_review_time <= now` 的学习记录。
2. 如果数量不足，再从用户自己的卡组里补未学习过的新卡。
3. 默认限制 20 张；传入 `limit <= 0` 时也会回到 20。

指定卡组下一张卡的顺序是：

1. 先取该卡组到期复习卡。
2. 没有到期卡时，取该卡组未学习的新卡。
3. 都没有时返回空。

统计里的每日新卡上限是 20。

## SM-2 算法

闪卡使用经典 SM-2 间隔重复算法。用户在前端提交 1 到 4 的简单评分，后端映射到 SM-2 的 0 到 5：

| 用户评分 | 映射后 | 含义 |
| --- | --- | --- |
| `1` | `1` | 完全忘记 |
| `2` | `3` | 模糊记忆 |
| `3` | `4` | 想起来了 |
| `4` | `5` | 完全记住 |

算法规则：

| 规则 | 说明 |
| --- | --- |
| 正确判断 | SM-2 评分大于等于 3 |
| 首次正确 | 间隔 1 天 |
| 第二次正确 | 间隔 6 天 |
| 后续正确 | `interval * easeFactor` |
| 错误 | 连续正确次数归 0，间隔回到 1 天 |
| EF 默认值 | 2.50 |
| EF 最小值 | 1.30 |
| 最大间隔 | 365 天 |
| 已掌握判断 | 连续正确次数大于等于 3 |

提交学习结果后会更新：

| 字段 | 说明 |
| --- | --- |
| `repetitions` | 连续正确次数 |
| `ease_factor` | 难度因子 |
| `interval_days` | 当前间隔天数 |
| `mastery_level` | 掌握度 |
| `last_review_time` | 本次复习时间 |
| `next_review_time` | 下次复习时间 |
| `total_reviews` | 总复习次数 |
| `correct_count` | 正确次数 |

## 每日统计和热力图

提交学习结果时会同步更新 `flashcard_daily_stats`：

| 字段 | 说明 |
| --- | --- |
| `new_cards` | 新学卡片数 |
| `review_cards` | 复习卡片数 |
| `correct_cards` | 正确卡片数 |
| `study_duration` | 学习时长，秒 |

热力图等级：

| 数量 | 等级 |
| --- | --- |
| `0` | 0 |
| `1-5` | 1 |
| `6-15` | 2 |
| `16-30` | 3 |
| `>30` | 4 |

连续学习天数会从最近学习日期往前计算。如果今天没学但昨天学了，源码里也会把昨天作为连续起点。

## 表结构速查

| 表 | 关键字段 |
| --- | --- |
| `flashcard_deck` | `user_id`、`name`、`is_public`、`card_count`、`study_count`、`fork_count`、`source_deck_id`、`del_flag` |
| `flashcard` | `deck_id`、`front_content`、`back_content`、`content_type`、`tags`、`source_question_id`、`sort_order` |
| `flashcard_study_record` | `user_id`、`card_id`、`deck_id`、`repetitions`、`ease_factor`、`interval_days`、`mastery_level`、`next_review_time` |
| `flashcard_daily_stats` | `user_id`、`stat_date`、`new_cards`、`review_cards`、`correct_cards`、`study_duration` |

主 SQL 里还保留了 `flashcard_deck_like`、`flashcard_review_log`、`flashcard_study_history`、`flashcard_user_deck` 等历史或扩展表。当前核心服务主要围绕上面四张表运转。

## 验证清单

- 创建卡组后，`flashcard_deck.card_count` 为 0。
- 新增卡片后，卡组卡片数量增加。
- 私有卡组不能被非创建者查看或 Fork。
- 公开卡组 Fork 后，新卡组为私有，源卡组 `fork_count` 增加。
- 从题库导入同一题目第二次会跳过。
- 今日学习优先返回到期复习卡，再补新卡。
- 提交评分 1 会重置连续正确次数。
- 连续正确 3 次后，掌握度变为已掌握。
- 热力图能返回完整日期范围，没学习的日期数量为 0。

## 常见坑

| 问题 | 原因 | 处理 |
| --- | --- | --- |
| 今日学习为空 | 没有到期卡，也没有用户自己的新卡 | 先创建或 Fork 卡组 |
| 公开卡组能看但不能编辑 | 不是创建者 | Fork 后编辑自己的副本 |
| 导入题库数量小于选择数量 | 已导入或题目不存在会跳过 | 看日志和 `source_question_id` |
| 下次复习时间异常远 | 多次高评分导致间隔增长 | 最大间隔限制为 365 天 |
| 学习人数不增加 | 只有首次学习该卡组时才增加 | 检查用户是否已有该卡组学习记录 |

---

## 深度拆解

### 一、SM-2 间隔重复算法深度分析

`SM2Algorithm.calculate` 的完整流程：

```text
输入: repetitions(连续正确次数), easeFactor(难度因子), interval(当前间隔), quality(评分0-5)
1. quality 越界保护 → clamp(0, 5)
2. easeFactor <= 0 → 兜底 2.50
3. isCorrect = quality >= 3
4. if 正确:
   ├─ repetitions=0 → interval=1
   ├─ repetitions=1 → interval=6
   └─ repetitions>=2 → interval = round(interval * easeFactor)
   └─ repetitions += 1
5. if 错误:
   ├─ interval = 1
   └─ repetitions = 0
6. interval = min(interval, 365)
7. EF' = EF + (0.1 - (5-q)*(0.08 + (5-q)*0.02))
8. EF' = max(EF', 1.30)
9. EF' = round(EF' * 100) / 100  (保留两位小数)
10. masteryLevel = calculateMasteryLevel(repetitions)
    ├─ 0 → 1(新卡)
    ├─ 1-2 → 2(学习中)
    └─ 3+ → 3(已掌握)
11. return SM2Result(repetitions, easeFactor, interval, masteryLevel, isCorrect)
```

**简化评分映射**：前端 1-4 映射到 SM-2 的 0-5 存在两套不同映射：

| 映射方法 | 评分1 | 评分2 | 评分3 | 评分4 | 默认 |
| --- | --- | --- | --- | --- | --- |
| `SM2Algorithm.convertToSM2Quality` | 0 | 2 | 4 | 5 | 3 |
| `FlashcardStudyServiceImpl.mapQualityToSM2` | **1** | **3** | **4** | **5** | **3** |

**两套映射不一致**！`convertToSM2Quality` 把评分1映射为 0（完全不记得），而 `mapQualityToSM2` 把评分1映射为 1（错误但看到答案后想起来）。`submitStudyResult` 调用的是 `mapQualityToSM2`，`convertToSM2Quality` 实际未被使用。

**EF 更新公式推导**：

```text
EF' = EF + (0.1 - (5-q)*(0.08 + (5-q)*0.02))
展开: EF' = EF + 0.1 - (5-q)*0.08 - (5-q)²*0.02
```

| quality | (5-q) | efDelta | 效果 |
| --- | --- | --- | --- |
| 5 | 0 | +0.10 | EF 增加（越容易，间隔增长更快） |
| 4 | 1 | +0.00 | EF 不变 |
| 3 | 2 | -0.14 | EF 减少（困难题间隔增长放缓） |
| 1 | 4 | -0.50 | EF 大幅下降 |

### 二、学习推荐与今日学习深度分析

`FlashcardStudyServiceImpl.getTodayStudyCards`：

```text
1. limit 默认 20
2. 获取到期复习卡 → studyRecordMapper.selectDueCards(userId, null, now, limit)
   ├─ 对每条 record: cardMapper.selectById + deckMapper.selectById → convertToStudyVO
3. 仍有空位 → 遍历用户所有卡组:
   ├─ deckMapper.selectByUserId(userId) → decks
   ├─ studyRecordMapper.selectLearnedCardIds(userId, deckId) → learnedSet
   ├─ cardMapper.selectIdsByDeckId(deckId) → allCardIds
   └─ 逐个: 未学过 + 未已添加 → cardMapper.selectById → convertToStudyVO
```

**N+1 问题严重**：
- 到期复习：每条记录查 2 次（card + deck）
- 新卡补充：每个卡组查 3 次（learnedIds + allCardIds + 逐个selectById）

**指定卡组下一张 `getNextCard`**：逻辑类似但简化——先查1条到期卡，再查1条新卡。同样存在逐行 selectById 的 N+1 问题。

### 三、提交学习结果深度分析

`submitStudyResult` 的完整流程：

```text
1. cardMapper.selectById(cardId) → 校验存在
2. studyRecordMapper.selectByUserAndCard(userId, cardId) → record
3. mapQualityToSM2(quality) → sm2Quality (1→1, 2→3, 3→4, 4→5)
4. isCorrect = sm2Quality >= 3
5. isNew = (record == null)
6. if 新建:
   ├─ 初始化 record (EF=2.5, repetitions=0, interval=0, mastery=NEW)
   ├─ 判断是否首次学习该卡组 → deckMapper.incrementStudyCount(deckId)
7. SM2Algorithm.calculate → sm2Result
8. 更新 record 全部字段 (repetitions, EF, interval, mastery, nextReview, totalReviews, correctCount)
9. isNew ? insert : update
10. dailyStatsMapper.incrementStats(userId, today, new/review/correct/duration)
11. 构建 FlashcardStudyResultVO (cardId, mastery, nextReview, interval, remainingDue)
```

**incrementStats 设计**：new/review/correct/duration 一次性传入，比面试题库的 incrementLearnCount/ReviewCount 分开调用更高效。

**首次学习卡组判断**：`countLearnedCards(userId, deckId) == 0` 时才增加 studyCount。这意味着一个用户在同一卡组下只计一次——但 Fork 后的新卡组是新 deckId，会被重新计数。

### 四、Fork 机制深度分析

`FlashcardDeckServiceImpl.forkDeck`：

```text
1. 校验源卡组存在
2. 非公开 + 非创建者 → 抛"无权复制"
3. 创建新 FlashcardDeck (isPublic=false, cardCount=0, forkCount=0, sourceDeckId=deckId)
4. deckMapper.insert(newDeck)
5. 复制所有闪卡:
   ├─ cardMapper.selectByDeckId(deckId) → sourceCards
   ├─ 逐个 clone (deckId→newDeck.id, 保持 sourceQuestionId)
   └─ cardMapper.batchInsert(newCards)
6. deckMapper.updateCardCount(newDeck.getId, newCards.size)
7. deckMapper.incrementForkCount(deckId)
```

**Fork 后学习记录不复制**——新卡组的学习从零开始，这是正确设计。

**Fork 后源卡组的更新不会同步到 Fork 副本**——典型的快照式 Fork，非实时同步。

**卡片数量更新**：`updateCardCount` 接收增量（+cards.size），而 `FlashcardServiceImpl.createCard` 中也用增量 +1、`deleteCard` 用 -1。**面试题库用的是 COUNT 重算**，而闪卡用的是增量更新——两种策略不一致。

### 五、跨模块题库导入深度分析

`FlashcardServiceImpl.importFromQuestionBank`：

```text
1. validateDeckOwnership(deckId, userId)
2. 遍历 questionIds:
   ├─ cardMapper.countBySourceQuestionId(deckId, questionId) > 0 → 跳过
   ├─ interviewQuestionService.getQuestionById(questionId) → question
   ├─ question == null → 跳过
   ├─ frontContent = question.title, backContent = question.answer
   ├─ contentType = MARKDOWN(2)
   └─ sourceQuestionId = questionId
3. cardMapper.batchInsert(cardsToInsert)
4. deckMapper.updateCardCount(deckId, cardsToInsert.size())
```

**跨模块依赖**：`FlashcardServiceImpl` 直接注入 `InterviewQuestionService`，属于模块间直接引用而非 API 调用。`xiaou-flashcard` 的 pom 需要依赖 `xiaou-interview`。

**防重复导入**：`countBySourceQuestionId` 检查同一卡组内是否已导入同一题目。但如果题目被删除再导入，原 sourceQuestionId 仍存在卡片中——`getQuestionById` 会抛 BusinessException 而不是返回 null，导致导入中断。

### 六、卡组删除与清理深度分析

`FlashcardDeckServiceImpl.deleteDeck`：

```text
1. 校验存在 + 校验所有权
2. cardMapper.deleteByDeckId(id) → 删除所有卡片
3. deckMapper.deleteById(id) → 删除卡组
```

**不清理的数据**：与面试题库一样，删除卡组后 `flashcard_study_record` 和 `flashcard_daily_stats` 不会清理，存在悬挂引用。更严重的是，Fork 来源的 `sourceDeckId` 指向已删除卡组——但 Fork 副本本身不受影响（已复制了数据）。

### 七、统计与热力图深度分析

`getStudyStats` 执行 **7 次 SQL**：

```text
1. countDueCards(userId, deckId, now)
2. countLearnedCards(userId, deckId)
3. countByMasteryLevel(userId, deckId, NEW)
4. countByMasteryLevel(userId, deckId, LEARNING)
5. countByMasteryLevel(userId, deckId, MASTERED)
6. dailyStatsMapper.selectByUserAndDate(userId, today)
7. cardMapper.countByDeckId(deckId) (仅 deckId!=null)
8. dailyStatsMapper.countStudyDays(userId)
9. calculateStreakDays → selectRecentDates(userId, endDate, 365)
```

**calculateStreakDays**：先获取最近365天的学习日期列表（一次性查询），再在内存中从今天往前匹配。比面试题库的逐日查询更高效。**容错设计**：如果今天没学但昨天学了，从昨天开始算连续——这是一个友好的处理。

**热力图**：获取指定日期范围的 stats，转为 Map，遍历完整日期范围填充每日数据。没学习的日期 count=0/level=0。这比面试题库按年查询更灵活（支持任意天数范围）。

### 八、深度发现与坑点

#### 8.1 确认 BUG

| 编号 | BUG | 位置 | 说明 |
| --- | --- | --- | --- |
| BUG-1 | SM-2 评分映射不一致 | `SM2Algorithm.convertToSM2Quality` vs `mapQualityToSM2` | 前者 1→0, 后者 1→1，后者实际在用，前者未被调用 |
| BUG-2 | 题库导入 getQuestionById 抛异常 | `FlashcardServiceImpl.importFromQuestionBank:202` | `getQuestionById` 抛 BusinessException 而非返回 null，题目不存在会导致整个导入中断 |
| BUG-3 | 删除卡组不清理学习记录 | `FlashcardDeckServiceImpl.deleteDeck` | study_record 和 daily_stats 成为悬挂数据 |
| BUG-4 | 卡片数量增量更新无保护 | `FlashcardServiceImpl.createCard:63` | `updateCardCount(deckId, 1)` 用增量而非 COUNT 重算，并发创建可能计数偏高 |
| BUG-5 | 今日学习 N+1 查询 | `FlashcardStudyServiceImpl.getTodayStudyCards` | 每张到期卡 2 次 SELECT，每张新卡至少 3 次 SELECT |
| BUG-6 | 卡组删除不检查 del_flag | `FlashcardDeckServiceImpl.deleteDeck` | 使用 `selectById` 不过滤 del_flag=1 的逻辑删除记录 |

#### 8.2 设计风险

| 编号 | 风险 | 说明 |
| --- | --- | --- |
| RISK-1 | 跨模块直接依赖 | FlashcardServiceImpl 注入 InterviewQuestionService，而非通过 API 模块调用 |
| RISK-2 | Fork 快照不同步 | 源卡组更新后 Fork 副本不会收到变更 |
| RISK-3 | 评分映射混乱 | 两套映射并存，文档描述的是 convertToSM2Quality 但实际用的是 mapQualityToSM2 |
| RISK-4 | 连续正确次数与掌握度绑定 | masteryLevel 仅看 repetitions (0→新, 1-2→学习, 3+→已掌握)，不考虑 easeFactor |
| RISK-5 | 热力图 studyDuration 不防篡改 | 前端传入 duration，无服务端校验 |
| RISK-6 | dailyStats incrementStats 无唯一约束保护 | 依赖 (user_id, stat_date) 唯一约束做 upsert |

#### 8.3 架构设计亮点

| 编号 | 亮点 | 说明 |
| --- | --- | --- |
| H-1 | 经典 SM-2 算法 | EF 公式科学合理，1→1天→6天→interval*EF 三级间隔增长 |
| H-2 | Fork 快照式设计 | 源和副本独立，互不影响，降低维护复杂度 |
| H-3 | 题库导入防重复 | sourceQuestionId 检查同一卡组不重复导入 |
| H-4 | 到期复习优先 + 新卡补足 | 学习推荐规则简单清晰，用户体验好 |
| H-5 | 连续学习容错 | 今天没学但昨天学了也算连续，降低断签挫败感 |
| H-6 | 一次性 incrementStats | new/review/correct/duration 四维度一次 upsert |
| H-7 | 热力图灵活日期范围 | 支持任意天数（默认365），比面试题库按年更灵活 |
| H-8 | 卡组学习人数精确控制 | 首次学习该卡组才 incrementStudyCount，同一用户只计一次 |
| H-9 | 卡组软删除设计 | del_flag 字段支持逻辑删除 |
| H-10 | 模块间内容转化 | 题库题目→闪卡正面/反面，sourceQuestionId 可追溯 |

#### 8.4 源码导航速查

| 想了解 | 读什么 |
| --- | --- |
| SM-2 算法 | `SM2Algorithm.java` — calculate + convertToSM2Quality + calculateMasteryLevel |
| 提交学习结果 | `FlashcardStudyServiceImpl.java` — submitStudyResult + mapQualityToSM2 |
| 今日学习 | `FlashcardStudyServiceImpl.java` — getTodayStudyCards (到期优先+新卡补足) |
| 下一张卡 | `FlashcardStudyServiceImpl.java` — getNextCard |
| 学习统计 | `FlashcardStudyServiceImpl.java` — getStudyStats (9次SQL) |
| 热力图 | `FlashcardStudyServiceImpl.java` — getHeatmap + calculateHeatLevel |
| 连续学习天数 | `FlashcardStudyServiceImpl.java` — calculateStreakDays |
| 卡组 CRUD | `FlashcardDeckServiceImpl.java` — createDeck + updateDeck + deleteDeck |
| Fork | `FlashcardDeckServiceImpl.java` — forkDeck (快照式复制) |
| 卡片 CRUD | `FlashcardServiceImpl.java` — createCard + batchCreate + updateCard + deleteCard |
| 题库导入 | `FlashcardServiceImpl.java` — importFromQuestionBank + sourceQuestionId 防重 |
| 学习记录域 | `FlashcardStudyRecord.java` — repetitions + easeFactor + intervalDays + masteryLevel |
| 卡组域 | `FlashcardDeck.java` — isPublic + cardCount + studyCount + forkCount + sourceDeckId |
| 掌握度枚举 | `MasteryLevel.java` — NEW/LEARNING/MASTERED |
| 内容类型枚举 | `CardContentType.java` — TEXT/MARKDOWN/CODE |


## 相关模块

| 模块 | 关系 | 说明 |
| --- | --- | --- |
| [公共底座](/modules/common) | 强依赖 | 闪卡模块依赖公共底座的统一响应、分页和异常处理 |
| [鉴权与用户体系](/modules/auth) | 强依赖 | 闪卡学习和卡组管理需要用户登录态 |
| [用户账户与个人中心](/modules/user-account) | 强依赖 | 用户学习数据依赖用户信息 |
| [积分与抽奖](/modules/points) | 间接关联 | 学习行为可能触发积分奖励 |
| [题库与成长闭环](/modules/interview-and-growth) | 强依赖 | 闪卡可以从题库导入题目 |
| [系统运营后台](/modules/system-ops) | 被依赖 | 闪卡管理界面在管理端 |
