# 前端路由索引

本页来自 `vue3-user-front/src/router/index.js` 和 `vue3-admin-front/src/router/index.js`。它用于把用户可见页面、管理后台页面和对应模块文档串起来。

## 用户端路由

| 路由 | 页面 | 认证 | 功能文档 |
| --- | --- | --- | --- |
| `/` | 首页 | 需要 | [模块总览](/modules/) |
| `/login`、`/register` | 登录注册 | 不需要 | [鉴权与用户体系](/modules/auth) |
| `/interview`、`/interview/random` | 题库首页、随机抽题 | 需要 | [面试题库](/modules/interview) |
| `/interview/question-sets/:id`、`/interview/questions/:setId/:questionId` | 题单详情、题目学习 | 需要 | [面试题库](/modules/interview) |
| `/interview/favorites`、`/interview/review` | 我的收藏、智能复习 | 需要 | [题库与成长闭环](/modules/interview-and-growth) |
| `/oj`、`/oj/problem/:id` | OJ 列表和题目详情 | 需要 | [OJ 判题系统](/modules/oj) |
| `/oj/contests`、`/oj/contests/:id` | 赛事中心和赛事详情 | 需要 | [OJ 判题系统](/modules/oj) |
| `/oj/submission/:id`、`/oj/my-submissions` | 提交详情、我的提交 | 需要 | [OJ 判题系统](/modules/oj) |
| `/oj/statistics`、`/oj/playground`、`/oj/ranking` | 做题统计、练习场、排行榜 | 需要 | [OJ 判题系统](/modules/oj) |
| `/mock-interview`、`/mock-interview/config` | AI 模拟面试入口和配置 | 需要 | [模拟面试与求职作战台](/modules/mock-interview-job-battle) |
| `/mock-interview/session`、`/mock-interview/report`、`/mock-interview/history` | 会话、报告、历史 | 需要 | [模拟面试与求职作战台](/modules/mock-interview-job-battle) |
| `/job-battle`、`/job-match-engine` | 求职作战台、岗位匹配引擎 | 需要 | [模拟面试与求职作战台](/modules/mock-interview-job-battle) |
| `/career-loop` | 求职闭环中台 | 需要 | [模拟面试与求职作战台](/modules/mock-interview-job-battle) |
| `/learning-cockpit`、`/growth-autopilot` | 学习成长驾驶舱、自动驾驶重定向 | 需要 | [题库与成长闭环](/modules/interview-and-growth) |
| `/learning-assets` | 我的学习资产 | 需要 | [学习资产](/modules/learning-assets) |
| `/sql-optimizer/workbench` | SQL 优化工作台 | 需要 | [SQL 优化工作台](/modules/sql-optimizer) |
| `/resume`、`/resume/templates` | 简历管理、模板中心 | 需要 | [简历系统](/modules/resume) |
| `/resume/editor`、`/resume/editor/:id` | 创建和编辑简历 | 需要 | [简历系统](/modules/resume) |
| `/community`、`/community/posts/:id` | 社区首页、帖子详情 | 需要 | [社区帖子](/modules/community) |
| `/community/collections`、`/community/my-posts`、`/community/create`、`/community/users/:userId` | 收藏、我的帖子、发帖、用户主页 | 需要 | [社区帖子](/modules/community) |
| `/notification` | 通知中心 | 需要 | [通知中心](/modules/notification) |
| `/moments`、`/moments/user/:userId`、`/moments/my-favorites` | 动态、主页、收藏 | 需要 | [动态广场](/modules/moments) |
| `/chat` | IM 聊天室 | 需要 | [IM 聊天室](/modules/chat) |
| `/profile` | 个人中心 | 需要 | [用户账户与个人中心](/modules/user-account) |
| `/points`、`/lottery` | 我的积分、幸运抽奖 | 需要 | [积分与抽奖](/modules/points) |
| `/plan` | 计划打卡 | 需要 | [计划与学习小组](/modules/plan-team) |
| `/team`、`/team/create`、`/team/my`、`/team/:id`、`/team/:id/edit` | 小组广场、创建、我的、小组详情、编辑 | 需要 | [计划与学习小组](/modules/plan-team) |
| `/knowledge`、`/knowledge/maps/:id` | 图谱广场、图谱学习 | 需要 | [知识图谱](/modules/knowledge) |
| `/version-history` | 版本更新历史 | 不需要 | [版本历史](/modules/version-history) |
| `/dev-tools`、`/dev-tools/json`、`/dev-tools/text-diff`、`/dev-tools/translation` | 程序员工具、JSON、文本比对、翻译 | 不需要 | [开发者工具](/modules/dev-tools) |
| `/moyu-tools`、`/moyu-tools/hot-topics`、`/moyu-tools/salary-calculator`、`/moyu-tools/calendar`、`/moyu-tools/daily-content`、`/moyu-tools/bug-store` | 摸鱼工具合集 | 混合 | [摸鱼工具](/modules/moyu) |
| `/blog`、`/blog/editor`、`/blog/editor/:id`、`/blog/:userId`、`/blog/:userId/article/:articleId` | 博客后台、编辑器、公开主页、文章详情 | 混合 | [博客](/modules/blog) |
| `/codepen`、`/codepen/editor`、`/codepen/editor/:id`、`/codepen/my`、`/codepen/:id` | 代码广场、编辑器、我的作品、作品详情 | 混合 | [代码工坊](/modules/codepen) |
| `/flashcard`、`/flashcard/deck/:id`、`/flashcard/study`、`/flashcard/study/:deckId`、`/flashcard/my`、`/flashcard/deck/create`、`/flashcard/deck/:id/edit`、`/flashcard/deck/:deckId/cards` | 闪卡浏览、学习、编辑 | 混合 | [闪卡](/modules/flashcard) |

## 管理端路由

| 路由 | 页面 | 功能文档 |
| --- | --- | --- |
| `/login` | 管理端登录 | [鉴权与用户体系](/modules/auth) |
| `/dashboard` | 仪表板 | [仪表盘与日志](/modules/dashboard-logs) |
| `/user` | 用户管理 | [用户账户与个人中心](/modules/user-account) |
| `/interview/categories`、`/interview/question-sets`、`/interview/questions` | 题库分类、题单、题目 | [面试题库](/modules/interview) |
| `/mock-interview/sessions`、`/mock-interview/directions` | 模拟面试会话和方向 | [模拟面试与求职作战台](/modules/mock-interview-job-battle) |
| `/oj/problems`、`/oj/problems/create`、`/oj/problems/:id/edit` | OJ 题目管理 | [OJ 判题系统](/modules/oj) |
| `/oj/contests`、`/oj/contests/create`、`/oj/contests/:id/edit`、`/oj/tags` | 赛事和标签 | [OJ 判题系统](/modules/oj) |
| `/resume/templates`、`/resume/analytics`、`/resume/reports` | 简历模板、数据、巡检 | [简历系统](/modules/resume) |
| `/knowledge/maps`、`/knowledge/maps/:id/edit` | 知识图谱管理 | [知识图谱](/modules/knowledge) |
| `/learning-assets/review`、`/learning-assets/statistics` | 学习资产审核和统计 | [学习资产](/modules/learning-assets) |
| `/community/categories`、`/community/tags`、`/community/posts`、`/community/comments`、`/community/users` | 社区运营 | [社区帖子](/modules/community) |
| `/moments/list`、`/moments/comments`、`/moments/statistics` | 动态运营 | [动态广场](/modules/moments) |
| `/chat/messages`、`/chat/users` | 聊天消息和在线用户 | [IM 聊天室](/modules/chat) |
| `/logs/login`、`/logs/operation` | 登录日志、操作日志 | [仪表盘与日志](/modules/dashboard-logs) |
| `/notification` | 通知管理 | [通知中心](/modules/notification) |
| `/sensitive/words`、`/sensitive/whitelist`、`/sensitive/strategy`、`/sensitive/statistics`、`/sensitive/source`、`/sensitive/version`、`/sensitive/config` | 敏感词风控台 | [敏感词风控](/modules/sensitive) |
| `/filestorage/storage-config`、`/filestorage/file-management`、`/filestorage/migration`、`/filestorage/system-settings` | 文件存储管理 | [文件存储](/modules/file-storage) |
| `/system/ai-config`、`/system/ai-governance` | AI 配置和治理 | [AI Runtime](/modules/ai-runtime) |
| `/system/version` | 版本管理 | [版本历史](/modules/version-history) |
| `/profile/index`、`/profile/edit`、`/profile/change-password` | 管理员个人中心 | [鉴权与用户体系](/modules/auth) |
| `/codepen/pens`、`/codepen/templates`、`/codepen/tags`、`/codepen/statistics` | 代码工坊运营 | [代码工坊](/modules/codepen) |
| `/moyu/calendar-events`、`/moyu/daily-content`、`/moyu/statistics`、`/moyu/bug-store` | 摸鱼工具运营 | [摸鱼工具](/modules/moyu) |
| `/points/index`、`/points/users`、`/points/details`、`/points/grant` | 积分后台 | [积分与抽奖](/modules/points) |
| `/blog/articles`、`/blog/categories`、`/blog/tags` | 博客运营 | [博客](/modules/blog) |
| `/lottery` | 抽奖管理 | [积分与抽奖](/modules/points) |

## 路由守卫

用户端和管理端都在 `router.beforeEach` 中完成登录态校验和标题设置。用户端按 `meta.requiresAuth` 判断，管理端除 `/login` 外默认需要登录。

## 截图素材

操作手册截图已沉淀在 `docs/manual-assets/2026-04-24/user` 和 `docs/manual-assets/2026-04-24/admin`。后续写“用户端操作手册”和“管理端操作手册”时，应优先复用这些素材。
