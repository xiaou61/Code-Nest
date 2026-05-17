# 源码地图

本页给新维护者一个“先看哪里”的地图。Code Nest 是 Maven 多模块后端 + 两个 Vue 3 前端 + 独立文档站。

## 根目录

| 路径 | 说明 |
| --- | --- |
| `pom.xml` | Maven 父工程，版本号为 `v2.2.0`，统一 Java 17、Spring Boot 3.4.4、LangChain4j、LangGraph4j 等依赖版本 |
| `xiaou-application` | Spring Boot 启动应用和少量聚合 Web 入口 |
| `xiaou-common` | 统一返回体、分页、异常、通知公共能力、权限切面、工具类 |
| `vue3-user-front` | 用户端 Vue 3 + Vite 应用 |
| `vue3-admin-front` | 管理端 Vue 3 + Vite 应用 |
| `docs` | 原始 PRD、计划、截图、面试亮点、归档材料 |
| `docs-site` | v2.2.0 新增 VitePress 独立文档站 |
| `sql` | 主库基线脚本和版本增量脚本 |

## 后端模块

| 模块 | 职责 |
| --- | --- |
| `xiaou-system` | 管理端认证、仪表盘、日志、AI 配置入口 |
| `xiaou-user-api` | 用户服务对外 API 契约 |
| `xiaou-user` | 用户注册登录、资料、头像、密码、管理端用户管理 |
| `xiaou-interview` | 面试分类、题单、题目、收藏、学习记录、掌握度 |
| `xiaou-community` | 社区分类、标签、帖子、评论、用户状态、AI 摘要 |
| `xiaou-filestorage` | 文件上传下载、存储配置、访问记录、迁移、系统设置 |
| `xiaou-notification` | 通知、模板、配置、已读状态 |
| `xiaou-moment` | 动态广场、点赞、评论、收藏、浏览 |
| `xiaou-sensitive-api` | 敏感词检查对外 API |
| `xiaou-sensitive` | 敏感词词库、策略、白名单、来源、统计、版本 |
| `xiaou-knowledge` | 知识图谱和节点管理 |
| `xiaou-version` | 版本历史展示和后台维护 |
| `xiaou-moyu` | 热榜、薪资计算器、程序员日历、每日内容、Bug 商店 |
| `xiaou-points` | 积分账户、明细、签到、抽奖、库存和风控 |
| `xiaou-chat` | IM 聊天 REST、WebSocket、在线用户、禁言、公告 |
| `xiaou-blog` | 个人博客配置、文章、分类、标签 |
| `xiaou-codepen` | 代码工坊作品、收藏、点赞、评论、标签、收益流水 |
| `xiaou-resume` | 简历、模板、导出、分享、分析 |
| `xiaou-plan` | 计划打卡、成长自动驾驶、驾驶舱排行 |
| `xiaou-mock-interview` | AI 模拟面试、求职作战台、岗位匹配、求职闭环 |
| `xiaou-team` | 学习小组、成员、申请、任务、打卡、讨论、排行榜 |
| `xiaou-flashcard` | 闪卡卡组、卡片、学习记录、每日统计 |
| `xiaou-ai` | Prompt、RAG、Schema、Graph、AI 回归、运行指标 |
| `xiaou-learning-asset` | 学习资产转化、候选资产、审核、发布 |
| `xiaou-sql-optimizer` | SQL 分析、改写、对比、历史记录 |
| `xiaou-oj` | OJ 题目、标签、测试用例、提交、题解、赛事、评论 |

## 后端包结构

| 层 | 常见路径 | 说明 |
| --- | --- | --- |
| Controller | `src/main/java/**/controller` | HTTP 路由入口，优先从这里找 API |
| Service | `src/main/java/**/service` | 业务编排和事务边界 |
| ServiceImpl | `src/main/java/**/service/impl` | 具体业务规则 |
| Mapper | `src/main/java/**/mapper` | MyBatis Mapper 接口 |
| XML Mapper | `src/main/resources/mapper` 或模块内 Java 目录 | SQL 查询和结果映射 |
| Domain/Entity | `src/main/java/**/domain` | 数据表实体或领域对象 |
| DTO | `src/main/java/**/dto` | 请求、响应、AI 结构化结果 |
| Config | `src/main/java/**/config` | 模块配置、WebSocket、线程池、第三方客户端 |

## 前端源码

| 路径 | 说明 |
| --- | --- |
| `vue3-user-front/src/router/index.js` | 用户端路由和登录守卫 |
| `vue3-user-front/src/views` | 用户端页面，按功能目录拆分 |
| `vue3-user-front/src/api` | 用户端接口封装 |
| `vue3-user-front/src/stores` | Pinia 状态 |
| `vue3-admin-front/src/router/index.js` | 管理端路由和登录守卫 |
| `vue3-admin-front/src/views` | 管理端页面 |
| `vue3-admin-front/src/api` | 管理端接口封装 |
| `vue3-admin-front/src/layout` | 管理后台布局、菜单、顶栏 |
| `vue3-user-front/src/utils/markdown.js` | 用户端 Markdown 渲染、DOMPurify 净化和代码高亮 |
| `vue3-admin-front/src/utils/markdown.js` | 管理端 Markdown 渲染、DOMPurify 净化和代码高亮 |

## 文档与素材

| 路径 | 说明 |
| --- | --- |
| `docs/PRD` | 历史功能 PRD，写模块背景时优先参考 |
| `docs/plans` | 版本演进和专题方案 |
| `docs/manual-assets/2026-04-24/user` | 用户端全功能截图素材 |
| `docs/manual-assets/2026-04-24/admin` | 管理端全功能截图素材 |
| `docs/面试亮点拆解` | 面试讲解版技术亮点 |
| `docs-site/modules` | 已结构化的功能文档 |
| `docs-site/reference` | API、路由、数据表、源码地图等索引 |

## 定位一个功能的顺序

1. 先在 [模块总览](/modules/) 找功能归属。
2. 到 [前端路由索引](/reference/frontend-routes) 找页面。
3. 到 [API 路由索引](/reference/api-routes) 找 Controller。
4. 到本页找后端模块。
5. 到 [数据表索引](/reference/database-tables) 找表和 Mapper。
6. 最后回到 `docs/PRD` 或 `docs/plans` 查背景材料。
