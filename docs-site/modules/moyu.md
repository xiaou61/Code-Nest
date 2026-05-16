# 摸鱼工具

摸鱼工具是 Code Nest 的轻运营工具集合，包含热榜、薪资计算器、程序员日历、每日内容和 Bug 商店。它看起来像“娱乐工具”，但源码里涵盖了缓存、外部接口、用户偏好、收藏、批量导入、状态管理和统计，是很适合学习中小型业务模块拆分的样例。

## 功能入口

| 功能 | 用户端入口 | 管理端入口 | 后端入口 |
| --- | --- | --- | --- |
| 工具首页 | `/moyu-tools` | 无 | `xiaou-moyu` |
| 热榜 | `/moyu-tools/hot-topics` | 无 | `/moyu/hot-topic` |
| 薪资计算器 | `/moyu-tools/salary-calculator` | 无 | `/moyu/salary-calculator` |
| 程序员日历 | `/moyu-tools/calendar` | `/moyu/calendar-events` | `/moyu/developer-calendar`、`/admin/moyu/developer-calendar` |
| 每日内容 | `/moyu-tools/daily-content` | `/moyu/daily-content`、`/moyu/statistics` | `/moyu/daily-content`、`/admin/moyu/daily-content` |
| Bug 商店 | `/moyu-tools/bug-store` | `/moyu/bug-store` | `/moyu/bug-store`、`/admin/moyu/bug-store` |

源码位置：

| 层级 | 文件或目录 |
| --- | --- |
| 用户前端 | `vue3-user-front/src/views/moyu-tools/` |
| 管理前端 API | `vue3-admin-front/src/api/moyu.js` |
| 后端 Controller | `xiaou-moyu/src/main/java/com/xiaou/moyu/controller/` |
| 后端 Service | `xiaou-moyu/src/main/java/com/xiaou/moyu/service/impl/` |
| 热榜枚举 | `xiaou-moyu/src/main/java/com/xiaou/moyu/enums/HotTopicEnum.java` |
| 定时任务 | `xiaou-moyu/src/main/java/com/xiaou/moyu/task/HotTopicTask.java` |

## 模块拆分

| 子模块 | 学习重点 |
| --- | --- |
| 热榜 | 外部数据源、Redis 缓存、定时刷新、异步线程池 |
| 薪资计算器 | 用户配置、按日工作记录、状态流转、派生金额计算 |
| 程序员日历 | MM-dd 循环日期、用户偏好、收藏、管理端运营 |
| 每日内容 | 内容类型、语言偏好、推荐、浏览/点赞/收藏统计 |
| Bug 商店 | 随机内容、浏览历史、难度和技术标签、批量导入 |

## 热榜

接口：

| 接口 | 说明 |
| --- | --- |
| `GET /moyu/hot-topic/categories` | 返回平台分类 |
| `GET /moyu/hot-topic/data/{platform}` | 返回单个平台热榜 |
| `GET /moyu/hot-topic/data/all` | 返回所有平台热榜 |
| `POST /moyu/hot-topic/refresh` | 手动刷新热榜 |

`HotTopicEnum` 定义平台代码、名称和分类。当前覆盖社交媒体、知识社区、新闻资讯、科技数码、娱乐生活和实用信息等分组。

热榜缓存规则：

| 项 | 说明 |
| --- | --- |
| 缓存前缀 | `moyu:hot-topic:` |
| 分类缓存 | `moyu:hot-topic:categories` |
| 平台缓存 | `moyu:hot-topic:data:{platform}` |
| 定时刷新 | 每 15 分钟执行一次 |
| 启动初始化 | 启动 1 分钟后执行，只请求缺失缓存的平台 |
| 线程池 | `hotTopicExecutor`，核心线程 3，最大线程 8，队列 50 |

读这段源码时要注意一个产品边界：热榜依赖外部数据源，必须能接受“某个平台失败但整个工具不崩”。所以文档、前端和日志都应该把“缓存为空、外部接口失败、单平台失败”当作正常异常路径。

## 薪资计算器

核心接口：

| 接口 | 说明 |
| --- | --- |
| `GET /moyu/salary-calculator/data` | 读取今日、周、月薪资面板数据 |
| `GET /moyu/salary-calculator/config` | 读取薪资配置 |
| `POST /moyu/salary-calculator/config` | 保存或更新薪资配置 |
| `DELETE /moyu/salary-calculator/config` | 删除配置 |
| `POST /moyu/salary-calculator/work-time` | 开始、暂停、恢复、结束工作 |

数据表：

| 表 | 作用 |
| --- | --- |
| `user_salary_config` | 用户月薪、每月工作天数、每日工时、自动计算时薪 |
| `work_record` | 每个用户每天一条工作记录，记录开始/结束/暂停时间和当日收入 |

工作状态：

| 值 | 含义 |
| --- | --- |
| `0` | 未开始 |
| `1` | 进行中 |
| `2` | 暂停中 |
| `3` | 已完成 |

工作动作：

| 动作 | 结果 |
| --- | --- |
| `START` | 记录开始时间，状态变为进行中 |
| `PAUSE` | 记录暂停开始时间，状态变为暂停中 |
| `RESUME` | 累加暂停分钟数，状态回到进行中 |
| `END` | 记录结束时间，扣除暂停时长，计算工作小时和收入 |

计算口径：`work_record.daily_earnings = 实际工作小时 * user_salary_config.hourly_rate`。`hourly_rate` 在数据库里由月薪、工作天数和每日工时生成。

## 程序员日历

用户侧接口：

| 接口 | 说明 |
| --- | --- |
| `GET /moyu/developer-calendar/today` | 今日推荐 |
| `GET /moyu/developer-calendar/month/{year}/{month}` | 月视图 |
| `GET /moyu/developer-calendar/events/{date}` | 指定日期事件，日期格式 `yyyy-MM-dd` |
| `GET /moyu/developer-calendar/events/type/{eventType}` | 按类型查询 |
| `GET /moyu/developer-calendar/events/major` | 重要事件 |
| `GET /moyu/developer-calendar/preference` | 用户偏好 |
| `POST /moyu/developer-calendar/preference` | 保存偏好 |
| `POST /moyu/developer-calendar/events/{eventId}/toggle-collection` | 收藏/取消收藏 |
| `GET /moyu/developer-calendar/collections/events` | 收藏事件 |

核心表：

| 表 | 字段重点 |
| --- | --- |
| `developer_calendar_event` | `event_date` 使用 `MM-dd`，每年循环；`event_type` 1 程序员节日、2 技术纪念日、3 开源节日 |
| `user_calendar_preference` | 事件提醒、每日内容推送、偏好语言、偏好内容类型、难度偏好、通知时间 |
| `user_calendar_collection` | 统一收藏表，`collection_type = 1` 表示日历事件 |

管理端可以新增、更新、删除、批量删除、启停事件，并查看事件统计。

## 每日内容

内容类型：

| 值 | 含义 |
| --- | --- |
| `1` | 编程格言 |
| `2` | 技术小贴士 |
| `3` | 代码片段 |
| `4` | 历史上的今天 |

用户侧能力：

| 接口 | 说明 |
| --- | --- |
| `GET /moyu/daily-content/today` | 今日内容推荐 |
| `GET /moyu/daily-content/type/{contentType}` | 按类型查询 |
| `GET /moyu/daily-content/random/{contentType}` | 随机内容 |
| `GET /moyu/daily-content/recommend` | 按用户偏好推荐 |
| `GET /moyu/daily-content/popular` | 热门内容 |
| `GET /moyu/daily-content/language/{language}` | 按语言查询 |
| `POST /moyu/daily-content/{contentId}/view` | 浏览计数 |
| `POST /moyu/daily-content/{contentId}/like` | 点赞计数 |
| `POST /moyu/daily-content/{contentId}/toggle-collection` | 收藏/取消收藏 |
| `GET /moyu/daily-content/collections` | 我的收藏内容 |

每日内容和程序员日历共用 `user_calendar_collection`。区别是：

| 收藏类型 | 含义 |
| --- | --- |
| `1` | 日历事件 |
| `2` | 每日内容 |

这是一种常见设计：同一个收藏表通过 `collection_type + target_id` 支撑多个轻量业务。

## Bug 商店

Bug 商店不是缺陷跟踪系统，而是“把典型 Bug 当成知识商品随机推荐”的小工具。

核心表：

| 表 | 说明 |
| --- | --- |
| `bug_item` | 标题、现象、原因分析、解决方案、技术标签、难度、状态、排序 |
| `user_bug_history` | 用户看过哪些 Bug，`user_id + bug_id` 唯一 |

难度等级：

| 值 | 含义 |
| --- | --- |
| `1` | 初级 |
| `2` | 中级 |
| `3` | 高级 |
| `4` | 专家级 |

管理端能力包括列表、详情、新增、更新、删除和批量导入。批量导入时要校验标题、现象、原因分析、解决方案等必要字段，否则会生成不可学习的空内容。

## 表结构速查

| 表 | 关键字段 |
| --- | --- |
| `daily_content` | `content_type`、`programming_language`、`difficulty_level`、`view_count`、`like_count`、`status` |
| `developer_calendar_event` | `event_date`、`event_type`、`is_major`、`blessing_text`、`related_links`、`status` |
| `bug_item` | `title`、`phenomenon`、`cause_analysis`、`solution`、`tech_tags`、`difficulty_level`、`status` |
| `user_calendar_preference` | `event_reminder`、`daily_content_push`、`preferred_languages`、`preferred_content_types` |
| `user_calendar_collection` | `collection_type`、`target_id`、`status` |
| `user_salary_config` | `monthly_salary`、`work_days_per_month`、`work_hours_per_day`、`hourly_rate` |
| `work_record` | `work_date`、`work_hours`、`daily_earnings`、`work_status`、`total_pause_minutes` |
| `user_bug_history` | `user_id`、`bug_id`、`view_time` |

## 验证清单

- 热榜首次打开时，如果 Redis 为空，启动初始化任务会补缓存。
- 单个平台外部接口失败，不影响其他平台展示。
- 薪资配置保存后，`user_salary_config` 只有当前用户一条有效配置。
- 工作时间按 `START -> PAUSE -> RESUME -> END` 流转，暂停时间会被扣除。
- 日历事件使用 `MM-dd` 循环日期，查询 `yyyy-MM-dd` 时会取月日匹配。
- 每日内容点赞和浏览能更新计数。
- 收藏事件和收藏内容都写入 `user_calendar_collection`，但 `collection_type` 不同。
- Bug 商店随机推荐后，会写入或更新用户浏览历史。

## 常见坑

| 问题 | 原因 | 处理 |
| --- | --- | --- |
| 热榜偶发为空 | 外部数据源失败或缓存未初始化 | 查 `moyu:hot-topic:data:*` 和定时任务日志 |
| 薪资开始工作失败 | 用户未保存薪资配置 | 先调用配置保存接口 |
| 同一天工作记录重复 | `work_record` 对 `user_id + work_date` 做唯一约束 | 更新当天记录，不要重复插入 |
| 日历按日期查不到事件 | 入参是 `yyyy-MM-dd`，库里是 `MM-dd` | 服务层要转换月日 |
| 收藏状态错乱 | 事件和内容共用收藏表 | 同时检查 `collection_type` 和 `target_id` |
| 批量导入 Bug 后质量差 | 只校验了格式，未校验内容完整度 | 管理端导入前增加字段和长度检查 |
