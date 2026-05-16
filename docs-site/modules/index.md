# 模块总览

Code Nest 的功能可以按读者视角分成五组：成长学习、内容社区、AI 能力、运营后台、基础设施。

## 成长学习

| 功能 | 用户端 | 管理端 | 后端模块 |
| --- | --- | --- | --- |
| 面试题库 | 题单、刷题、收藏、复习 | 分类、题单、题目管理 | `xiaou-interview` |
| OJ | 题目、提交、赛事、排行 | 题目、赛事、标签管理 | `xiaou-oj` |
| AI 模拟面试 | 配置、会话、报告、历史 | 会话和方向管理 | `xiaou-mock-interview`、`xiaou-ai` |
| 求职闭环 | 求职作战台、岗位匹配、成长驾驶舱 | AI 治理与配置 | `xiaou-mock-interview`、`xiaou-ai` |
| 闪卡 | 卡组、学习、编辑 | 后续可扩展审核 | `xiaou-flashcard` |
| 计划打卡 | 计划、每日打卡 | 后续可扩展统计 | `xiaou-plan` |
| 学习小组 | 小组、任务、讨论 | 后续可扩展运营 | `xiaou-team` |
| 知识图谱 | 图谱查看 | 图谱管理 | `xiaou-knowledge` |

## 内容社区

| 功能 | 用户端 | 管理端 | 后端模块 |
| --- | --- | --- | --- |
| 社区 | 发帖、评论、收藏、个人主页 | 分类、标签、帖子、评论、用户 | `xiaou-community` |
| 动态广场 | 动态、互动、收藏 | 动态、评论、统计 | `xiaou-moment` |
| 博客 | 个人博客、编辑器、文章详情 | 文章、分类、标签 | `xiaou-blog` |
| 代码工坊 | 代码广场、编辑器、作品详情 | 作品、模板、标签、统计 | `xiaou-codepen` |
| 学习资产 | 内容沉淀、发布记录 | 审核、统计 | `xiaou-learning-asset` |

## 平台能力

| 功能 | 说明 | 后端模块 |
| --- | --- | --- |
| 双端鉴权 | 管理端和用户端隔离登录域 | `xiaou-common`、`xiaou-system`、`xiaou-user` |
| AI Runtime | 模型、Prompt、Graph、RAG、Schema、回归 | `xiaou-ai` |
| 文件存储 | 上传、访问控制、存储配置、迁移 | `xiaou-filestorage` |
| 通知 | 系统通知、私信和消息状态 | `xiaou-notification` |
| IM 聊天室 | WebSocket、心跳、撤回、禁言、踢人 | `xiaou-chat` |
| 积分抽奖 | 积分账户、规则、明细、抽奖 | `xiaou-points` |
| 敏感词风控 | 词库、白名单、策略、统计 | `xiaou-sensitive` |
| 版本与摸鱼工具 | 版本墙、程序员日历、热榜、薪资计算、Bug 商店 | `xiaou-version`、`xiaou-moyu` |

## 已拆分文档页

| 分组 | 文档页 |
| --- | --- |
| 账号与安全 | [鉴权与用户体系](/modules/auth)、[用户账户与个人中心](/modules/user-account)、[敏感词风控](/modules/sensitive) |
| 学习成长 | [面试题库](/modules/interview)、[模拟面试与求职作战台](/modules/mock-interview-job-battle)、[学习资产](/modules/learning-assets)、[闪卡](/modules/flashcard)、[计划与学习小组](/modules/plan-team)、[知识图谱](/modules/knowledge)、[SQL 优化工作台](/modules/sql-optimizer) |
| 内容社区 | [社区帖子](/modules/community)、[动态广场](/modules/moments)、[博客](/modules/blog)、[代码工坊](/modules/codepen) |
| 平台运营 | [IM 聊天室](/modules/chat)、[简历系统](/modules/resume)、[积分与抽奖](/modules/points)、[文件存储](/modules/file-storage)、[通知中心](/modules/notification)、[系统运营后台](/modules/system-ops)、[仪表盘与日志](/modules/dashboard-logs) |
| 轻工具与版本 | [开发者工具](/modules/dev-tools)、[摸鱼工具](/modules/moyu)、[版本历史](/modules/version-history) |

## 文档补齐策略

每个模块后续都按同一模板补齐：

1. 功能定位。
2. 用户端入口。
3. 管理端入口。
4. 后端源码位置。
5. 数据表和关键字段。
6. 核心流程。
7. 权限和安全边界。
8. 常见问题。
9. 验证方式。
