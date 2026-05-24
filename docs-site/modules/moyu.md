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

## 推荐学习顺序

摸鱼工具的子功能多，但每个功能都不算重。学习时不要一口气看完所有页面，建议从“无登录也能理解的运营内容”读到“需要用户状态的记录类功能”。

1. 先看热榜，理解外部接口、Redis 缓存、启动初始化和定时刷新。
2. 再看程序员日历和每日内容，理解运营内容、用户偏好、收藏和管理端维护。
3. 接着看薪资计算器，学习单用户配置、按日期记录和状态流转。
4. 然后看 Bug 商店，理解随机推荐、浏览历史、难度标签和批量导入。
5. 最后把五个子功能对照表结构，观察哪些能力复用收藏表，哪些能力依赖外部数据源。

最小学习案例可以从热榜开始：清空 Redis 热榜缓存，打开页面，观察启动初始化或手动刷新怎样把缓存补回来。

## 源码地图

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

---

## 摸鱼模块深度拆解

以下内容来自对 xiaou-moyu 模块全部源码的逐行阅读，覆盖 5 个 ServiceImpl、6 个 Controller、12 个 Domain、8 个 Mapper、1 个定时任务和 1 个工具类。

### 一、Bug 商店完整业务流

#### 1.1 随机推荐算法

**源码**：`BugStoreServiceImpl.getRandomBug`

```
getRandomBug(userId):
  1. 获取用户最近 2 小时内浏览过的 Bug ID 列表 (最多 50 条)
     SQL: SELECT bug_id FROM user_bug_history
          WHERE user_id=? AND view_time > (NOW() - INTERVAL 2 HOUR)
          ORDER BY view_time DESC LIMIT 50
  2. 调用 selectRandomBug(userId, excludeIds, 1) 获取 1 个随机 Bug
  3. 如果返回 null (所有 Bug 都在排除列表中)
     → 降级: 清除排除列表, 调用 selectRandomBug(userId, null, 1)
  4. 如果仍然 null → 返回 null (Bug 库为空)
  5. 转换为 BugItemDto, 异步记录浏览历史
```

**随机选取 SQL**（Mapper `selectRandomBug`）：

```sql
SELECT * FROM bug_item
WHERE status = 1
  AND id NOT IN (#{excludeIds})
  AND (created_by != #{userId} OR #{userId} IS NULL)
ORDER BY RAND()
LIMIT #{limit}
```

**关键发现**：使用 `ORDER BY RAND()` 进行随机选取。这在数据量大时性能很差——MySQL 需要为每一行生成随机数再排序。Bug 商店数据量通常不大（几百到几千条），问题不明显，但如果 Bug 库增长到万级以上，需要考虑优化。

#### 1.2 浏览历史机制

**源码**：`BugStoreServiceImpl.recordUserView`

```java
UserBugHistory history = new UserBugHistory();
history.setUserId(userId);
history.setBugId(bugId);
history.setViewTime(LocalDateTime.now());
history.setCreateTime(LocalDateTime.now());
userBugHistoryMapper.insert(history);
```

- **无去重**：每次查看同一个 Bug 都会新增一条浏览记录
- **无 TTL 清理**：浏览记录永远不会被清理，`user_bug_history` 表会无限增长
- **2 小时排除窗口**：只排除最近 2 小时内看过的 Bug，超过 2 小时后同一个 Bug 可能再次推荐

#### 1.3 管理端 CRUD

| 操作 | 方法 | 说明 |
| --- | --- | --- |
| 添加 | `addBug` | BeanUtil.copyProperties + 设置默认值 |
| 更新 | `updateBug` | 按 ID 更新 |
| 删除 | `deleteBug` | 物理删除 |
| 批量导入 | `batchImportBugs` | 循环构建 BugItem + insertBatch |

**批量导入注意**：在一个 `@Transactional` 方法内批量插入，如果数据量大可能造成长事务。

### 二、每日内容个性化推荐

**源码**：`DailyContentServiceImpl.getTodayContent`

```
getTodayContent(userId):
  1. 检查用户是否开启每日推送 (dailyContentPush=1)
  2. 获取用户偏好:
     - preferredContentTypes: JSON 数组 [1,2,3,4]
     - preferredLanguages: JSON 数组 ["Java","Python"]
     - difficultyPreference: Integer (1-4)
  3. 如果用户有内容类型偏好:
     for each type in preferredContentTypes:
       优先: selectByUserPreference([type], languages, difficulty, limit=1)
       降级: selectRandomByContentType(type, limit=1)
  4. 如果用户无偏好:
     for each type in [1,2,3,4]:
       selectRandomByContentType(type, limit=1)
  5. 返回今日推荐列表 (最多 4 条)
```

**偏好 SQL**（Mapper `selectByUserPreference`）：

```sql
SELECT * FROM daily_content
WHERE status = 1
  AND content_type IN (#{contentTypes})
  AND (programming_language IN (#{languages}) OR programming_language IS NULL)
  AND (difficulty_level = #{difficulty} OR difficulty_level IS NULL)
ORDER BY RAND()
LIMIT #{limit}
```

**关键发现**：偏好查询使用 `ORDER BY RAND()`，且在循环中逐类型查询（最多 4 次 SQL），这是典型的 N+1 查询。但由于每条只取 1 条记录，实际性能影响不大。

#### 2.1 收藏状态切换

**源码**：`DailyContentServiceImpl.toggleContentCollection`

```
toggleCollection(userId, contentId):
  1. 查询现有收藏: selectByUserIdAndTarget(userId, type=2, contentId)
  2. 如果已存在 → 切换 status (1→0 或 0→1)
  3. 如果不存在 → INSERT 新记录 (status=1)
```

**关键发现**：收藏记录是**软删除/恢复**模式（切换 status），不是物理删除。已取消收藏的记录保留在数据库中。`getUserCollectedContent` 查询时需要过滤 `status=1`。

### 三、程序员日历

**源码**：`DeveloperCalendarServiceImpl`

#### 3.1 日期格式特殊设计

- 数据库存储格式：`MM-dd`（如 `10-24` 程序员节）
- 查询参数格式：`yyyy-MM-dd`（完整日期）
- 服务层转换：提取月日部分进行匹配

```
getTodayEvents():
  today = LocalDate.now().format("MM-dd")
  → selectByEventDate(today)

getEventsByDate(yyyy-MM-dd):
  monthDay = date.substring(5)  // "MM-dd"
  → selectByEventDate(monthDay)
```

**关键发现**：日期截取使用 `substring(5)`，如果输入格式不是 `yyyy-MM-dd`（如 `yyyy-M-d`），截取位置会错位。应使用 `DateTimeFormatter` 进行安全解析。

#### 3.2 收藏与每日内容共用表

`user_calendar_collection` 表通过 `collection_type` 区分：

| collection_type | 含义 | target_id 指向 |
| --- | --- | --- |
| 1 | 日历事件 | `developer_calendar_event.id` |
| 2 | 每日内容 | `daily_content.id` |

**批量查询优化**：`getUserCollectedEvents` 和 `getUserCollectedContent` 都使用 `selectBatchIds` 批量查询，避免了 N+1 问题。

### 四、热榜完整架构

**源码**：`HotTopicServiceImpl` + `HotTopicTask`

#### 4.1 外部 API 对接

```java
@Value("${hot-topic.api.base-url:http://113.44.190.45:9996/api}")
private String baseUrl;
```

- 热榜数据来自外部 API（独立部署的热榜聚合服务）
- 通过 `RestTemplate` 调用
- 返回数据结构: `HotTopicResponse` → `List<HotTopicCategory>` → `List<HotTopicItem>`

#### 4.2 缓存策略

| 维度 | Redis Key | TTL | 说明 |
| --- | --- | --- | --- |
| 分类列表 | `hot_topics:categories` | 15 分钟 | 减少分类接口调用 |
| 平台数据 | `hot_topics:data:{platform}` | 15 分钟 | 每个平台独立缓存 |

#### 4.3 初始化与刷新

| 时机 | 方法 | 行为 |
| --- | --- | --- |
| 应用启动 1 分钟后 | `initializeHotTopicDataIfNeeded` | 串行请求，100ms 间隔，只补缺失缓存 |
| 每 15 分钟 | `refreshHotTopicData` | 并行请求，30s 超时，覆盖式更新 |

**关键发现**：初始化使用**串行请求**（避免启动并发压力），刷新使用**并行请求**（ThreadPoolUtils + 超时控制）。设计合理，但初始化在平台数量多时可能需要较长时间。

#### 4.4 `HotTopicEnum` 平台枚举

覆盖平台包括：微博、知乎、抖音、快手、B站、百度、头条、少数派、IT之家、V2EX、GitHub Trending 等。

### 五、薪资计算器工作状态机

**源码**：`SalaryCalculatorServiceImpl.handleWorkTimeAction`

```
                  START                    PAUSE
    未开始(0) ───────────→ 进行中(1) ────────────→ 暂停中(2)
                               ↑                      │
                               │       RESUME          │
                               └──────────────────────┘
                               │
                     END       │
                               ↓
                           已完成(3)
```

**关键校验**：
- PAUSE 只能在进行中(1)时执行
- RESUME 只能在暂停中(2)时执行
- END 需要先 START
- END 时如果处于暂停中，先计算暂停时长再结算

**收入计算**：

```
actualWorkMinutes = (endTime - startTime) - totalPauseMinutes
workHours = actualWorkMinutes / 60  (保留2位小数)
dailyEarnings = workHours × hourlyRate
```

**关键发现**：暂停时长以**分钟**为单位计算（`Duration.between().toMinutes()`），不足 1 分钟的暂停不计入。如果频繁暂停/恢复，实际暂停时间会被低估（每段暂停最多损失 59 秒精度）。

### 六、深度发现与坑点

#### 6.1 已确认的代码问题

| 编号 | 问题 | 位置 | 影响 |
| --- | --- | --- | --- |
| BUG-1 | `ORDER BY RAND()` 全表扫描 | `BugItemMapper.selectRandomBug` | Bug 库万级以上时性能下降 |
| BUG-2 | 浏览记录无清理机制 | `user_bug_history` 表 | 数据无限增长 |
| BUG-3 | 日期截取使用 substring | `DeveloperCalendarController` | 输入格式不规范时解析错误 |
| BUG-4 | 暂停时长精度为分钟 | `SalaryCalculatorServiceImpl` | 频繁暂停时累计误差 |
| BUG-5 | 收藏统计返回全零 | `DailyContentServiceImpl.getCollectionStatistics` | 统计功能未实现 |
| BUG-6 | 内容统计使用循环查询 | `DailyContentServiceImpl.getContentStatistics` | 4 次 SQL 查各类型数量 |

#### 6.2 设计层面的潜在风险

| 编号 | 风险 | 说明 |
| --- | --- | --- |
| RISK-1 | 外部热榜 API 无熔断 | 如果外部服务长时间不可用，缓存过期后前端无数据 |
| RISK-2 | 批量导入长事务 | `batchImportBugs` 在一个事务内插入全部数据 |
| RISK-3 | MM-dd 日期无法跨年区分 | 同一 MM-dd 的事件每年都会展示，无法设置"只在 2024 年生效" |
| RISK-4 | 点赞/浏览无去重 | 同一用户可重复点赞/浏览，计数可被刷 |

#### 6.3 架构设计亮点

| 编号 | 亮点 | 说明 |
| --- | --- | --- |
| H-1 | 统一收藏表 | 事件和内容共用收藏表，减少表数量 |
| H-2 | 初始化 vs 刷新策略分离 | 启动时串行避免压力，运行时并行提高效率 |
| H-3 | 工作状态机 | START/PAUSE/RESUME/END 四态流转清晰 |
| H-4 | 排除窗口防重复推荐 | 2 小时内不重复推荐同一 Bug |
| H-5 | 批量查询避免 N+1 | 收藏列表使用 selectBatchIds |

#### 6.4 源码导航速查

| 想了解 | 读什么 |
| --- | --- |
| Bug 商店 | `BugStoreServiceImpl.java` — 随机推荐+浏览历史+CRUD |
| 每日内容 | `DailyContentServiceImpl.java` — 个性化推荐+收藏 |
| 开发日历 | `DeveloperCalendarServiceImpl.java` — MM-dd 日期事件 |
| 热榜 | `HotTopicServiceImpl.java` — 外部 API+Redis 缓存 |
| 薪资计算 | `SalaryCalculatorServiceImpl.java` — 状态机+收入计算 |
| 热榜定时任务 | `HotTopicTask.java` — 初始化+定时刷新 |
| 热榜枚举 | `HotTopicEnum.java` — 平台代码/名称/分类 |
| 日期反序列化 | `FlexibleDateDeserializer.java` — 灵活日期格式处理 |
