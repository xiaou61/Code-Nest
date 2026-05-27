# 数据库设计

Code-Nest 使用单一 MySQL 数据库 `code_nest`，当前基线包含 **136 张表**，加上版本增量脚本后共 **142 张表**，采用"基线 + 版本增量"的脚本组织方式。

## 设计原则

| 原则 | 说明 |
|------|------|
| 单库多表 | 所有模块共享 `code_nest` 库，通过表名前缀区分模块 |
| snake_case | 表名和字段名统一小写下划线风格 |
| utf8mb4 | 全库默认字符集，支持 Emoji 和中文 |
| 逻辑删除 | 大部分表使用 `is_deleted` 字段，不物理删除 |
| 审计字段 | `created_time` / `updated_time` 由 MyBatis-Plus 自动填充 |
| ID 策略 | 雪花算法 (Snowflake) 生成 Long 型主键 |

## 表名前缀与模块映射

| 前缀 | 模块 | 表数 | 说明 |
|------|------|------|------|
| `blog_` | xiaou-blog | 4 | 博客文章/分类/标签/配置 |
| `bug_` | xiaou-moyu | 1 | Bug 商店 |
| `career_loop_` | xiaou-mock-interview | 4 | 求职闭环 |
| `chat_` | xiaou-chat | 5 | 聊天室/消息/在线用户/禁言/敏感词 |
| `code_pen_` | xiaou-codepen | 8 | 代码工坊/收藏/评论/文件夹/Fork/标签 |
| `community_` | xiaou-community | 9 | 社区帖子/评论/分类/标签/点赞/收藏 |
| `daily_content` | xiaou-moyu | 1 | 每日内容 |
| `developer_calendar_` | xiaou-moyu | 1 | 程序员日历 |
| `file_` | xiaou-filestorage | 5 | 文件信息/存储/访问/迁移/系统设置 |
| `flashcard` | xiaou-flashcard | 9 | 闪卡/卡组/学习记录/复习日志/统计 |
| `interview_` | xiaou-interview | 8 | 面试题/分类/收藏/掌握度/题单/学习记录 |
| `job_battle_` | xiaou-mock-interview | 1 | 求职对战 |
| `knowledge_` | xiaou-knowledge | 2 | 知识图谱节点/关系 |
| `lottery_` | xiaou-points | 4 | 抽奖配置/记录/调整历史/统计 |
| `mock_interview_` | xiaou-mock-interview | 4 | 模拟面试/方向/QA/统计 |
| `moment` / `moments` | xiaou-moment | 5 | 动态/评论/点赞/收藏/浏览 |
| `notification` | xiaou-notification | 4 | 通知/配置/模板/已读记录 |
| `oj_` | xiaou-oj | 9 | 题目/提交/测试用例/题解/赛事/标签 |
| `plan_` | xiaou-plan | 2 | 计划/签到记录 |
| `resume_` | xiaou-resume | 6 | 简历/模板/版本/分析/分享/段落 |
| `sensitive_` | xiaou-sensitive | 11 | 敏感词/策略/分类/日志/统计/白名单/版本 |
| `sql_monitor_` | xiaou-sql-optimizer | 1 | SQL 监控日志 |
| `storage_` | xiaou-filestorage | 1 | 存储配置 |
| `study_team_` | xiaou-team | 10 | 小组/成员/签到/讨论/任务/申请/统计 |
| `sys_` | xiaou-system | 7 | 管理员/角色/权限/日志 |
| `user_` | xiaou-user/points/plan | 10 | 用户信息/积分/计划/签到/抽奖限制/日历 |
| `growth_autopilot_` | xiaou-plan | 3 | 成长自动导航目标/任务/事件 |
| `learning_cockpit_` | xiaou-application | 1 | 学习驾驶舱排名快照 |
| `version_history` | xiaou-version | 1 | 版本历史 |
| `work_record` | xiaou-moyu | 1 | 工作记录 |

## 核心表结构

### 用户与权限

```text
user_info (用户主表)
├── id (BIGINT, 雪花ID)
├── username / email / password
├── nickname / avatar / bio
├── status (0=正常, 1=禁用)
├── created_time / updated_time
└── is_deleted

sys_admin (管理员)
├── id / username / password / nickname
└── status

sys_role → sys_role_permission → sys_permission (RBAC 表, 当前未启用)
```

### OJ 判题

```text
oj_problem (题目)
├── id / title / content / difficulty
├── time_limit / memory_limit
└── status

oj_submission (提交)
├── id / problem_id / user_id
├── language / code
├── status (PENDING→JUDGING→AC/WA/TLE/MLE/CE/RE/SE)
├── execute_time / memory_usage
└── judge_response

oj_test_case (测试用例)
oj_solution (题解)
oj_contest / oj_contest_participant / oj_contest_problem (赛事)
```

### 聊天室

```text
chat_rooms (聊天室)
├── id / name / type (1=一对一, 2=群组)
└── creator_id

chat_messages (消息)
├── id / room_id / sender_id / content
├── type (TEXT/IMAGE/SYSTEM)
├── is_recalled (撤回标记)
└── created_time

chat_online_users (在线用户, Redis 辅助)
chat_user_bans (禁言记录)
```

### 积分与抽奖

```text
user_points_balance (积分余额)
├── user_id / balance / total_earned / total_spent
└── updated_time

user_points_detail (积分明细)
├── user_id / type (EARN/SPEND) / amount
├── source_module / source_id
└── description

lottery_prize_config (奖品配置)
lottery_draw_record (抽奖记录)
lottery_adjust_history (调整历史)
lottery_statistics_daily (日统计)
user_lottery_limit (用户限制/风控)
```

### 学习小组

```text
study_team (小组)
├── id / name / description / avatar
├── creator_id / max_members
├── status (0=活跃, 1=解散)
└── created_time

study_team_member (成员)
├── team_id / user_id / role (OWNER/ADMIN/MEMBER)
└── joined_time

study_team_checkin (签到)
study_team_discussion (讨论)
study_team_task (任务)
study_team_application (申请)
study_team_daily_stats (日统计)
```

### 敏感词体系

```text
sensitive_word (敏感词库)
├── id / word / category_id
├── level (1=替换, 2=审核, 3=拦截)
└── is_enabled

sensitive_strategy (策略)
sensitive_category (分类)
sensitive_log (检测日志)
sensitive_hit_statistics (命中统计)
sensitive_whitelist (白名单)
sensitive_version (词库版本)
sensitive_source (词库来源)
sensitive_homophone (同音字)
sensitive_similar_char (形近字)
sensitive_user_violation (用户违规记录)
```

## 数据库视图

| 视图 | 说明 |
|------|------|
| `v_user_unread_notifications` | 用户未读通知视图，JOIN notification + notification_user_read_record |

## 版本增量脚本

数据库变更通过版本目录组织，每个版本对应一个增量 SQL 文件：

| 版本 | 主要变更 | 涉及模块 |
|------|----------|----------|
| v1.2.0 | 初始基线 | 全部 |
| v1.2.1 | 敏感词、知识图谱 | sensitive, knowledge |
| v1.3.0 | 版本历史、程序员日历、Bug 商店 | version, moyu |
| v1.4.0 | 积分体系、聊天室 | points, chat |
| v1.5.0 | 博客、社区、动态、抽奖、敏感词升级 | blog, community, moment, points, sensitive |
| v1.6.0 | 简历、代码工坊 | resume, codepen |
| v1.6.1 | 计划打卡 | plan |
| v1.6.3 | 模拟面试 | mock-interview |
| v1.7.0 | 面试学习记录、聊天室升级、学习追踪 | interview, chat |
| v1.7.1 | 学习小组 | team |
| v1.7.2 | 闪卡 | flashcard |
| v1.8.0 | OJ 题目、判题、题解 | oj |
| v1.8.1 | OJ 赛事 | oj |
| v1.8.2 | 求职闭环、学习驾驶舱、SQL 优化、OJ 评论 | mock-interview, oj, sql-optimizer |
| v1.8.4 | 学习资产转化引擎 | learning-asset |

## 索引策略

当前索引策略以**主键索引 + 外键字段索引**为主，部分高频查询场景有额外索引：

| 场景 | 索引字段 | 说明 |
|------|----------|------|
| 用户登录 | `username`, `email` | 唯一索引，登录查询 |
| 帖子列表 | `community_post.category_id`, `created_time` | 分类筛选 + 时间排序 |
| 提交记录 | `oj_submission.problem_id`, `user_id`, `status` | 题目/用户/状态筛选 |
| 聊天消息 | `chat_messages.room_id`, `created_time` | 聊天记录分页 |
| 积分明细 | `user_points_detail.user_id`, `created_time` | 用户积分流水 |
| 签到记录 | `plan_checkin_record.plan_id`, `checkin_date` | 签到去重 |
| 敏感词 | `sensitive_word.word` | DFA 匹配 |
| 操作日志 | `sys_operation_log.created_time` | 日志查询 |

## 数据生命周期

| 数据类型 | 保留策略 | 清理方式 |
|----------|----------|----------|
| 聊天消息 | 永久保留 | 撤回使用 is_recalled 标记 |
| 操作日志 | 保留 90 天 | 定时任务清理 |
| 提交记录 | 永久保留 | — |
| 积分明细 | 永久保留 | — |
| 文件信息 | 跟随业务 | 业务删除时标记 is_deleted |
| 在线用户 | 实时 | Redis 过期自动清理 |
| 通知 | 永久保留 | 用户标记已读/删除 |

## Redis 数据分布

Redis 使用多个 database 索引隔离不同数据：

| Database | 用途 | 主要 Key 模式 |
|----------|------|---------------|
| db3 | 业务数据 (Redisson) | `xiaou:*`, 分布式锁, 限流计数器 |
| db4 | Sa-Token 会话 | `satoken:*`, Token 存储 |

**Redisson 主要用途**：

| 用途 | 说明 |
|------|------|
| 分布式锁 | 防止重复签到、重复提交 |
| 限流 | API 限流、抽奖限流 |
| 实时状态 | 在线用户列表、WebSocket 会话 |
| 缓存 | 热门帖子、用户信息缓存 |
| AI 运行观测 | `xiaou:ai:runtime:metrics` |

## 脚本组织

```text
sql/
├── MySql/
│   ├── code_nest.sql          ← 完整结构基线 (136 表)
│   └── code_nest_data.sql     ← 初始化数据
├── v1.2.0/                    ← 初始版本
├── v1.2.1/                    ← 敏感词/知识图谱增量
├── v1.3.0/                    ← 版本历史/日历/Bug商店增量
├── v1.4.0/                    ← 积分/聊天室增量
├── v1.5.0/                    ← 博客/社区/动态增量
├── v1.6.0/                    ← 简历/代码工坊增量
├── v1.6.1/                    ← 计划打卡增量
├── v1.6.3/                    ← 模拟面试增量
├── v1.7.0/                    ← 面试学习记录增量
├── v1.7.1/                    ← 学习小组增量
├── v1.7.2/                    ← 闪卡增量
├── v1.8.0/                    ← OJ 判题增量
├── v1.8.1/                    ← OJ 赛事增量
├── v1.8.2/                    ← 求职闭环/SQL优化增量
└── v1.8.4/                    ← 学习资产增量
```

## 新增脚本原则

1. 结构变更写入版本增量目录，不直接覆盖历史脚本。
2. 表名和字段使用 `snake_case`。
3. 默认字符集保持 `utf8mb4`。
4. 线上兼容优先，谨慎使用破坏性 `DROP`。
5. 初始化数据和结构变更拆清执行顺序。
6. 新增功能文档要写清涉及的表、核心索引和数据生命周期。

## 脚本检查清单

提交数据库变更前，至少确认：

1. 基线脚本和版本增量脚本的职责没有混用。
2. 新表使用 `utf8mb4`，字段命名保持 `snake_case`。
3. 增量脚本能在已有库上重复评审，不依赖手工改表。
4. 初始化数据和结构脚本执行顺序清楚。
5. 对应模块文档和 [数据表索引](/reference/database-tables) 已同步更新。

## 源码导航

| 路径 | 说明 |
|------|------|
| `sql/MySql/code_nest.sql` | 完整结构基线 |
| `sql/MySql/code_nest_data.sql` | 初始化数据 |
| `sql/v1.7.1/study_team.sql` | 学习小组增量脚本示例 |
| `xiaou-common/.../config/MybatisPlusConfig.java` | MyBatis-Plus 配置 (分页/自动填充) |
| `xiaou-application/.../application-dev.yml` | 数据源配置 (P6Spy 代理) |


## 相关文档

| 文档 | 说明 |
| --- | --- |
| [架构总览](/architecture/overview) | 整体架构和部署拓扑 |
| [后端模块](/architecture/backend-modules) | 后端 Maven 子模块详解 |
| [数据表索引](/reference/database-tables) | 全部 142 张表清单 |
| [数据库字段阅读指南](/reference/database-field-guide) | 命名规范和类型说明 |
| [模块总览](/modules/) | 各模块的数据表分布 |
