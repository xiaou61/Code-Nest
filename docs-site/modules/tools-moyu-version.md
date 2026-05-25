# 工具、摸鱼与版本

这一组能力偏向轻工具、运营内容和产品版本展示。它们不像 OJ、社区、AI Runtime 那样重业务流程，但非常影响用户日常体验：开发者工具解决即时小需求，摸鱼工具增加轻量互动，版本历史让用户知道产品在持续进化。

## 推荐学习顺序

这组模块可以按"无后端 -> 有运营内容 -> 正式公告"的顺序学习：

1. 先读开发者工具，理解纯前端工具如何避免不必要的上传和后端依赖。
2. 再读摸鱼工具，理解轻量内容模块为什么需要后台运营、缓存和定时任务。
3. 最后读版本历史，理解正式产品公告的草稿、发布、隐藏和批量操作。

## 源码地图

| 能力 | 源码入口 | 说明 |
| --- | --- | --- |
| 开发者工具页面 | `vue3-user-front/src/views/dev-tools` | 4 个 Vue 组件，纯前端 |
| 开发者工具导航 | `vue3-user-front/src/components/DevToolsNav.vue` | 公共导航栏 |
| 摸鱼用户端页面 | `vue3-user-front/src/views/moyu-tools` | 多个工具页 |
| 摸鱼管理端页面 | `vue3-admin-front/src/views/moyu` | 后台管理页 |
| 摸鱼后端 | `xiaou-moyu/src/main/java/com/xiaou/moyu` | 7 个 Controller |
| 摸鱼后台 API 封装 | `vue3-admin-front/src/api/moyu.js` | Axios 调用 |
| 版本历史后端 | `xiaou-version/src/main/java/com/xiaou/version` | 版本管理 |
| 版本历史管理端 | `vue3-admin-front/src/views/system/version/index.vue` | 版本 CRUD |
| 版本历史 API 封装 | `vue3-admin-front/src/api/version.js` | Axios 调用 |

## 功能地图

| 功能 | 用户端入口 | 管理端入口 | 后端模块 | 深入文档 |
| --- | --- | --- | --- | --- |
| 开发者工具 | `/dev-tools` 等 | 暂无后台 | 前端为主 | [开发者工具](/modules/dev-tools) |
| 摸鱼工具首页 | `/moyu-tools` | `/moyu/*` | `xiaou-moyu` | [摸鱼工具](/modules/moyu) |
| 程序员日历 | `/moyu-tools/calendar` | `/moyu/calendar-events` | `xiaou-moyu` | [摸鱼工具](/modules/moyu) |
| 热榜 | `/moyu-tools/hot-topics` | 暂无独立后台 | `xiaou-moyu` | [摸鱼工具](/modules/moyu) |
| 每日内容 | `/moyu-tools/daily-content` | `/moyu/daily-content` | `xiaou-moyu` | [摸鱼工具](/modules/moyu) |
| 薪资计算器 | `/moyu-tools/salary-calculator` | 暂无后台 | `xiaou-moyu` | [摸鱼工具](/modules/moyu) |
| Bug 商店 | `/moyu-tools/bug-store` | `/moyu/bug-store` | `xiaou-moyu` | [摸鱼工具](/modules/moyu) |
| 版本历史 | `/version-history` | `/system/version` | `xiaou-version` | [版本历史](/modules/version-history) |

## 三类能力的边界

| 类别 | 主要目标 | 是否依赖后端 | 是否需要运营 | 数据存储 |
| --- | --- | --- | --- | --- |
| 开发者工具 | 即用即走，提高开发效率 | 当前大多不依赖 | 不需要 | localStorage |
| 摸鱼工具 | 轻量内容、娱乐、日常访问 | 依赖后端和 Redis | 需要 | MySQL + Redis |
| 版本历史 | 产品更新展示和公告 | 依赖后端 | 需要 | MySQL |

开发者工具强调隐私和本地处理；摸鱼工具强调内容质量、缓存刷新和互动；版本历史强调发布流程、状态控制和文案准确。

## 开发者工具

当前开发者工具主要是前端纯工具：

| 工具 | 实现特点 | 登录要求 |
| --- | --- | --- |
| JSON 工具 | `JSON.parse`、`JSON.stringify`、格式化、压缩、校验 | 否 |
| 文本比对 | 前端本地行级/LCS 比对 | 否 |
| 聚合翻译 | 编码用户输入后跳转到第三方翻译页面 | 否 |

设计原则：

- 不把用户输入上传到业务后端。
- 能在浏览器本地完成的计算优先本地完成。
- 新增工具时先确认是否需要历史记录、用户账号、服务端任务或审计。
- 如果接入外部 API，要补隐私说明、失败提示和频率限制。

详细实现看 [开发者工具](/modules/dev-tools)。

## 摸鱼工具

摸鱼工具包含多种轻量能力。后端模块是 `xiaou-moyu`，有 7 个 Controller：

### 用户端 Controller

| Controller | 路由前缀 | 能力 |
| --- | --- | --- |
| `HotTopicController` | `/user/moyu/hot-topic` | 热榜数据获取和刷新 |
| `DailyContentController` | `/user/moyu/daily-content` | 每日内容（名言、提示、代码片段、历史） |
| `DeveloperCalendarController` | `/user/moyu/calendar` | 程序员日历事件和收藏 |
| `SalaryCalculatorController` | `/user/moyu/salary` | 薪资配置和工作记录 |
| `BugStoreController` | `/user/moyu/bug-store` | Bug 浏览和浏览历史 |

### 管理端 Controller

| Controller | 路由前缀 | 能力 |
| --- | --- | --- |
| `AdminDailyContentController` | `/admin/moyu/daily-content` | 每日内容 CRUD |
| `AdminDeveloperCalendarController` | `/admin/moyu/calendar` | 日历事件 CRUD |
| `AdminBugStoreController` | `/admin/moyu/bug-store` | Bug 条目 CRUD + 批量导入 |

### 各能力详情

| 能力 | 核心点 | Redis 使用 | 定时任务 |
| --- | --- | --- | --- |
| 热榜 | 缓存前缀 `moyu:hot-topic:` | 15 分钟刷新 | 有 |
| 薪资计算器 | 用户薪资配置、工作记录 | 无 | 无 |
| 程序员日历 | 日历事件、用户收藏 | 无 | 无 |
| 每日内容 | 编程名言、技术提示、代码片段、历史上的今天 | 无 | 无 |
| Bug 商店 | Bug 条目、分类、导入、浏览历史 | 无 | 无 |

### 用户端接口清单

#### 热榜 `/moyu/hot-topic`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/moyu/hot-topic/categories` | 获取热榜分类列表 |
| GET | `/moyu/hot-topic/data/{platform}` | 获取指定平台热榜数据 |
| GET | `/moyu/hot-topic/data/all` | 获取所有平台热榜数据 |
| POST | `/moyu/hot-topic/refresh` | 手动刷新热榜缓存 |

#### 每日内容 `/moyu/daily-content`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/moyu/daily-content/today` | 获取今日内容 |
| GET | `/moyu/daily-content/type/{contentType}` | 按类型获取内容 |
| GET | `/moyu/daily-content/random/{contentType}` | 随机获取指定类型内容 |
| GET | `/moyu/daily-content/recommend` | 获取推荐内容 |
| GET | `/moyu/daily-content/popular` | 获取热门内容 |
| GET | `/moyu/daily-content/language/{language}` | 按语言获取内容 |
| POST | `/moyu/daily-content/{contentId}/view` | 记录浏览 |
| POST | `/moyu/daily-content/{contentId}/like` | 点赞 |
| POST | `/moyu/daily-content/{contentId}/toggle-collection` | 切换收藏 |
| GET | `/moyu/daily-content/collections` | 获取收藏列表 |

#### 程序员日历 `/moyu/developer-calendar`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/moyu/developer-calendar/today` | 获取今日日历事件 |
| GET | `/moyu/developer-calendar/month/{year}/{month}` | 获取月度事件 |
| GET | `/moyu/developer-calendar/events/{date}` | 获取指定日期事件 |
| GET | `/moyu/developer-calendar/events/type/{eventType}` | 按类型获取事件 |
| GET | `/moyu/developer-calendar/events/major` | 获取重大事件 |
| GET | `/moyu/developer-calendar/preference` | 获取用户偏好 |
| POST | `/moyu/developer-calendar/preference` | 设置用户偏好 |
| POST | `/moyu/developer-calendar/events/{eventId}/toggle-collection` | 切换事件收藏 |
| GET | `/moyu/developer-calendar/collections/events` | 获取收藏事件列表 |

#### 薪资计算器 `/moyu/salary-calculator`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/moyu/salary-calculator/data` | 获取薪资数据 |
| GET | `/moyu/salary-calculator/config` | 获取薪资配置 |
| POST | `/moyu/salary-calculator/config` | 保存薪资配置 |
| DELETE | `/moyu/salary-calculator/config` | 删除薪资配置 |
| POST | `/moyu/salary-calculator/work-time` | 记录工作时间 |

#### Bug 商店 `/moyu/bug-store`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/moyu/bug-store/random` | 随机获取 Bug |

### 管理端接口清单

#### 每日内容管理 `/admin/moyu/daily-content`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/moyu/daily-content/list` | 内容列表 |
| GET | `/admin/moyu/daily-content/type/{contentType}` | 按类型查询 |
| GET | `/admin/moyu/daily-content/{id}` | 内容详情 |
| POST | `/admin/moyu/daily-content` | 创建内容 |
| PUT | `/admin/moyu/daily-content/{id}` | 更新内容 |
| DELETE | `/admin/moyu/daily-content/{id}` | 删除内容 |
| POST | `/admin/moyu/daily-content/batch-delete` | 批量删除 |
| POST | `/admin/moyu/daily-content/{id}/status` | 更新状态 |
| GET | `/admin/moyu/daily-content/statistics` | 统计数据 |
| GET | `/admin/moyu/daily-content/collections/statistics` | 收藏统计 |
| GET | `/admin/moyu/daily-content/popular-ranking` | 热门排行 |

#### 日历事件管理 `/admin/moyu/developer-calendar`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/moyu/developer-calendar/events` | 事件列表 |
| GET | `/admin/moyu/developer-calendar/events/type/{eventType}` | 按类型查询 |
| GET | `/admin/moyu/developer-calendar/events/{id}` | 事件详情 |
| POST | `/admin/moyu/developer-calendar/events` | 创建事件 |
| PUT | `/admin/moyu/developer-calendar/events/{id}` | 更新事件 |
| DELETE | `/admin/moyu/developer-calendar/events/{id}` | 删除事件 |
| POST | `/admin/moyu/developer-calendar/events/batch-delete` | 批量删除 |
| POST | `/admin/moyu/developer-calendar/events/{id}/status` | 更新状态 |
| GET | `/admin/moyu/developer-calendar/events/statistics` | 统计数据 |

#### Bug 商店管理 `/admin/moyu/bug-store`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/admin/moyu/bug-store/list` | Bug 列表 |
| GET | `/admin/moyu/bug-store/{id}` | Bug 详情 |
| POST | `/admin/moyu/bug-store` | 创建 Bug |
| PUT | `/admin/moyu/bug-store/{id}` | 更新 Bug |
| DELETE | `/admin/moyu/bug-store/{id}` | 删除 Bug |
| POST | `/admin/moyu/bug-store/batch-import` | 批量导入 |

### 缓存与外部数据源

| 工具 | 外部数据源 | 缓存 Key | 缓存 TTL | 刷新方式 |
|------|-----------|---------|---------|---------|
| 热榜 | 各平台热榜 API | `moyu:hot-topic:{platform}` | 15 分钟 | 定时任务 + 手动刷新 |
| 每日内容 | 数据库 | 无 | — | 实时查询 |
| 程序员日历 | 数据库 | 无 | — | 实时查询 |
| Bug 商店 | 数据库 | 无 | — | 实时查询 |
| 薪资计算器 | 数据库 | 无 | — | 实时查询 |

热榜是唯一使用外部 API 和 Redis 缓存的摸鱼工具。其他工具的数据都来自数据库，实时查询。

摸鱼工具虽然轻，但后端状态不少。排查时要看三类东西：

1. 数据库内容是否存在并启用。
2. Redis 缓存是否刷新。
3. 前端是否按日期、分类、平台或类型传了正确参数。

详细规则看 [摸鱼工具](/modules/moyu)。

## 版本历史

版本历史面向所有用户，是"产品变更说明"的正式入口。

| 字段/状态 | 说明 | 取值 |
| --- | --- | --- |
| 版本号 | 例如 `v2.2.0` | 字符串 |
| 标题 | 更新主标题 | 字符串 |
| 描述 | 简要说明 | 字符串 |
| 更新类型 | 标注变更级别 | `1` 重大、`2` 功能、`3` 修复、`4` 其他 |
| 状态 | 控制可见性 | `0` 草稿、`1` 发布、`2` 隐藏 |
| PRD 链接 | 可关联需求或说明文档 | URL |

写版本记录时建议遵循：

- 面向用户写结果，不只写技术实现。
- 对破坏性变更或迁移步骤单独标出。
- 修复类内容写清影响范围。
- 不要把内部敏感配置、Key、真实用户数据写进版本说明。

详细流程看 [版本历史](/modules/version-history)。
如果你要继续按真实提交补版本说明，不只写用户公告，也建议接着看 [按 Git Log 重写版本更新记录](/guide/git-log-release-notes)。
如果你已经确定这版要发什么，准备直接写 README 段、产品版本墙和团队交接，也可以直接套 [版本公告与发版交接模板](/guide/version-release-handoff-template)。

## 运营工作台视角

| 运营动作 | 去哪里做 | 注意事项 |
| --- | --- | --- |
| 维护每日内容 | `/moyu/daily-content` | 检查类型（名言/提示/代码/历史）、日期、启用状态 |
| 维护日历事件 | `/moyu/calendar-events` | 注意日期和收藏关系 |
| 维护 Bug 商店 | `/moyu/bug-store` | 导入后检查分类和技术标签 |
| 发布版本 | `/system/version` | 先草稿检查，再发布 |
| 查看工具问题 | 用户端工具页 | 多数问题在前端，无后台记录 |

## 验证清单

| 场景 | 预期 |
| --- | --- |
| JSON 格式化 | 输入合法 JSON 后格式化成功，错误 JSON 给出提示 |
| 文本比对 | 两段文本在前端本地显示差异 |
| 聚合翻译 | 能带着输入跳转到翻译页面 |
| 热榜刷新 | 后端刷新任务后 Redis 缓存更新 |
| 薪资开始/暂停/恢复/结束 | 工作记录状态按动作变化 |
| 每日内容收藏 | 收藏记录写入，类型和日历收藏区分清楚 |
| Bug 商店导入 | 批量导入后分类、标签、浏览详情正常 |
| 版本草稿 | 用户端不可见 |
| 版本发布 | 用户端 `/version-history` 可见 |
| 版本隐藏 | 用户端不展示，后台仍可管理 |

## 常见坑

| 问题 | 原因 | 处理 |
| --- | --- | --- |
| 以为开发者工具有后端记录 | 当前主要是前端本地实现 | 看 `vue3-user-front/src/views/dev-tools` |
| 热榜数据不变 | 缓存未过期或定时任务未刷新 | 查 `moyu:hot-topic:` key |
| 薪资状态错乱 | 同一天重复开始/结束或暂停状态未恢复 | 查 `work_record` 的日期和状态 |
| 每日内容收藏混乱 | 内容收藏和日历收藏共用收藏表 | 区分 collection type |
| 版本发布后用户看不到 | 状态不是发布或发布时间/路由缓存问题 | 查版本状态和用户端接口 |
| 版本文案太技术化 | 只写 commit 级变更 | 改成用户可理解的收益和影响 |

## 文档维护提醒

新增轻工具时，先判断它属于哪类：

1. 纯前端工具：更新 [开发者工具](/modules/dev-tools)。
2. 摸鱼运营内容：更新 [摸鱼工具](/modules/moyu) 和数据表索引。
3. 产品更新公告：更新 [版本历史](/modules/version-history)。

如果新工具开始保存用户数据，就不能再按"纯前端工具"处理，必须补接口、表结构、隐私说明和异常排查。
