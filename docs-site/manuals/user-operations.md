# 用户端操作手册

本页把用户端页面、入口、截图素材和真实验证链路统一整理。原始手册位于 `docs/Code-Nest-本地全功能截图测试与操作手册-2026-04-24.md`，截图源目录为 `docs/manual-assets/2026-04-24/user`。

## 本地入口

| 项 | 值 |
| --- | --- |
| 用户端地址 | `http://localhost:3001` |
| 登录页 | `http://localhost:3001/login` |
| 注册页 | `http://localhost:3001/register` |
| 后端 API | `http://localhost:9999/api` |
| 截图目录 | `docs/manual-assets/2026-04-24/user` |

## 登录与首页

| 页面 | 地址 | 截图 |
| --- | --- | --- |
| 登录 | `/login` | `00-login.png` |
| 注册 | `/register` | `00-register.png` |
| 首页 | `/` | `01-home.png` |

登录注册页面依赖用户端验证码。验证码明文会出现在后端日志中，可用关键字 `生成验证码成功` 查找。

## 学习成长

| 页面 | 地址 | 主要能力 | 截图 |
| --- | --- | --- | --- |
| 面试题库 | `/interview` | 题单列表、分类筛选、学习入口 | `02-interview.png` |
| 随机抽题 | `/interview/random` | 随机题目训练 | `53-interview-random.png` |
| 题单详情 | `/interview/question-sets/:id` | 题目列表、学习进度 | `36-interview-question-set-detail.png` |
| 题目学习 | `/interview/questions/:setId/:questionId` | 答案展开、掌握度标记、上一题下一题 | `37` 到 `41` 系列 |
| 面试收藏 | `/interview/favorites` | 收藏题目列表 | `54-interview-favorites.png` |
| 智能复习 | `/interview/review` | 掌握度复习 | `55-interview-review.png` |
| OJ | `/oj` | 题目列表、难度、标签 | `03-oj.png` |
| OJ 题目 | `/oj/problem/:id` | 题面、代码编辑、运行提交 | `42-oj-problem-detail.png` |
| 我的提交 | `/oj/my-submissions` | 提交记录 | `43-oj-my-submissions.png` |
| 提交详情 | `/oj/submission/:id` | 判题结果和错误信息 | `44-oj-submission-detail.png` |
| 赛事中心 | `/oj/contests` | 赛事列表 | `82-oj-contests-list.png` |
| 赛事详情 | `/oj/contests/:id` | 报名、赛事题目、榜单 | `45-oj-contest-detail.png` |
| 做题统计 | `/oj/statistics` | 个人做题数据 | `56-oj-statistics.png` |
| 排行榜 | `/oj/ranking` | 用户排行 | `57-oj-ranking.png` |
| 练习场 | `/oj/playground` | 独立代码运行 | `27-oj-playground.png` |
| AI 模拟面试 | `/mock-interview` | 方向选择和入口 | `04-mock-interview.png` |
| 面试配置 | `/mock-interview/config` | 难度、题量、风格 | `80`、`81` 系列 |
| 面试会话 | `/mock-interview/session?sessionId=...` | 作答、追问、评分反馈 | `99` 到 `101` 系列 |
| 面试报告 | `/mock-interview/report?sessionId=...` | 总结、评分、学习建议、转资产 | `102`、`106`、`107` |
| 面试历史 | `/mock-interview/history` | 历史会话回流 | `28`、`103` |
| 求职作战台 | `/job-battle` | JD 解析、简历匹配、计划 | `05-job-battle.png` |
| 岗位匹配引擎 | `/job-match-engine` | 多岗位匹配排序 | `58-job-match-engine.png` |
| 求职闭环 | `/career-loop` | 阶段追踪和动作清单 | `06-career-loop.png` |
| 学习驾驶舱 | `/learning-cockpit` | 成长分、能力雷达、今日任务 | `07-learning-cockpit.png` |
| 学习资产 | `/learning-assets` | 资产转化记录、发布状态 | `08`、`108`、`109`、`116` |
| SQL 优化 | `/sql-optimizer/workbench` | SQL 分析、改写、对比 | `09-sql-optimizer.png` |
| 计划打卡 | `/plan` | 计划、每日打卡、复盘 | `15-plan.png` |
| 学习小组 | `/team` | 小组广场、加入、讨论 | `16-team.png` |
| 创建小组 | `/team/create` | 新建小组 | `83-team-create.png` |
| 我的小组 | `/team/my` | 已加入小组 | `75-team-my.png` |
| 小组详情 | `/team/:id` | 任务、打卡、讨论 | `52`、`98` |
| 闪卡 | `/flashcard` | 公开卡组和学习入口 | `21-flashcard.png` |
| 我的卡组 | `/flashcard/my` | 私有卡组 | `33-flashcard-my.png` |
| 卡组详情 | `/flashcard/deck/:id` | 卡组信息 | `50-flashcard-deck-detail.png` |
| 闪卡学习 | `/flashcard/study/:deckId` | 记忆复习 | `51-flashcard-study-complete.png` |
| 卡组和卡片编辑 | `/flashcard/deck/create`、`/flashcard/deck/:id/edit`、`/flashcard/deck/:deckId/cards` | 创建、编辑、维护卡片 | `73`、`74`、`84`、`85` |
| 知识图谱 | `/knowledge`、`/knowledge/maps/:id` | 图谱列表和图谱学习 | `18`、`76` |

## 内容创作

| 页面 | 地址 | 主要能力 | 截图 |
| --- | --- | --- | --- |
| 简历管理 | `/resume` | 简历列表、保存回流 | `10`、`95` |
| 简历模板 | `/resume/templates` | 模板中心 | `29-resume-templates.png` |
| 简历编辑器 | `/resume/editor` | 创建和编辑简历 | `59-resume-editor.png` |
| 技术社区 | `/community` | 帖子列表 | `11-community.png` |
| 发帖 | `/community/create` | 发布帖子 | `30-community-create.png` |
| 帖子详情 | `/community/posts/:id` | 评论、点赞、AI 摘要 | `47`、`92` |
| 我的收藏和帖子 | `/community/collections`、`/community/my-posts` | 收藏和个人内容 | `60`、`61`、`91` |
| 用户主页 | `/community/users/:userId` | 用户社区资料 | `62-community-user-profile.png` |
| 博客 | `/blog` | 开通博客、文章列表 | `19`、`87`、`88` |
| 博客编辑器 | `/blog/editor` | Markdown 写作 | `31-blog-editor.png` |
| 博客主页和文章详情 | `/blog/:userId`、`/blog/:userId/article/:articleId` | 公开博客展示 | `48`、`71`、`89` |
| 代码广场 | `/codepen` | 在线作品广场 | `20-codepen.png` |
| 代码编辑器 | `/codepen/editor` | HTML/CSS/JS 在线编辑 | `32-codepen-editor.png` |
| 我的作品和详情 | `/codepen/my`、`/codepen/:id` | 发布回流、点赞、详情 | `49`、`72`、`96`、`97` |

## 社交互动与工具

| 页面 | 地址 | 主要能力 | 截图 |
| --- | --- | --- | --- |
| 动态广场 | `/moments` | 发布、点赞、收藏、评论 | `12`、`93`、`94` |
| 动态用户主页和收藏 | `/moments/user/:userId`、`/moments/my-favorites` | 用户主页、收藏列表 | `63`、`64` |
| 聊天室 | `/chat` | WebSocket 消息、图片、撤回、公告、禁言、踢出 | `13`、`104` 到 `115` |
| 通知中心 | `/notification` | 系统通知、已读未读 | `22-notification.png` |
| 我的积分 | `/points` | 余额、签到、流水 | `14`、`86` |
| 幸运抽奖 | `/lottery` | 抽奖和记录 | `17`、`90` |
| 个人中心 | `/profile` | 资料维护 | `23-profile.png` |
| 程序员工具 | `/dev-tools` | 工具合集 | `24-dev-tools.png` |
| JSON 工具 | `/dev-tools/json` | JSON 格式化 | `34-dev-tools-json.png` |
| 文本比对 | `/dev-tools/text-diff` | 文本 diff | `65-dev-tools-text-diff.png` |
| 聚合翻译 | `/dev-tools/translation` | 翻译工具 | `66-dev-tools-translation.png` |
| 摸鱼工具 | `/moyu-tools` | 工具合集 | `25-moyu-tools.png` |
| 今日热榜 | `/moyu-tools/hot-topics` | 多平台热榜 | `35-moyu-hot-topics.png` |
| 时薪计算器 | `/moyu-tools/salary-calculator` | 薪资和摸鱼统计 | `67-moyu-salary-calculator.png` |
| 程序员日历 | `/moyu-tools/calendar` | 事件日历 | `68-moyu-calendar.png` |
| 每日内容 | `/moyu-tools/daily-content` | 每日语录/文章 | `69-moyu-daily-content.png` |
| Bug 商店 | `/moyu-tools/bug-store` | Bug 资产库 | `70-moyu-bug-store.png` |
| 版本历史 | `/version-history` | 版本墙 | `26-version-history.png` |

## 验证口径

1. 直达页面只证明路由和页面渲染正常。
2. 带参数详情页优先从列表点击进入，避免本地数据 ID 不一致。
3. 写操作以“接口返回成功、页面回流、数据库或后台可见”三者同时成立为完整闭环。
4. 依赖外部服务的功能需要额外确认 sidecar 或 go-judge 可用。
