# 摸鱼工具

摸鱼工具模块提供热榜、薪资计算器、程序员日历、每日内容和 Bug 商店等轻工具能力。

## 功能入口

| 端 | 页面 |
| --- | --- |
| 用户端 | `/moyu-tools`、`/moyu-tools/hot-topics`、`/moyu-tools/salary-calculator`、`/moyu-tools/calendar`、`/moyu-tools/daily-content`、`/moyu-tools/bug-store` |
| 管理端 | `/moyu/calendar-events`、`/moyu/daily-content`、`/moyu/statistics`、`/moyu/bug-store` |
| 后端模块 | `xiaou-moyu` |

## 用户侧接口域

| 接口域 | 能力 |
| --- | --- |
| `/moyu/hot-topic` | 平台分类、单平台热榜、全平台热榜、刷新 |
| `/moyu/salary-calculator` | 薪资数据、配置、工时 |
| `/moyu/developer-calendar` | 程序员日历 |
| `/moyu/daily-content` | 今日内容、按类型、随机、推荐、热门、语言、浏览、点赞、收藏 |
| `/moyu/bug-store` | 随机 Bug 商品 |

## 管理侧能力

- 日历事件管理。
- 每日内容管理、状态、批量删除、统计、收藏统计、热门排行。
- Bug 商店列表、详情、新增、更新、删除、批量导入。
- 摸鱼统计分析。

## 开发注意

- 热榜数据可能依赖外部来源，要有缓存和降级。
- 每日内容支持多类型和多语言时，需要统一枚举。
- Bug 商店偏运营内容，批量导入要校验格式。

