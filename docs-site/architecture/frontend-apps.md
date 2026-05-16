# 前端应用

Code Nest 有两个 Vue 3 + Vite 前端项目，并且都保留 Electron 打包能力。

## 管理端

目录：

```text
vue3-admin-front
```

常用命令：

```bash
cd vue3-admin-front
npm install
npm run dev
```

管理端主要面向运营和管理员。核心页面包括：

| 路由域 | 能力 |
| --- | --- |
| `/user` | 用户管理 |
| `/interview` | 面试题分类、题单、题目 |
| `/mock-interview` | AI 模拟面试会话和方向配置 |
| `/oj` | OJ 题目、赛事、标签 |
| `/resume` | 简历模板、报告和分析 |
| `/knowledge` | 知识图谱管理 |
| `/learning-assets` | 学习资产审核与统计 |
| `/community` | 社区分类、标签、帖子、评论、用户 |
| `/moments` | 动态、评论和统计 |
| `/chat` | 聊天消息和在线用户 |
| `/logs` | 登录日志和操作日志 |
| `/notification` | 通知管理 |
| `/sensitive` | 敏感词、白名单、策略、统计和配置 |
| `/filestorage` | 存储配置、文件管理、迁移和系统设置 |
| `/system` | AI 配置、版本管理、AI 治理 |
| `/codepen` | 代码作品、模板、标签和统计 |
| `/moyu` | 日历、内容、统计和 Bug 商店 |
| `/points` | 积分总览、用户、明细和发放 |
| `/blog` | 博客文章、分类和标签 |
| `/lottery` | 抽奖活动管理 |

## 用户端

目录：

```text
vue3-user-front
```

常用命令：

```bash
cd vue3-user-front
npm install
npm run dev2
```

用户端主要面向开发者成长场景。核心页面包括：

| 路由域 | 能力 |
| --- | --- |
| `/interview` | 面试题库、随机刷题、收藏、复习 |
| `/oj` | 题目列表、题目详情、提交、赛事、排行、Playground |
| `/mock-interview` | AI 模拟面试配置、会话、报告和历史 |
| `/job-battle` | 求职作战台 |
| `/job-match-engine` | 岗位匹配引擎 |
| `/career-loop` | 求职闭环 |
| `/learning-cockpit` | 学习成长驾驶舱 |
| `/learning-assets` | 学习资产沉淀 |
| `/sql-optimizer/workbench` | SQL 优化工作台 |
| `/resume` | 我的简历、模板中心、编辑器 |
| `/community` | 社区帖子、收藏、个人帖子、发帖和用户主页 |
| `/moments` | 动态广场、用户主页和收藏 |
| `/chat` | 聊天室 |
| `/points` | 积分账户 |
| `/plan` | 计划打卡 |
| `/team` | 学习小组 |
| `/lottery` | 抽奖 |
| `/knowledge` | 知识图谱 |
| `/version-history` | 版本历史 |
| `/dev-tools` | JSON、文本对比、翻译等工具 |
| `/moyu-tools` | 热榜、薪资计算、程序员日历、每日内容、Bug 商店 |
| `/blog` | 个人博客、文章编辑、文章详情 |
| `/codepen` | 代码广场、编辑器、我的作品、详情 |
| `/flashcard` | 闪卡卡组、学习、我的卡组和编辑 |

## 前端开发约定

- API 封装优先放在 `src/api/`。
- 路由变更先看 `src/router/index.js`。
- 请求复用 `src/utils/request.js`。
- 管理端和用户端 Token key 不相同，避免跨端登录态混用。
- 新页面完成后，同步补充到文档站的模块页和截图手册。

## 检查清单

新增或调整前端页面时，建议检查：

1. 路由是否在对应项目的 `src/router/index.js` 中注册，并配置正确的认证要求。
2. API 封装是否放在 `src/api/`，没有把请求散落在页面组件里。
3. 请求拦截器是否正确携带用户端或管理端 Token。
4. 页面入口是否同步到 [前端路由索引](/reference/frontend-routes)。
5. 如果页面可被用户或管理员操作，是否补到对应模块页和操作手册。
