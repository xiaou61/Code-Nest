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
