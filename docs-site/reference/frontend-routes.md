# 前端路由索引

本页来自 `vue3-user-front/src/router/index.js` 和 `vue3-admin-front/src/router/index.js`。它用于把用户可见页面、管理后台页面和对应模块文档串起来。

## 路由架构概览

| 维度 | 用户端 | 管理端 |
| --- | --- | --- |
| 路由模式 | HTML5 History | HTML5 History |
| Base URL | / | import.meta.env.BASE_URL |
| 路由总数 | ~60 条 | ~40 条 |
| 认证策略 | meta.requiresAuth 显式声明 | 默认全部需要登录 |
| 页面缓存 | meta.keepAlive 控制 | 无 |
| Layout | App.vue 全局 Header + router-view | Layout 组件包裹侧边栏+内容区 |
| 页面切换动画 | page-fade (opacity + translateY) | 无 |
| 404 处理 | `:pathMatch(.*)*` 通配 | `:pathMatch(.*)*` 通配 |
| 滚动行为 | 回到顶部 | 浏览器默认 |
| 路由懒加载 | 全部 `() => import()` | 全部 `() => import()` |
| 导航增强 | Ctrl+K 命令面板 | 无 |

## 用户端路由

### 核心入口

| 路由 | name | 页面 | 认证 | keepAlive | 功能文档 |
| --- | --- | --- | --- | --- | --- |
| `/` | Home | 首页 | 需要 | - | [模块总览](/modules/) |
| `/login` | Login | 登录 | 不需要 | - | [鉴权与用户体系](/modules/auth) |
| `/register` | Register | 注册 | 不需要 | - | [鉴权与用户体系](/modules/auth) |

### 面试题库

| 路由 | name | 页面 | 认证 | keepAlive | 功能文档 |
| --- | --- | --- | --- | --- | --- |
| `/interview` | Interview | 面试题库首页 | 需要 | **true** | [面试题库](/modules/interview) |
| `/interview/random` | RandomQuestions | 随机抽题 | 需要 | false | [面试题库](/modules/interview) |
| `/interview/question-sets/:id` | QuestionSetDetail | 题单详情 | 需要 | **true** | [面试题库](/modules/interview) |
| `/interview/questions/:setId/:questionId` | QuestionDetail | 题目学习 | 需要 | false | [面试题库](/modules/interview) |
| `/interview/favorites` | MyFavorites | 我的收藏 | 需要 | **true** | [面试题库](/modules/interview) |
| `/interview/review` | InterviewReview | 智能复习 | 需要 | false | [面试题库](/modules/interview) |

### 在线判题 (OJ)

| 路由 | name | 页面 | 认证 | keepAlive | 功能文档 |
| --- | --- | --- | --- | --- | --- |
| `/oj` | OJ | 在线判题列表 | 需要 | **true** | [OJ 判题系统](/modules/oj) |
| `/oj/contests` | OjContests | 赛事中心 | 需要 | **true** | [OJ 判题系统](/modules/oj) |
| `/oj/contests/:id` | OjContestDetail | 赛事详情 | 需要 | false | [OJ 判题系统](/modules/oj) |
| `/oj/problem/:id` | OjProblemDetail | 题目详情 | 需要 | false | [OJ 判题系统](/modules/oj) |
| `/oj/submission/:id` | OjSubmissionDetail | 提交详情 | 需要 | false | [OJ 判题系统](/modules/oj) |
| `/oj/my-submissions` | OjMySubmissions | 我的提交 | 需要 | **true** | [OJ 判题系统](/modules/oj) |
| `/oj/statistics` | OjStatistics | 做题统计 | 需要 | false | [OJ 判题系统](/modules/oj) |
| `/oj/playground` | OjPlayground | 练习场 | 需要 | false | [OJ 判题系统](/modules/oj) |
| `/oj/ranking` | OjRanking | 排行榜 | 需要 | **true** | [OJ 判题系统](/modules/oj) |

### 模拟面试与求职

| 路由 | name | 页面 | 认证 | keepAlive | 功能文档 |
| --- | --- | --- | --- | --- | --- |
| `/mock-interview` | MockInterview | AI模拟面试入口 | 需要 | **true** | [模拟面试与求职作战台](/modules/mock-interview-job-battle) |
| `/mock-interview/config` | MockInterviewConfig | 面试配置 | 需要 | false | [模拟面试与求职作战台](/modules/mock-interview-job-battle) |
| `/mock-interview/session` | MockInterviewSession | 面试进行中 | 需要 | false | [模拟面试与求职作战台](/modules/mock-interview-job-battle) |
| `/mock-interview/report` | MockInterviewReport | 面试报告 | 需要 | false | [模拟面试与求职作战台](/modules/mock-interview-job-battle) |
| `/mock-interview/history` | MockInterviewHistory | 面试历史 | 需要 | **true** | [模拟面试与求职作战台](/modules/mock-interview-job-battle) |
| `/job-battle` | JobBattle | 求职作战台 | 需要 | **true** | [模拟面试与求职作战台](/modules/mock-interview-job-battle) |
| `/job-match-engine` | JobMatchEngine | 岗位匹配引擎 2.0 | 需要 | **true** | [模拟面试与求职作战台](/modules/mock-interview-job-battle) |
| `/career-loop` | CareerLoop | 求职闭环中台 | 需要 | **true** | [模拟面试与求职作战台](/modules/mock-interview-job-battle) |

### 学习与成长

| 路由 | name | 页面 | 认证 | keepAlive | 功能文档 |
| --- | --- | --- | --- | --- | --- |
| `/learning-cockpit` | LearningCockpit | 学习成长驾驶舱 2.0 | 需要 | **true** | [题库与成长闭环](/modules/interview-and-growth) |
| `/learning-assets` | LearningAssets | 我的学习资产 | 需要 | **true** | [学习资产](/modules/learning-assets) |
| `/sql-optimizer/workbench` | SqlOptimizerWorkbench | SQL优化工作台 2.0 | 需要 | **true** | [SQL 优化工作台](/modules/sql-optimizer) |
| `/growth-autopilot` | GrowthAutopilot | 成长闭环自动驾驶 | 需要 | **true** | [题库与成长闭环](/modules/interview-and-growth) |
| `/knowledge` | Knowledge | 知识图谱广场 | 需要 | **true** | [知识图谱](/modules/knowledge) |
| `/knowledge/maps/:id` | KnowledgeMapViewer | 知识图谱学习 | 需要 | false | [知识图谱](/modules/knowledge) |

> **注意**：`/growth-autopilot` 实际是一个重定向路由，自动跳转到 `/learning-cockpit?tab=autopilot`，保持 URL 兼容性。

### 简历系统

| 路由 | name | 页面 | 认证 | keepAlive | 功能文档 |
| --- | --- | --- | --- | --- | --- |
| `/resume` | ResumeHome | 简历管理 | 需要 | **true** | [简历系统](/modules/resume) |
| `/resume/templates` | ResumeTemplates | 简历模板中心 | 需要 | **true** | [简历系统](/modules/resume) |
| `/resume/editor` | ResumeCreate | 创建简历 | 需要 | false | [简历系统](/modules/resume) |
| `/resume/editor/:id` | ResumeEdit | 编辑简历 | 需要 | false | [简历系统](/modules/resume) |

### 社区与互动

| 路由 | name | 页面 | 认证 | keepAlive | 功能文档 |
| --- | --- | --- | --- | --- | --- |
| `/community` | Community | 技术社区首页 | 需要 | **true** | [社区帖子](/modules/community) |
| `/community/posts/:id` | PostDetail | 帖子详情 | 需要 | false | [社区帖子](/modules/community) |
| `/community/collections` | MyCollections | 我的收藏 | 需要 | **true** | [社区帖子](/modules/community) |
| `/community/my-posts` | MyPosts | 我的帖子 | 需要 | **true** | [社区帖子](/modules/community) |
| `/community/create` | CreatePost | 创作帖子 | 需要 | false | [社区帖子](/modules/community) |
| `/community/users/:userId` | CommunityUserProfile | 用户主页 | 需要 | false | [社区帖子](/modules/community) |
| `/notification` | Notification | 通知中心 | 需要 | **true** | [通知中心](/modules/notification) |
| `/moments` | Moments | 朋友圈 | 需要 | **true** | [动态广场](/modules/moments) |
| `/moments/user/:userId` | MomentUserProfile | 用户主页 | 需要 | false | [动态广场](/modules/moments) |
| `/moments/my-favorites` | MomentFavorites | 我的收藏 | 需要 | **true** | [动态广场](/modules/moments) |
| `/chat` | Chat | IM 聊天室 | 需要 | false | [IM 聊天室](/modules/chat) |

### 个人中心与积分

| 路由 | name | 页面 | 认证 | keepAlive | 功能文档 |
| --- | --- | --- | --- | --- | --- |
| `/profile` | Profile | 个人中心 | 需要 | - | [用户账户与个人中心](/modules/user-account) |
| `/points` | Points | 我的积分 | 需要 | **true** | [积分与抽奖](/modules/points) |
| `/lottery` | Lottery | 幸运抽奖 | 需要 | false | [积分与抽奖](/modules/points) |
| `/plan` | PlanCheckin | 计划打卡 | 需要 | **true** | [计划与学习小组](/modules/plan-team) |

### 学习小组

| 路由 | name | 页面 | 认证 | keepAlive | 功能文档 |
| --- | --- | --- | --- | --- | --- |
| `/team` | TeamSquare | 小组广场 | 需要 | **true** | [计划与学习小组](/modules/plan-team) |
| `/team/create` | TeamCreate | 创建小组 | 需要 | false | [计划与学习小组](/modules/plan-team) |
| `/team/my` | MyTeams | 我的小组 | 需要 | **true** | [计划与学习小组](/modules/plan-team) |
| `/team/:id` | TeamDetail | 小组详情 | 需要 | false | [计划与学习小组](/modules/plan-team) |
| `/team/:id/edit` | TeamEdit | 编辑小组 | 需要 | false | [计划与学习小组](/modules/plan-team) |

### 闪卡记忆

| 路由 | name | 页面 | 认证 | keepAlive | 功能文档 |
| --- | --- | --- | --- | --- | --- |
| `/flashcard` | Flashcard | 闪卡浏览 | 不需要 | **true** | [闪卡](/modules/flashcard) |
| `/flashcard/deck/:id` | FlashcardDeckDetail | 卡组详情 | 不需要 | false | [闪卡](/modules/flashcard) |
| `/flashcard/study` | FlashcardStudy | 闪卡学习 | 需要 | false | [闪卡](/modules/flashcard) |
| `/flashcard/study/:deckId` | FlashcardDeckStudy | 卡组学习 | 需要 | false | [闪卡](/modules/flashcard) |
| `/flashcard/my` | MyFlashcardDecks | 我的卡组 | 需要 | **true** | [闪卡](/modules/flashcard) |
| `/flashcard/deck/create` | CreateFlashcardDeck | 创建卡组 | 需要 | false | [闪卡](/modules/flashcard) |
| `/flashcard/deck/:id/edit` | EditFlashcardDeck | 编辑卡组 | 需要 | false | [闪卡](/modules/flashcard) |
| `/flashcard/deck/:deckId/cards` | FlashcardCards | 管理闪卡 | 需要 | false | [闪卡](/modules/flashcard) |

### 博客

| 路由 | name | 页面 | 认证 | keepAlive | 功能文档 |
| --- | --- | --- | --- | --- | --- |
| `/blog` | MyBlog | 我的博客 | 需要 | **true** | [博客](/modules/blog) |
| `/blog/editor` | BlogEditor | 写文章 | 需要 | false | [博客](/modules/blog) |
| `/blog/editor/:id` | BlogEditorEdit | 编辑文章 | 需要 | false | [博客](/modules/blog) |
| `/blog/:userId` | BlogHome | 博客主页 | **不需要** | **true** | [博客](/modules/blog) |
| `/blog/:userId/article/:articleId` | ArticleDetail | 文章详情 | **不需要** | false | [博客](/modules/blog) |

### 代码工坊

| 路由 | name | 页面 | 认证 | keepAlive | 功能文档 |
| --- | --- | --- | --- | --- | --- |
| `/codepen` | CodePenSquare | 代码广场 | **不需要** | **true** | [代码工坊](/modules/codepen) |
| `/codepen/editor` | CodePenEditor | 代码编辑器 | 需要 | false | [代码工坊](/modules/codepen) |
| `/codepen/editor/:id` | CodePenEditorEdit | 编辑作品 | 需要 | false | [代码工坊](/modules/codepen) |
| `/codepen/my` | MyCodePens | 我的作品 | 需要 | **true** | [代码工坊](/modules/codepen) |
| `/codepen/:id` | CodePenDetail | 作品详情 | **不需要** | false | [代码工坊](/modules/codepen) |

### 工具与娱乐

| 路由 | name | 页面 | 认证 | keepAlive | 功能文档 |
| --- | --- | --- | --- | --- | --- |
| `/dev-tools` | DevTools | 程序员工具首页 | 不需要 | **true** | [开发者工具](/modules/dev-tools) |
| `/dev-tools/json` | JsonTool | JSON工具 | 不需要 | **true** | [开发者工具](/modules/dev-tools) |
| `/dev-tools/text-diff` | TextDiff | 文本比对 | 不需要 | **true** | [开发者工具](/modules/dev-tools) |
| `/dev-tools/translation` | Translation | 聚合翻译 | 不需要 | **true** | [开发者工具](/modules/dev-tools) |
| `/moyu-tools` | MoyuTools | 摸鱼工具首页 | 需要 | **true** | [摸鱼工具](/modules/moyu) |
| `/moyu-tools/hot-topics` | HotTopics | 今日热榜 | **不需要** | **true** | [摸鱼工具](/modules/moyu) |
| `/moyu-tools/salary-calculator` | SalaryCalculator | 时薪计算器 | 需要 | false | [摸鱼工具](/modules/moyu) |
| `/moyu-tools/calendar` | DeveloperCalendar | 程序员日历 | 需要 | **true** | [摸鱼工具](/modules/moyu) |
| `/moyu-tools/daily-content` | DailyContent | 每日内容 | 需要 | **true** | [摸鱼工具](/modules/moyu) |
| `/moyu-tools/bug-store` | BugStore | Bug商店 | 需要 | false | [摸鱼工具](/modules/moyu) |
| `/version-history` | VersionHistory | 版本更新历史 | 不需要 | **true** | [版本历史](/modules/version-history) |

### 404 兜底

| 路由 | name | 页面 | 认证 |
| --- | --- | --- | --- |
| `/:pathMatch(.*)*` | NotFound | 404 页面 | - |

## 管理端路由

管理端采用 Layout 组件统一包裹，所有管理页面共享侧边栏 + 内容区布局。登录页 `/login` 是唯一的例外，不使用 Layout。

### 核心页面

| 路由 | name | 页面 | 功能文档 |
| --- | --- | --- | --- |
| `/login` | Login | 管理端登录 | [鉴权与用户体系](/modules/auth) |
| `/dashboard` | Dashboard | 仪表板 | [仪表盘与日志](/modules/dashboard-logs) |
| `/user` | User | 用户管理 | [用户账户与个人中心](/modules/user-account) |

### 运营管理

| 路由 | name | 页面 | 功能文档 |
| --- | --- | --- | --- |
| `/interview/categories` | InterviewCategories | 题库分类管理 | [面试题库](/modules/interview) |
| `/interview/question-sets` | QuestionSets | 题单管理 | [面试题库](/modules/interview) |
| `/interview/questions` | Questions | 题目管理 | [面试题库](/modules/interview) |
| `/mock-interview/sessions` | MockInterviewSessions | 面试会话 | [模拟面试与求职作战台](/modules/mock-interview-job-battle) |
| `/mock-interview/directions` | MockInterviewDirections | 方向配置 | [模拟面试与求职作战台](/modules/mock-interview-job-battle) |
| `/community/categories` | CommunityCategories | 分类管理 | [社区帖子](/modules/community) |
| `/community/tags` | CommunityTags | 标签管理 | [社区帖子](/modules/community) |
| `/community/posts` | CommunityPosts | 帖子管理 | [社区帖子](/modules/community) |
| `/community/comments` | CommunityComments | 评论管理 | [社区帖子](/modules/community) |
| `/community/users` | CommunityUsers | 用户管理 | [社区帖子](/modules/community) |
| `/moments/list` | MomentsList | 动态管理 | [动态广场](/modules/moments) |
| `/moments/comments` | MomentsComments | 评论管理 | [动态广场](/modules/moments) |
| `/moments/statistics` | MomentsStatistics | 数据统计 | [动态广场](/modules/moments) |
| `/chat/messages` | ChatMessages | 消息管理 | [IM 聊天室](/modules/chat) |
| `/chat/users` | ChatUsers | 在线用户 | [IM 聊天室](/modules/chat) |

### OJ 管理

| 路由 | name | 页面 | 功能文档 |
| --- | --- | --- | --- |
| `/oj/problems` | OjProblems | 题目管理 | [OJ 判题系统](/modules/oj) |
| `/oj/problems/create` | OjProblemCreate | 新增题目 | [OJ 判题系统](/modules/oj) |
| `/oj/problems/:id/edit` | OjProblemEdit | 编辑题目 | [OJ 判题系统](/modules/oj) |
| `/oj/contests` | OjContests | 赛事管理 | [OJ 判题系统](/modules/oj) |
| `/oj/contests/create` | OjContestCreate | 新增赛事 | [OJ 判题系统](/modules/oj) |
| `/oj/contests/:id/edit` | OjContestEdit | 编辑赛事 | [OJ 判题系统](/modules/oj) |
| `/oj/tags` | OjTags | 标签管理 | [OJ 判题系统](/modules/oj) |

### 内容运营

| 路由 | name | 页面 | 功能文档 |
| --- | --- | --- | --- |
| `/blog/articles` | BlogArticles | 文章管理 | [博客](/modules/blog) |
| `/blog/categories` | BlogCategories | 分类管理 | [博客](/modules/blog) |
| `/blog/tags` | BlogTags | 标签管理 | [博客](/modules/blog) |
| `/codepen/pens` | CodePenManagement | 作品管理 | [代码工坊](/modules/codepen) |
| `/codepen/templates` | CodePenTemplates | 模板管理 | [代码工坊](/modules/codepen) |
| `/codepen/tags` | CodePenTags | 标签管理 | [代码工坊](/modules/codepen) |
| `/codepen/statistics` | CodePenStatistics | 数据统计 | [代码工坊](/modules/codepen) |

### 简历与学习

| 路由 | name | 页面 | 功能文档 |
| --- | --- | --- | --- |
| `/resume/templates` | ResumeTemplates | 模板管理 | [简历系统](/modules/resume) |
| `/resume/analytics` | ResumeAnalytics | 数据总览 | [简历系统](/modules/resume) |
| `/resume/reports` | ResumeReports | 健康巡检 | [简历系统](/modules/resume) |
| `/knowledge/maps` | KnowledgeMaps | 图谱管理 | [知识图谱](/modules/knowledge) |
| `/knowledge/maps/:id/edit` | KnowledgeMapEdit | 编辑图谱 | [知识图谱](/modules/knowledge) |
| `/learning-assets/review` | LearningAssetsReview | 学习资产审核台 | [学习资产](/modules/learning-assets) |
| `/learning-assets/statistics` | LearningAssetsStatistics | 学习资产统计 | [学习资产](/modules/learning-assets) |

### 积分与摸鱼

| 路由 | name | 页面 | 功能文档 |
| --- | --- | --- | --- |
| `/points/index` | PointsOverview | 积分概览 | [积分与抽奖](/modules/points) |
| `/points/users` | PointsUsers | 积分排行 | [积分与抽奖](/modules/points) |
| `/points/details` | PointsDetails | 积分明细 | [积分与抽奖](/modules/points) |
| `/points/grant` | PointsGrant | 积分发放 | [积分与抽奖](/modules/points) |
| `/lottery` | LotteryManagement | 抽奖管理 | [积分与抽奖](/modules/points) |
| `/moyu/calendar-events` | MoyuCalendarEvents | 日历事件管理 | [摸鱼工具](/modules/moyu) |
| `/moyu/daily-content` | MoyuDailyContent | 每日内容管理 | [摸鱼工具](/modules/moyu) |
| `/moyu/statistics` | MoyuStatistics | 统计分析 | [摸鱼工具](/modules/moyu) |
| `/moyu/bug-store` | MoyuBugStore | Bug商店管理 | [摸鱼工具](/modules/moyu) |

### 系统管理

| 路由 | name | 页面 | 功能文档 |
| --- | --- | --- | --- |
| `/sensitive/words` | SensitiveWords | 词库管理 | [敏感词风控](/modules/sensitive) |
| `/sensitive/whitelist` | SensitiveWhitelist | 白名单管理 | [敏感词风控](/modules/sensitive) |
| `/sensitive/strategy` | SensitiveStrategy | 策略配置 | [敏感词风控](/modules/sensitive) |
| `/sensitive/statistics` | SensitiveStatistics | 统计分析 | [敏感词风控](/modules/sensitive) |
| `/sensitive/source` | SensitiveSource | 词库来源 | [敏感词风控](/modules/sensitive) |
| `/sensitive/version` | SensitiveVersion | 版本历史 | [敏感词风控](/modules/sensitive) |
| `/sensitive/config` | SensitiveConfig | 配置管理 | [敏感词风控](/modules/sensitive) |
| `/filestorage/storage-config` | StorageConfig | 存储配置 | [文件存储](/modules/file-storage) |
| `/filestorage/file-management` | FileManagement | 文件管理 | [文件存储](/modules/file-storage) |
| `/filestorage/migration` | FileMigration | 文件迁移 | [文件存储](/modules/file-storage) |
| `/filestorage/system-settings` | SystemSettings | 系统设置 | [文件存储](/modules/file-storage) |
| `/system/ai-config` | AiConfig | AI配置与观测 | [AI Runtime](/modules/ai-runtime) |
| `/system/ai-governance` | AiGovernance | AI质量治理 | [AI Runtime](/modules/ai-runtime) |
| `/system/version` | VersionManagement | 版本管理 | [版本历史](/modules/version-history) |
| `/logs/login` | LoginLogs | 登录日志 | [仪表盘与日志](/modules/dashboard-logs) |
| `/logs/operation` | OperationLogs | 操作日志 | [仪表盘与日志](/modules/dashboard-logs) |
| `/notification` | Notification | 通知管理 | [通知中心](/modules/notification) |

### 管理员个人

| 路由 | name | 页面 | 功能文档 |
| --- | --- | --- | --- |
| `/profile/index` | Profile | 个人中心 | [鉴权与用户体系](/modules/auth) |
| `/profile/edit` | EditProfile | 编辑资料 | [鉴权与用户体系](/modules/auth) |
| `/profile/change-password` | ChangePassword | 修改密码 | [鉴权与用户体系](/modules/auth) |

## 路由守卫详解

### 用户端守卫

用户端在 `router.beforeEach` 中实现认证拦截：

```js
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()

  // 1. 设置页面标题
  document.title = `${to.meta.title || '用户端'} - Code Nest`

  // 2. 检查是否需要登录
  if (to.meta.requiresAuth) {
    if (!userStore.token || !userStore.isLoggedIn) {
      ElMessage.warning('请先登录')
      next('/login')
      return
    }
  }

  // 3. 已登录用户访问登录/注册页 → 跳转首页
  if ((to.path === '/login' || to.path === '/register')
      && userStore.token && userStore.isLoggedIn) {
    next('/')
    return
  }

  next()
})
```

**关键行为：**
- 需要 `meta.requiresAuth = true` 才触发认证检查
- 未认证时跳转 `/login`，不保留原路径（无 redirect 参数）
- 使用 Pinia userStore 中的 token + isLoggedIn 双重判断
- Token 存储在 Cookie (`user_token`) 和 localStorage 中
- 用户信息存储在 localStorage 中，页面刷新时自动恢复

### 管理端守卫

管理端同样在 `router.beforeEach` 中实现认证拦截：

```js
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  // 1. 设置页面标题
  if (to.meta?.title) {
    document.title = `${to.meta.title} - Code Nest 管理后台`
  }

  // 2. 默认全部需要登录（除了 /login）
  const requiresAuth = to.meta?.requiresAuth !== false

  if (requiresAuth && to.path !== '/login') {
    if (!userStore.token || !userStore.isLoggedIn) {
      ElMessage.warning('请先登录')
      next({
        path: '/login',
        query: { redirect: to.fullPath }  // 保存目标路径
      })
      return
    }
  }

  // 3. 已登录用户访问登录页 → 跳转首页
  if (to.path === '/login' && userStore.token && userStore.isLoggedIn) {
    next('/')
    return
  }

  next()
})
```

**与用户端的关键差异：**
- 默认所有页面需要认证（`requiresAuth !== false`），除非显式声明不需要
- 登录后带 redirect 参数跳回原页面，用户端不带
- 管理端 Token 存储在 Cookie (`token`) 中，键名不同于用户端的 `user_token`
- 管理端支持 Token 过期自动刷新（30分钟内过期触发 refresh）
- 管理端支持跨 Tab 页登出广播（CustomEvent）

### 双端认证隔离

| 维度 | 用户端 | 管理端 |
| --- | --- | --- |
| Cookie 键名 | `user_token` | `token` |
| localStorage 键名 | `user_token` + `user_info` | `token` + `userInfo` + `tokenExpireTime` |
| 后端认证 | StpUserUtil (Sa-Token) | StpAdminUtil (Sa-Token) |
| Redis DB | db3 (业务) + db4 (Sa-Token) | db3 + db4 (同一实例) |
| Token 刷新 | 无自动刷新 | 30分钟内过期自动刷新 |
| 跨 Tab 同步 | 无 | CustomEvent 广播 |

## keepAlive 页面缓存策略

用户端使用 `meta.keepAlive` 控制页面缓存。缓存策略遵循以下原则：

| 场景 | keepAlive | 原因 |
| --- | --- | --- |
| 列表/广场页 | **true** | 返回时保留滚动位置和筛选条件 |
| 详情/编辑页 | false | 确保数据实时、状态干净 |
| 学习/答题页 | false | 确保进度正确、每次全新状态 |
| 工具首页 | **true** | 保留用户已输入的内容 |

> **实现注意**：当前 App.vue 未使用 `KeepAlive` 组件包裹 `component :is`，因此 `keepAlive` meta 字段虽已定义，但实际缓存效果取决于后续是否在 App.vue 中添加 `KeepAlive` 包裹。如需启用，修改为：
>
> ```vue
> <router-view v-slot="{ Component }">
>   <transition name="page-fade" mode="out-in">
>     <keep-alive :include="cachedViews">
>       <component :is="Component" />
>     </keep-alive>
>   </transition>
> </router-view>
> ```

## 懒加载与分包策略

两端均使用 `() => import()` 动态导入实现路由级代码分割。Vite 在构建时自动将动态导入的组件拆分为独立 chunk。

### 用户端 Vite 分包配置

```js
build: {
  rollupOptions: {
    output: {
      manualChunks: {
        'monaco-editor': ['monaco-editor'],
        'g6': ['@antv/g6'],
        'markdown': ['markdown-it', 'dompurify'],
        'element-plus': ['element-plus'],
        'vue-vendor': ['vue', 'vue-router', 'pinia'],
        'axios': ['axios']
      }
    }
  }
}
```

### 管理端 Vite 分包配置

```js
manualChunks: {
  'monaco-editor': ['monaco-editor'],
  'echarts': ['echarts'],
  'd3': ['d3'],
  'g6': ['@antv/g6'],
  'element-plus': ['element-plus'],
  'vue-vendor': ['vue', 'vue-router', 'pinia'],
  'axios': ['axios']
}
```

## 命令面板导航 (用户端)

用户端实现了类 VS Code 的命令面板（Ctrl+K），配置在 `src/config/navigation.js` 中。面板分为 5 个区域：

| 区域 | 说明 | 条目数 |
| --- | --- | --- |
| 常用场景 | 首页、社区、驾驶舱、通知 | 4 |
| 学习与训练 | 面试、求职、OJ、SQL、图谱等 | 15 |
| 创作与输出 | 代码工坊、博客、简历、工具 | 4 |
| 社区与互动 | 朋友圈、聊天室、小组 | 3 |
| 娱乐与辅助 | 摸鱼、抽奖、积分、版本历史 | 6 |

命令面板还支持「最近访问」历史记录，通过 localStorage 持久化最近 8 条访问路径。

## 截图素材

操作手册截图已沉淀在 `AI-DOCS/assets/images/2026-04-24/user` 和 `AI-DOCS/assets/images/2026-04-24/admin`。编写"用户端操作手册"和"管理端操作手册"时，应优先复用这些素材。

## 维护检查清单

前端路由变更后，检查：

1. 用户端或管理端 `src/router/index.js` 已更新。
2. 新路由的认证要求和页面实际访问行为一致。
3. 本页同步记录路由、页面说明和对应功能文档。
4. 对应模块页已补用户入口或管理入口。
5. 如果有截图验证价值，操作手册也同步更新。
6. 新增路由的 `name` 不与现有路由冲突。
7. 带 `keepAlive` 的路由确认组件内实现了 `onActivated` 数据刷新（若需要）。
8. 带 `:id` 动态参数的路由确认 `props: true` 已设置（若组件使用 props 接收参数）。

## 源码导航

| 文件 | 说明 |
| --- | --- |
| `vue3-user-front/src/router/index.js` | 用户端路由定义 + 守卫（908 行） |
| `vue3-admin-front/src/router/index.js` | 管理端路由定义 + 守卫（634 行） |
| `vue3-user-front/src/config/navigation.js` | 命令面板导航配置 |
| `vue3-user-front/src/stores/user.js` | 用户端 Pinia Store（Token/用户信息） |
| `vue3-admin-front/src/stores/user.js` | 管理端 Pinia Store（Token 刷新/广播） |
| `vue3-user-front/src/utils/command-history.js` | 命令面板历史持久化 |


## 相关文档

| 文档 | 说明 |
| --- | --- |
| [前端应用](/architecture/frontend-apps) | 前端架构设计 |
| [API 路由索引](/reference/api-routes) | 后端接口清单 |
| [前端渲染安全](/reference/frontend-rendering-security) | XSS 防护规范 |
| [模块总览](/modules/) | 各模块功能说明 |
