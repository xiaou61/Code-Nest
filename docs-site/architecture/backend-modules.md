# 后端模块

后端采用 Maven 多模块结构，根 `pom.xml` 的 `<modules>` 声明所有子模块。业务逻辑应优先落在对应 `xiaou-*` 模块，不建议直接塞进 `xiaou-application`。

## 核心模块表

| 模块 | 职责 | 文档入口 |
| --- | --- | --- |
| `xiaou-application` | 聚合启动、全局配置、API 出口 | [架构总览](/architecture/overview) |
| `xiaou-common` | 公共配置、返回体、异常、工具类 | [鉴权与用户体系](/modules/auth) |
| `xiaou-system` | 管理员、系统配置、日志、AI 配置后台 | [系统运营后台](/modules/system-ops) |
| `xiaou-user` | 用户注册、登录、资料和用户态能力 | [鉴权与用户体系](/modules/auth) |
| `xiaou-user-api` | 用户跨模块 API 契约 | [鉴权与用户体系](/modules/auth) |
| `xiaou-ai` | AI Runtime、Prompt、Graph、RAG、回归 | [AI Runtime](/modules/ai-runtime) |
| `xiaou-interview` | 面试题库、题单、收藏、掌握度 | [题库与成长闭环](/modules/interview-and-growth) |
| `xiaou-mock-interview` | AI 模拟面试、求职作战台、成长闭环 | [题库与成长闭环](/modules/interview-and-growth) |
| `xiaou-oj` | 题目、提交、判题、题解、赛事 | [OJ 判题系统](/modules/oj) |
| `xiaou-community` | 社区帖子、评论、分类、标签 | [社区与内容矩阵](/modules/community-content) |
| `xiaou-moment` | 动态广场、互动、推荐和审核 | [社区与内容矩阵](/modules/community-content) |
| `xiaou-blog` | 博客文章、分类、标签、评论 | [社区与内容矩阵](/modules/community-content) |
| `xiaou-codepen` | 代码工坊、在线作品、模板和标签 | [社区与内容矩阵](/modules/community-content) |
| `xiaou-learning-asset` | 内容转学习资产、审核和发布记录 | [题库与成长闭环](/modules/interview-and-growth) |
| `xiaou-chat` | WebSocket 聊天室、禁言、撤回、广播 | [IM 聊天室](/modules/chat) |
| `xiaou-notification` | 系统通知、私信和消息状态 | [系统运营后台](/modules/system-ops) |
| `xiaou-resume` | 简历模板、编辑、分析和导出 | [简历系统](/modules/resume) |
| `xiaou-filestorage` | 文件上传、存储配置、迁移和访问控制 | [文件存储](/modules/file-storage) |
| `xiaou-sensitive` | 敏感词词库、策略、统计和检测实现 | [系统运营后台](/modules/system-ops) |
| `xiaou-sensitive-api` | 敏感词跨模块 API 契约 | [系统运营后台](/modules/system-ops) |
| `xiaou-knowledge` | 知识图谱节点、关系和图谱渲染数据 | [题库与成长闭环](/modules/interview-and-growth) |
| `xiaou-version` | 版本历史和发布墙 | [工具、摸鱼与版本](/modules/tools-moyu-version) |
| `xiaou-moyu` | 摸鱼工具、日历、热榜、Bug 商店 | [工具、摸鱼与版本](/modules/tools-moyu-version) |
| `xiaou-points` | 积分账户、规则、明细、抽奖 | [积分与抽奖](/modules/points) |
| `xiaou-plan` | 个人计划、打卡、连续统计 | [题库与成长闭环](/modules/interview-and-growth) |
| `xiaou-team` | 学习小组、讨论、任务和打卡 | [题库与成长闭环](/modules/interview-and-growth) |
| `xiaou-flashcard` | 闪卡卡组、卡片、学习记录 | [题库与成长闭环](/modules/interview-and-growth) |
| `xiaou-sql-optimizer` | 慢 SQL 分析与优化记录 | [AI Runtime](/modules/ai-runtime) |

## 新增后端功能的落点

1. 先判断业务归属模块。
2. Controller、Service、Mapper 放在业务模块。
3. 公共返回体和异常复用 `xiaou-common`。
4. 跨模块调用优先抽象到 API 契约模块。
5. 涉及数据结构时，新增版本增量 SQL 到 `sql/vX.Y.Z/`。
6. 最后同步文档站对应模块页。

