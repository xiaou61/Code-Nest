# 数据库字段阅读指南

这页是 [数据表索引](/reference/database-tables) 的下一层说明。数据表索引告诉你“有哪些表”，本页告诉你“常见字段怎么读、怎么改、怎么验证”。

Code Nest 经历了多个版本，字段风格并不完全统一。读表时不要只看字段名，要结合模块页、Mapper、Service 和状态流转一起判断。

## 通用字段

| 字段 | 常见含义 | 阅读建议 |
| --- | --- | --- |
| `id` | 主键 ID | 多数为业务表自增主键，前端不要自己生成 |
| `user_id` | 普通用户 ID | 用户端接口必须从登录态取当前用户，不信任前端传参 |
| `admin_id`、`created_by` | 管理员或创建人 | 管理端操作、审计和后台数据归属常用 |
| `create_time`、`created_time` | 创建时间 | 不同表命名不完全一致，查询排序前先看实际列名 |
| `update_time`、`updated_time` | 更新时间 | 多数由数据库 `ON UPDATE` 或业务代码维护 |
| `status` | 状态 | 各模块含义不同，不能跨表套用 |
| `is_deleted`、`deleted` | 逻辑删除 | 有些表用 `0/1`，有些把删除放进 `status` |
| `sort_order` | 排序值 | 分类、标签、配置、版本等展示顺序常用 |
| `remark`、`description` | 备注或描述 | 通常不参与核心状态判断 |

最容易出错的是 `status`。它在版本、知识图谱、博客、聊天、OJ、学习资产里都出现，但含义完全不同。

## 状态字段不要通用化

下面是常见状态字段的读法：

| 表或模块 | 字段 | 示例含义 |
| --- | --- | --- |
| `version_history` | `status` | `0` 草稿、`1` 已发布、`2` 已隐藏 |
| `oj_problem` | `status` | `0` 隐藏、`1` 公开 |
| `oj_submission` | `status` | `pending`、判题中、AC/WA/CE/TLE 等判题状态 |
| `oj_contest` | `status` | `0` 草稿、`1` 即将开始、`2` 进行中、`3` 已结束 |
| `blog_article` | `status` | 草稿、发布、下架、删除 |
| `chat_messages` | `is_deleted` | `0` 未删除、`1` 已删除或撤回 |
| `study_team_application` | `status` | 待审核、通过、拒绝、取消 |
| `learning_asset_record` | `status` | `PENDING_CONFIRM` 等学习资产生成状态 |
| `learning_asset_candidate` | `status` | `DRAFT` 等候选资产状态 |

新增状态字段时，至少同步：

1. 后端枚举或常量。
2. 前端展示文案和筛选条件。
3. 模块页状态机或核心流程。
4. API 调用示例或验证记录里的失败态。

## 用户归属字段

`user_id` 是用户端数据隔离的核心字段。常见模式：

| 场景 | 代表表 | 读法 |
| --- | --- | --- |
| 用户资料 | `user_info` | 用户主表，其他用户表通常引用它 |
| 学习记录 | `interview_learn_record`、`interview_mastery_record`、`flashcard_study_record` | 一个用户对某个题、卡片、日期的状态 |
| 内容创作 | `community_post`、`moments`、`blog_article`、`code_pen` | 内容作者或发布者 |
| 行为关系 | `community_post_like`、`community_post_collect`、`moment_favorites` | 用户对目标内容的行为 |
| 积分 | `user_points_balance`、`user_points_detail` | 余额和流水必须按用户隔离 |
| OJ | `oj_submission`、`oj_contest_participant` | 提交和报名归属用户 |

后端写接口时：

```text
当前 user_id = 登录态中的用户 ID
不是请求体里的 userId
```

只有管理端查询、审核或运维接口才允许按用户 ID 过滤别人的数据。

## 逻辑删除字段

项目里逻辑删除有几种写法：

| 写法 | 代表表 | 说明 |
| --- | --- | --- |
| `is_deleted = 0/1` | `chat_messages`、学习小组部分表 | 明确删除标记 |
| `deleted = 0/1` | `version_history`、`sql_optimize_record` | 旧模块或工具模块常见 |
| `status` 表示删除 | `blog_article`、评论类表 | 删除是状态机的一种状态 |

删除接口验证时不要只看接口返回成功，还要检查：

1. 列表是否过滤掉已删除数据。
2. 详情是否禁止访问或展示已删除状态。
3. 管理端是否还能审计或强制删除。
4. 唯一索引是否允许删除后重新创建。

## 文件相关字段

文件模块核心表包括 `file_info`、`file_access`、`file_storage`、`storage_config`、`file_migration`、`file_system_setting`。

| 字段 | 常见含义 | 重点 |
| --- | --- | --- |
| `file_id` | 文件记录 ID | 业务表保存文件引用时优先保存 ID 或后端返回 URL |
| `file_size` | 文件字节数 | 上传限制和存储统计会用 |
| `storage_config_id` | 存储配置 ID | 本地、对象存储、多存储迁移会用 |
| `is_public` | 是否公开读取 | 公开文件可匿名读，私有文件要登录 |
| `business_type` | 业务类型 | 头像、聊天图片、附件等场景区分 |
| `access_count` | 访问次数 | 文件访问统计和运营分析使用 |

文件链路变更时至少验证上传、URL 获取、公开读取、私有读取和管理端文件列表。

## 通知相关字段

通知核心表包括 `notification`、`notification_template`、`notification_config`、`notification_user_read_record`。

| 字段 | 常见含义 | 重点 |
| --- | --- | --- |
| `receiver_id` | 接收用户 | `null` 可能代表公告或全员通知 |
| `sender_id` | 发送者 | 系统消息可为空或固定系统身份 |
| `type` | 通知类型 | 系统通知、互动提醒、公告等 |
| `source_module` | 来源模块 | 排查某条通知从哪个业务触发 |
| `source_id` | 来源业务 ID | 回跳详情页或追踪业务数据 |
| `status` | 通知状态 | 未读、已读、删除等 |
| `read_time` | 读取时间 | 已读状态验证依据 |

通知问题排查时，先查业务事件是否创建通知，再查接收人和已读记录。

## 积分和抽奖字段

积分表要重点区分“余额”和“流水”。

| 表 | 关键字段 | 说明 |
| --- | --- | --- |
| `user_points_balance` | `user_id`、`total_points` | 用户当前积分余额 |
| `user_points_detail` | `points_change`、`points_type`、`source_type`、`source_id` | 每次积分变化的来源和数量 |
| `user_checkin_bitmap` | 年月、位图字段 | 签到日历和重复签到判断 |
| `lottery_draw_record` | `prize_points`、`cost_points` | 抽奖获得和消耗 |
| `lottery_prize_config` | 概率、库存、奖品积分 | 奖品配置 |
| `user_lottery_limit` | 用户限制 | 每日或周期抽奖限制 |

积分变更的验证口径：

```text
余额变化 = 流水合计 = 业务动作预期
```

如果余额和流水不一致，优先查事务、重复提交、幂等和异常回滚。

## OJ 字段

OJ 的关键字段集中在题目、测试用例、提交和赛事。

| 表 | 字段 | 说明 |
| --- | --- | --- |
| `oj_problem` | `status`、`difficulty`、`time_limit`、`memory_limit` | 题目展示和判题限制 |
| `oj_test_case` | 输入、输出、是否样例 | 判题依据 |
| `oj_submission` | `problem_id`、`user_id`、`language`、`code`、`status`、`result` | 提交状态和判题结果 |
| `oj_submission` | `contest_id` | 为空是普通提交，不为空是赛事提交 |
| `oj_contest` | `status`、开始/结束时间 | 赛事状态 |
| `oj_contest_participant` | `contest_id`、`user_id` | 报名关系 |

OJ 变更不要只查 `oj_submission.status`。还要看 go-judge 是否可达、详情是否回写、赛事榜单和积分奖励是否受影响。

## AI 和学习资产字段

AI 场景往往不是只落一张表。

| 场景 | 代表表 | 字段重点 |
| --- | --- | --- |
| 模拟面试 | `mock_interview_session`、`mock_interview_qa` | 会话状态、问题、回答、评分、反馈 |
| 求职作战台 | `job_battle_plan_record` | 用户、计划名称、分析结果、创建时间 |
| 求职闭环 | `career_loop_session`、`career_loop_action` | 阶段、动作状态、快照 |
| SQL 优化 | `sql_optimize_record` | 原 SQL、优化 SQL、分析结果、收藏和删除 |
| 学习资产 | `learning_asset_record`、`learning_asset_candidate`、`learning_asset_publish_log` | 生成记录、候选状态、发布日志 |

AI 相关字段验证时要同时看：

1. 模型调用是否成功。
2. 结构化字段是否完整。
3. 前端展示是否能读懂字段。
4. 失败或兜底时字段是否有默认值。
5. 学习资产、通知或统计是否跟着回流。

## 索引和唯一约束

常见唯一约束模式：

| 约束 | 代表含义 |
| --- | --- |
| `uk_user_id` | 一个用户只有一份配置或余额 |
| `uk_user_date` | 一个用户一天只有一条记录 |
| `uk_user_question` | 一个用户对一道题只有一条学习记录 |
| `uk_contest_user` | 一个用户只能报名一次赛事 |
| `uk_user_notification` | 一个用户对一条公告只生成一次已读记录 |

写接口时如果遇到唯一约束冲突，不要简单吞掉异常。要判断是幂等成功、业务冲突，还是前端重复提交。

## 新增字段检查清单

新增或调整字段时，按下面检查：

1. 是否补了版本 SQL，并同步主库基线。
2. 实体、DTO、VO、Mapper XML 是否同步。
3. 前端表单、列表、详情页是否同步。
4. API 路由索引和模块页是否说明字段影响。
5. 如果是状态字段，是否补状态机和失败态。
6. 如果是金额、积分、库存、次数，是否考虑幂等和事务。
7. 如果是用户归属字段，是否从登录态取值。
8. 如果是文件、通知、AI、OJ 字段，是否跑对应链路验证。

## 快速记录模板

```text
表名：
新增或修改字段：
字段类型和默认值：
所属业务状态：
读写接口：
前端展示位置：
索引或唯一约束：
数据迁移影响：
最低验证：
```



## 相关文档

| 文档 | 说明 |
| --- | --- |
| [数据表索引](/reference/database-tables) | 全部表清单 |
| [数据库与脚本](/architecture/database) | 数据库设计 |
| [术语表](/reference/glossary) | 字段命名解释 |
| [模块总览](/modules/) | 各模块数据表分布 |
