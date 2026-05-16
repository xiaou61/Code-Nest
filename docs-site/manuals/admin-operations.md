# 管理端操作手册

本页整理管理端页面、截图素材和运营动作。截图源目录为 `docs/manual-assets/2026-04-24/admin`。

## 本地入口

| 项 | 值 |
| --- | --- |
| 管理端地址 | `http://localhost:3000` |
| 登录页 | `http://localhost:3000/login` |
| 默认账号 | `admin` |
| 默认密码 | `123456` |
| 截图目录 | `docs/manual-assets/2026-04-24/admin` |

## 基础运营

| 页面 | 地址 | 主要能力 | 截图 |
| --- | --- | --- | --- |
| 登录 | `/login` | 管理端登录 | `00-login.png` |
| 仪表板 | `/dashboard` | 用户、积分、在线、登录、操作健康 | `01-dashboard.png` |
| 用户管理 | `/user` | 用户查询、创建、编辑、状态、重置密码 | `02-user.png` |
| 登录日志 | `/logs/login` | 登录行为查询和清理 | `16-logs-login.png` |
| 操作日志 | `/logs/operation` | 操作行为查询、删除、清理 | `45-logs-operation.png` |
| 个人中心 | `/profile/index` | 管理员资料 | `64-profile-index.png` |
| 编辑资料 | `/profile/edit` | 管理员资料更新 | `65-profile-edit.png` |
| 修改密码 | `/profile/change-password` | 管理员密码修改 | `66-profile-change-password.png` |

## 学习成长运营

| 页面 | 地址 | 主要能力 | 截图 |
| --- | --- | --- | --- |
| 面试分类 | `/interview/categories` | 分类管理 | `03-interview-categories.png` |
| 面试题单 | `/interview/question-sets` | 题单管理和导入 | `34-interview-question-sets.png` |
| 面试题目 | `/interview/questions` | 题目管理 | `04-interview-questions.png` |
| 模拟面试会话 | `/mock-interview/sessions` | 会话列表和详情 | `05`、`72`、`73` |
| 模拟面试方向 | `/mock-interview/directions` | 方向配置 | `35-mock-interview-directions.png` |
| OJ 题目 | `/oj/problems` | 题目列表 | `06-oj-problems.png` |
| 新增 OJ 题目 | `/oj/problems/create` | 新建题目 | `29-oj-problem-create.png` |
| 编辑 OJ 题目 | `/oj/problems/:id/edit` | 编辑题目和测试用例 | `31-oj-problem-edit-with-testcase-modal.png` |
| OJ 赛事 | `/oj/contests` | 赛事列表 | `07-oj-contests.png` |
| 新增 OJ 赛事 | `/oj/contests/create` | 新建赛事 | `30-oj-contest-create.png` |
| 编辑 OJ 赛事 | `/oj/contests/:id/edit` | 编辑赛事 | `32-oj-contest-edit.png` |
| OJ 标签 | `/oj/tags` | 标签管理 | `36-oj-tags.png` |
| 知识图谱 | `/knowledge/maps` | 图谱列表 | `10-knowledge-maps.png` |
| 知识图谱编辑 | `/knowledge/maps/:id/edit` | 节点树和节点面板 | `33-knowledge-map-edit-node-panel.png` |
| 学习资产审核 | `/learning-assets/review` | 候选资产审核、通过、驳回、合并 | `11`、`74`、`78`、`80` |
| 学习资产统计 | `/learning-assets/statistics` | 转化和发布统计 | `38-learning-assets-statistics.png` |

## 内容运营

| 页面 | 地址 | 主要能力 | 截图 |
| --- | --- | --- | --- |
| 社区分类 | `/community/categories` | 分类管理 | `39-community-categories.png` |
| 社区标签 | `/community/tags` | 标签管理 | `40-community-tags.png` |
| 社区帖子 | `/community/posts` | 帖子审核和置顶下架 | `12-community-posts.png` |
| 社区评论 | `/community/comments` | 评论查询和删除 | `13-community-comments.png` |
| 社区用户 | `/community/users` | 用户状态管理 | `41-community-users.png` |
| 动态列表 | `/moments/list` | 动态管理 | `14-moments-list.png` |
| 动态评论 | `/moments/comments` | 评论管理 | `42-moments-comments.png` |
| 动态统计 | `/moments/statistics` | 活跃和互动统计 | `43-moments-statistics.png` |
| 博客文章 | `/blog/articles` | 文章管理 | `27-blog-articles.png` |
| 博客分类 | `/blog/categories` | 分类管理 | `46-blog-categories.png` |
| 博客标签 | `/blog/tags` | 标签管理 | `47-blog-tags.png` |
| 代码作品 | `/codepen/pens` | 作品管理 | `24-codepen-pens.png` |
| 代码模板 | `/codepen/templates` | 模板管理 | `55-codepen-templates.png` |
| 代码标签 | `/codepen/tags` | 标签管理 | `56-codepen-tags.png` |
| 代码统计 | `/codepen/statistics` | 作品数据 | `57-codepen-statistics.png` |
| 简历模板 | `/resume/templates` | 模板管理 | `08`、`69` |
| 简历数据总览 | `/resume/analytics` | 简历统计 | `09-resume-analytics.png` |
| 简历健康巡检 | `/resume/reports` | 巡检报告 | `37-resume-reports.png` |

## 平台运营

| 页面 | 地址 | 主要能力 | 截图 |
| --- | --- | --- | --- |
| 聊天消息 | `/chat/messages` | 消息查询、删除、公告 | `15`、`71`、`75`、`79` |
| 在线用户 | `/chat/users` | 在线用户、禁言、踢出 | `44`、`70`、`76`、`77` |
| 通知管理 | `/notification` | 通知、模板、配置 | `17-notification.png` |
| 敏感词词库 | `/sensitive/words` | 词库维护 | `18-sensitive-words.png` |
| 敏感词统计 | `/sensitive/statistics` | 命中统计 | `19-sensitive-statistics.png` |
| 敏感词白名单 | `/sensitive/whitelist` | 白名单 | `48-sensitive-whitelist.png` |
| 敏感词策略 | `/sensitive/strategy` | 策略配置 | `49-sensitive-strategy.png` |
| 敏感词来源 | `/sensitive/source` | 词库来源 | `50-sensitive-source.png` |
| 敏感词版本 | `/sensitive/version` | 版本记录 | `51-sensitive-version.png` |
| 敏感词配置 | `/sensitive/config` | 风控配置 | `52-sensitive-config.png` |
| 存储配置 | `/filestorage/storage-config` | 存储策略和健康 | `20-filestorage-storage-config.png` |
| 文件管理 | `/filestorage/file-management` | 文件列表和访问 | `21-filestorage-file-management.png` |
| 文件迁移 | `/filestorage/migration` | 迁移任务 | `53-filestorage-migration.png` |
| 系统设置 | `/filestorage/system-settings` | 文件类型和系统参数 | `54-filestorage-system-settings.png` |
| AI 配置与观测 | `/system/ai-config` | Prompt、RAG、Schema、回归、指标 | `22-system-ai-config.png` |
| AI 治理 | `/system/ai-governance` | 质量治理概览 | 截图待补 |
| 版本管理 | `/system/version` | 版本历史维护 | `23-system-version.png` |
| 积分概览 | `/points/index` | 积分统计 | `26-points-index.png` |
| 积分排行 | `/points/users` | 用户积分排行 | `61-points-users.png` |
| 积分明细 | `/points/details` | 积分流水 | `62-points-details.png` |
| 积分发放 | `/points/grant` | 后台发放积分 | `63`、`67`、`68` |
| 抽奖管理 | `/lottery` | 奖品、库存、记录、风控 | `28-lottery.png` |
| 程序员日历 | `/moyu/calendar-events` | 事件维护 | `25-moyu-calendar-events.png` |
| 每日内容 | `/moyu/daily-content` | 内容维护 | `58-moyu-daily-content.png` |
| 摸鱼统计 | `/moyu/statistics` | 工具统计 | `59-moyu-statistics.png` |
| Bug 商店 | `/moyu/bug-store` | Bug 数据维护 | `60-moyu-bug-store.png` |

## 运营动作检查

1. 写操作提交后要确认列表回流。
2. 会影响用户端实时状态的动作要同时检查用户端页面，例如聊天公告、禁言、踢出、学习资产审核结果。
3. 文件存储、AI RAG、OJ 判题都依赖外部或本地附属服务，页面可访问不等于完整链路可用。
4. 后续新增管理页时，同步更新 [前端路由索引](/reference/frontend-routes)、[API 路由索引](/reference/api-routes) 和本页。
