# 模块依赖地图

这页用于做影响面评估：当你准备改某个模块时，先看它依赖哪些平台能力、会影响哪些前端入口、需要同步哪些文档和验证项。

如果你只是想知道功能在哪里，先看 [模块总览](/modules/)；如果你要判断改动会牵连谁，就看本页。

## 读图方法

Code Nest 的依赖关系可以按四层理解：

```text
前端入口
  -> 业务模块
  -> 平台能力
  -> 运维、验证、文档和发布
```

阅读一个模块时建议顺序是：

1. 先定位用户端或管理端入口。
2. 找到对应后端业务模块。
3. 判断它依赖的公共能力。
4. 检查是否涉及外部服务或高风险边界。
5. 选择验证项并同步文档。

## 基础平台能力

这些能力是多个业务模块的共同依赖。改它们时要把影响面放大看。

| 能力 | 后端模块 | 被影响的常见业务 | 改动风险 |
| --- | --- | --- | --- |
| 鉴权与当前用户 | `xiaou-common`、`xiaou-user`、`xiaou-system` | 几乎所有用户端和管理端接口 | 登录失效、越权、管理端权限混乱 |
| 文件存储 | `xiaou-filestorage` | 头像、聊天图片、简历、内容附件、导出文件 | URL 前缀、公开读取、存储配置、迁移 |
| 通知中心 | `xiaou-notification` | 注册、审核、互动、公告、系统消息 | 接收人错误、已读状态不同步、消息丢失 |
| 积分抽奖 | `xiaou-points` | 签到、OJ 首次 AC、后台发放、抽奖消费 | 重复发放、扣减不幂等、余额流水不一致 |
| 敏感词风控 | `xiaou-sensitive`、`xiaou-sensitive-api` | 社区、动态、博客、评论、内容发布 | 漏拦截、误杀、跨模块检测不一致 |
| AI Runtime | `xiaou-ai`、`xiaou-system` | 模拟面试、求职作战台、SQL 优化、社区摘要 | Prompt 回归、Schema 解析、RAG 依赖、成本和失败率 |
| WebSocket | `xiaou-chat` | 聊天室、实时消息和在线态 | ticket、Origin、限流、ACK、断线恢复 |
| 日志审计 | `xiaou-system`、`xiaou-common` | 管理端高风险操作、登录和运营记录 | 排障证据不足、操作不可追溯 |

## 成长学习依赖地图

| 业务模块 | 用户入口 | 管理入口 | 主要依赖 | 重点验证 |
| --- | --- | --- | --- | --- |
| 面试题库 | `/interview/*` | `/interview/*` | 鉴权、用户学习记录、收藏、掌握度 | 题单、题目学习、掌握度、复习回流 |
| OJ 判题 | `/oj/*` | `/oj/*` | 鉴权、go-judge、积分、评论、排行 | 运行、提交、结果回写、首次 AC、赛事 |
| 模拟面试 | `/mock-interview/*` | `/mock-interview/*` | AI Runtime、用户、学习资产、通知 | 会话、追问、评分、报告、资产转化 |
| 求职作战台 | `/job-battle`、`/job-match-engine`、`/career-loop` | `/system/ai-config`、`/system/ai-governance` | AI Runtime、RAG、用户档案、指标 | JD 解析、匹配、计划、复盘和治理指标 |
| 学习资产 | `/learning-assets` | `/learning-assets/review` | AI 总结、审核、通知、用户发布记录 | 候选生成、发布、审核、驳回、合并 |
| SQL 优化 | `/sql-optimizer/workbench` | `/system/ai-config` | AI Runtime、Schema、历史记录 | SQL 分析、改写、对比、失败态 |
| 计划打卡 | `/plan` | 后续可扩展 | 用户、提醒、统计 | 创建计划、每日打卡、连续统计 |
| 学习小组 | `/team/*` | 后续可扩展 | 用户、成员、申请、任务、讨论 | 加入小组、发布讨论、任务打卡 |
| 闪卡 | `/flashcard/*` | 后续可扩展 | 用户、卡组、学习记录 | 创建卡组、编辑卡片、学习完成 |
| 知识图谱 | `/knowledge/*` | `/knowledge/*` | 图谱节点、关系、公开读取 | 图谱列表、节点展示、后台编辑 |

改成长学习模块时，最容易漏的是用户学习状态、统计回写和 AI/OJ 外部依赖。不要只验证页面打开。

## 内容与互动依赖地图

| 业务模块 | 用户入口 | 管理入口 | 主要依赖 | 重点验证 |
| --- | --- | --- | --- | --- |
| 社区帖子 | `/community/*` | `/community/*` | 鉴权、敏感词、通知、AI 摘要、渲染安全 | 发帖、评论、点赞、收藏、后台审核 |
| 动态广场 | `/moments/*` | `/moments/*` | 鉴权、文件、敏感词、通知、统计 | 发布、图片、评论、收藏、统计 |
| 博客 | `/blog/*` | `/blog/*` | Markdown、文件、敏感词、分类标签 | 写文章、公开展示、后台管理 |
| CodePen | `/codepen/*` | `/codepen/*` | 代码展示、收藏、点赞、评论、统计 | 发布作品、详情展示、后台统计 |
| 简历系统 | `/resume/*` | `/resume/*` | 文件导出、模板、分析报告 | 保存、导出、模板、分析记录 |
| IM 聊天室 | `/chat` | `/chat/*` | WebSocket、文件、敏感词、限流、公告 | ticket、发送、ACK、撤回、禁言、踢出 |
| 通知中心 | `/notification` | `/notification` | 业务事件、公告、已读状态 | 消息生成、已读、管理端公告 |

内容类模块通常会经过敏感词、文件、通知和前端渲染安全。只改其中一个页面，也要检查后端写入、管理端可见和刷新后回流。

## 工具与运营依赖地图

| 模块 | 用户入口 | 管理入口 | 主要依赖 | 重点验证 |
| --- | --- | --- | --- | --- |
| 积分与抽奖 | `/points`、`/lottery` | `/points/*`、`/lottery` | 用户、积分规则、库存、限制规则 | 签到、发放、流水、抽奖扣减 |
| 文件存储 | 上传组件、公开文件 URL | `/filestorage/*` | 存储策略、系统设置、访问控制 | 上传、读取、公开权限、URL 前缀 |
| 敏感词风控 | 内容发布链路 | `/sensitive/*` | 词库、白名单、策略、版本 | 检测、替换、策略切换、统计 |
| 版本历史 | `/version-history` | `/system/version` | 版本记录、发布状态 | 版本展示、后台维护 |
| 开发者工具 | `/dev-tools/*` | 无 | 前端本地能力、渲染安全 | JSON、Diff、翻译页面 |
| 摸鱼工具 | `/moyu-tools/*` | `/moyu/*` | 外部热榜、日历、薪资配置、Bug 数据 | 热榜兜底、配置保存、后台统计 |
| 仪表盘与日志 | 无 | `/dashboard`、`/logs/*` | 多业务聚合、登录日志、操作日志 | 指标展示、日志查询、排障证据 |

工具类页面看起来轻，但有些会依赖外部服务、后台配置或 HTML 片段渲染。改动时按 [权限与安全边界](/guide/security-boundaries) 过一遍。

## 前端到后端映射

| 前端目录 | 主要服务对象 | 常见后端依赖 |
| --- | --- | --- |
| `vue3-user-front/src/views/interview` | 面试题学习 | `xiaou-interview`、用户、收藏、掌握度 |
| `vue3-user-front/src/views/oj` | OJ 刷题和赛事 | `xiaou-oj`、go-judge、积分 |
| `vue3-user-front/src/views/mock-interview` | AI 面试 | `xiaou-mock-interview`、`xiaou-ai`、学习资产 |
| `vue3-user-front/src/views/community` | 社区 | `xiaou-community`、敏感词、通知、AI 摘要 |
| `vue3-user-front/src/views/chat` | 聊天室 | `xiaou-chat`、WebSocket、文件 |
| `vue3-admin-front/src/views/system` | 系统和 AI 配置 | `xiaou-system`、`xiaou-ai`、版本历史 |
| `vue3-admin-front/src/views/filestorage` | 文件运营 | `xiaou-filestorage`、系统设置 |
| `vue3-admin-front/src/views/sensitive` | 敏感词运营 | `xiaou-sensitive` |
| `vue3-admin-front/src/views/points` | 积分运营 | `xiaou-points` |

更细的页面和路由映射见 [前端路由索引](/reference/frontend-routes)。

## 高风险依赖

这些依赖一旦变更，建议默认升级验证范围。

| 依赖 | 涉及模块 | 最低验证 |
| --- | --- | --- |
| Sa-Token 登录域 | 用户端、管理端、WebSocket ticket、当前用户 | 登录、接口 401、管理端权限、当前用户读写 |
| 文件 URL 和公开读取 | 头像、内容附件、聊天图片、简历导出 | 上传、读取、公开/非公开、Nginx 或 `/api/files` |
| DOMPurify 和 Markdown 渲染 | 博客、社区、题库、通知、工具、RAG 命中片段 | 构建、真实页面渲染、XSS 样例不执行 |
| AI Prompt 和 Schema | 模拟面试、求职作战台、SQL 优化、社区摘要 | AI 回归、结构化字段、失败态 |
| go-judge | OJ 运行、提交、赛事 | AC/WA/CE 样例、结果回写、提交详情 |
| WebSocket ticket | 聊天连接、实时消息 | ticket 获取、连接、ACK、限流、断线 |
| 积分流水 | 签到、OJ、抽奖、后台发放 | 余额、流水、重复提交和库存 |

## 改动类型到验证范围

| 改动类型 | 先查 | 最低验证 |
| --- | --- | --- |
| 新增用户端页面 | 前端路由索引、模块页、API 路由 | 用户端构建、真实路由烟测 |
| 新增管理端页面 | 管理端路由、权限、系统运营后台 | 管理端构建、登录和权限烟测 |
| 新增后端接口 | API 路由索引、权限边界、响应错误码 | 后端构建、接口或页面烟测 |
| 新增表字段 | 数据表索引、Mapper、版本 SQL | 后端构建、相关读写流程 |
| 修改平台能力 | 本页基础平台能力表 | 相关业务链路回归，不只跑单模块 |
| 修改 AI/OJ/聊天 | 对应模块页和测试与回归 | 外部依赖、失败态和回归样例 |
| 修改部署配置 | Docker 与服务部署、问题定位流程 | 构建、启动、代理、真实路径访问 |

## 文档同步规则

做完影响面判断后，按下面方式同步文档：

| 发现的影响 | 要同步 |
| --- | --- |
| 新页面或路由 | [前端路由索引](/reference/frontend-routes)、对应模块页、操作手册 |
| 新接口前缀 | [API 路由索引](/reference/api-routes)、对应模块页 |
| 新表或字段 | [数据表索引](/reference/database-tables)、对应模块页 |
| 新跨模块依赖 | 本页、[模块总览](/modules/)、对应模块页 |
| 新高风险验证 | [测试与回归](/guide/testing-regression)、[发布前验证](/guide/release-verification)、[验证记录与已知问题](/manuals/verified-scenarios) |

## 快速影响面记录模板

```text
改动模块：
用户端入口：
管理端入口：
后端模块：
依赖的平台能力：
外部依赖：
高风险边界：
需要同步的文档：
最低验证：
未验证项及原因：
```

