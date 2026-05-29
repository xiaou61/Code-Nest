# Code-Nest 本地全功能截图测试与操作手册（2026-04-24）

## 1. 文档目的

这份手册用于在本地环境下启动 `Code-Nest`，验证用户端与管理端主要功能页面，并把截图、入口地址、账号信息、已知问题统一沉淀下来，方便后续继续补测、复测和交接。

本轮已经完成：

- 本地后端、用户端、管理端拉起
- 用户端注册与登录链路验证
- 管理端登录链路验证
- 199 张本地页面截图落盘（用户端 118 张，管理端 81 张）
- 用户端/管理端本地入口索引整理

截图目录：

- `docs/manual-assets/2026-04-24/user`
- `docs/manual-assets/2026-04-24/admin`

## 2. 本地环境信息

### 2.1 本地访问地址

- 后端 API：`http://localhost:9999/api`
- Swagger：`http://localhost:9999/api/swagger-ui.html`
- 用户端：`http://localhost:3001`
- 管理端：`http://localhost:3000`

### 2.2 本地服务依赖

- MySQL：`127.0.0.1:3306`
- Redis：`127.0.0.1:6379`
- Java：JDK 17
- Maven：3.9.x
- Node.js：24.x

### 2.3 当前使用配置

后端开发配置来自：

- `xiaou-application/src/main/resources/application.yml`
- `xiaou-application/src/main/resources/application-dev.yml`
- `xiaou-application/src/main/resources/application-sec.yml`

当前开发库配置：

- 数据库：`code_nest`
- MySQL 用户名：`root`
- MySQL 密码：`1234`

## 3. 本地启动步骤

### 3.1 数据库初始化

如果你的本地还没有导入数据，按 README 初始化：

```sql
CREATE DATABASE code_nest DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE code_nest;
SOURCE sql/MySql/code_nest.sql;
SOURCE sql/MySql/code_nest_data.sql;
```

### 3.2 编译后端

当前仓库在这个环境下，直接执行 `spring-boot:run` 不稳定。实测可用路径是先把所有 `xiaou-*` 模块安装到本地 Maven 仓库，再启动 fat jar：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -pl xiaou-application -am install -DskipTests
```

### 3.3 启动后端

```powershell
& 'C:\Program Files\Java\jdk-17\bin\java.exe' `
  -Dfile.encoding=UTF-8 `
  -jar .\xiaou-application\target\xiaou-application-1.8.2.jar `
  --spring.profiles.active=dev
```

### 3.4 启动用户端

```powershell
cd .\vue3-user-front
npm run dev2
```

### 3.5 启动管理端

```powershell
cd .\vue3-admin-front
npm run dev
```

### 3.6 启动 AI RAG sidecar（按需）

如果你要测试 AI 配置页中的 RAG 相关能力，或者模拟面试 / SQL 优化中的知识库召回，需要额外启动 sidecar：

```powershell
.\scripts\ai\start-llamaindex-service.ps1 -ApiKey "your-rag-key"
```

或者：

```powershell
.\scripts\ai\start-ai-dev-stack.ps1 -RagApiKey "your-rag-key" -ImportSampleKnowledge
```

## 4. 测试账号

### 4.1 管理端

- 地址：`http://localhost:3000/login`
- 账号：`admin`
- 密码：`123456`

管理端登录页已预填默认账号密码，直接点登录即可。

### 4.2 用户端

- 地址：`http://localhost:3001/login`
- 本轮新建测试账号：`codexuser0424`
- 密码：`CodeNest123`

### 4.3 用户端验证码说明

用户端注册/登录带验证码。当前后端会把验证码明文打到运行日志里，可以直接从日志中匹配：

- 日志文件：`.tmp/manual/backend-run.out.log`
- 关键词：`生成验证码成功`

示例日志格式：

```text
生成验证码成功，key: xxxxx, code: AbCd
```

也可以先从浏览器网络面板拿到 `captchaKey`，再去日志里按 `key` 搜对应 `code`。

## 5. 用户端已截图验证页面

### 5.1 登录与启动入口

| 页面 | 本地地址 | 截图 |
| --- | --- | --- |
| 登录 | `http://localhost:3001/login` | `manual-assets/2026-04-24/user/00-login.png` |
| 注册 | `http://localhost:3001/register` | `manual-assets/2026-04-24/user/00-register.png` |
| 首页 | `http://localhost:3001/` | `manual-assets/2026-04-24/user/01-home.png` |

### 5.2 学习与成长相关

| 页面 | 本地地址 | 截图 |
| --- | --- | --- |
| 面试题库 | `http://localhost:3001/interview` | `manual-assets/2026-04-24/user/02-interview.png` |
| 在线判题 | `http://localhost:3001/oj` | `manual-assets/2026-04-24/user/03-oj.png` |
| AI 模拟面试 | `http://localhost:3001/mock-interview` | `manual-assets/2026-04-24/user/04-mock-interview.png` |
| 求职作战台 | `http://localhost:3001/job-battle` | `manual-assets/2026-04-24/user/05-job-battle.png` |
| 求职闭环中台 | `http://localhost:3001/career-loop` | `manual-assets/2026-04-24/user/06-career-loop.png` |
| 学习成长驾驶舱 | `http://localhost:3001/learning-cockpit` | `manual-assets/2026-04-24/user/07-learning-cockpit.png` |
| 我的学习资产 | `http://localhost:3001/learning-assets` | `manual-assets/2026-04-24/user/08-learning-assets.png` |
| SQL 优化工作台 | `http://localhost:3001/sql-optimizer/workbench` | `manual-assets/2026-04-24/user/09-sql-optimizer.png` |
| OJ 练习场 | `http://localhost:3001/oj/playground` | `manual-assets/2026-04-24/user/27-oj-playground.png` |
| 模拟面试历史 | `http://localhost:3001/mock-interview/history` | `manual-assets/2026-04-24/user/28-mock-interview-history.png` |
| 计划打卡 | `http://localhost:3001/plan` | `manual-assets/2026-04-24/user/15-plan.png` |
| 知识图谱 | `http://localhost:3001/knowledge` | `manual-assets/2026-04-24/user/18-knowledge.png` |
| 幸运抽奖 | `http://localhost:3001/lottery` | `manual-assets/2026-04-24/user/17-lottery.png` |
| 我的积分 | `http://localhost:3001/points` | `manual-assets/2026-04-24/user/14-points.png` |

### 5.3 创作与内容相关

| 页面 | 本地地址 | 截图 |
| --- | --- | --- |
| 简历管理 | `http://localhost:3001/resume` | `manual-assets/2026-04-24/user/10-resume.png` |
| 简历模板 | `http://localhost:3001/resume/templates` | `manual-assets/2026-04-24/user/29-resume-templates.png` |
| 技术社区 | `http://localhost:3001/community` | `manual-assets/2026-04-24/user/11-community.png` |
| 创作帖子 | `http://localhost:3001/community/create` | `manual-assets/2026-04-24/user/30-community-create.png` |
| 我的博客 | `http://localhost:3001/blog` | `manual-assets/2026-04-24/user/19-blog.png` |
| 博客编辑器 | `http://localhost:3001/blog/editor` | `manual-assets/2026-04-24/user/31-blog-editor.png` |
| 代码广场 | `http://localhost:3001/codepen` | `manual-assets/2026-04-24/user/20-codepen.png` |
| 代码编辑器 | `http://localhost:3001/codepen/editor` | `manual-assets/2026-04-24/user/32-codepen-editor.png` |
| 闪卡记忆 | `http://localhost:3001/flashcard` | `manual-assets/2026-04-24/user/21-flashcard.png` |
| 我的卡组 | `http://localhost:3001/flashcard/my` | `manual-assets/2026-04-24/user/33-flashcard-my.png` |

### 5.4 社区互动与工具相关

| 页面 | 本地地址 | 截图 |
| --- | --- | --- |
| 朋友圈 | `http://localhost:3001/moments` | `manual-assets/2026-04-24/user/12-moments.png` |
| 聊天室 | `http://localhost:3001/chat` | `manual-assets/2026-04-24/user/13-chat.png` |
| 通知中心 | `http://localhost:3001/notification` | `manual-assets/2026-04-24/user/22-notification.png` |
| 小组广场 | `http://localhost:3001/team` | `manual-assets/2026-04-24/user/16-team.png` |
| 程序员工具 | `http://localhost:3001/dev-tools` | `manual-assets/2026-04-24/user/24-dev-tools.png` |
| JSON 工具 | `http://localhost:3001/dev-tools/json` | `manual-assets/2026-04-24/user/34-dev-tools-json.png` |
| 摸鱼工具 | `http://localhost:3001/moyu-tools` | `manual-assets/2026-04-24/user/25-moyu-tools.png` |
| 今日热榜 | `http://localhost:3001/moyu-tools/hot-topics` | `manual-assets/2026-04-24/user/35-moyu-hot-topics.png` |
| 版本更新历史 | `http://localhost:3001/version-history` | `manual-assets/2026-04-24/user/26-version-history.png` |
| 个人中心 | `http://localhost:3001/profile` | `manual-assets/2026-04-24/user/23-profile.png` |

### 5.5 第二轮深层详情页与真实交互

| 页面 | 本地地址 | 截图 |
| --- | --- | --- |
| 题单详情 | `http://localhost:3001/interview/question-sets/2` | `manual-assets/2026-04-24/user/36-interview-question-set-detail.png` |
| 题目详情 | `http://localhost:3001/interview/questions/2/81` | `manual-assets/2026-04-24/user/37-interview-question-detail.png` |
| 题目答案展开 | `http://localhost:3001/interview/questions/2/81` | `manual-assets/2026-04-24/user/38-interview-question-answer.png` |
| 题目掌握度已提交 | `http://localhost:3001/interview/questions/2/81` | `manual-assets/2026-04-24/user/39-interview-mastery-marked.png` |
| 下一题 | `http://localhost:3001/interview/questions/2/82` | `manual-assets/2026-04-24/user/40-interview-question-next.png` |
| 返回题单后列表状态 | `http://localhost:3001/interview/question-sets/2` | `manual-assets/2026-04-24/user/41-interview-question-set-back.png` |
| OJ 题目详情 | `http://localhost:3001/oj/problem/1` | `manual-assets/2026-04-24/user/42-oj-problem-detail.png` |
| 我的 OJ 提交 | `http://localhost:3001/oj/my-submissions` | `manual-assets/2026-04-24/user/43-oj-my-submissions.png` |
| OJ 提交详情 | `http://localhost:3001/oj/submission/6` | `manual-assets/2026-04-24/user/44-oj-submission-detail.png` |
| OJ 赛事详情 | `http://localhost:3001/oj/contests/1` | `manual-assets/2026-04-24/user/45-oj-contest-detail.png` |
| 赛事题目详情 | `http://localhost:3001/oj/problem/8?contestId=1` | `manual-assets/2026-04-24/user/46-oj-contest-problem-detail.png` |
| 社区帖子详情 | `http://localhost:3001/community/posts/172` | `manual-assets/2026-04-24/user/47-community-post-detail.png` |
| 博客文章详情 | `http://localhost:3001/blog/3/article/1` | `manual-assets/2026-04-24/user/48-blog-article-detail.png` |
| CodePen 作品详情（已点赞） | `http://localhost:3001/codepen/1` | `manual-assets/2026-04-24/user/49-codepen-detail-liked.png` |
| 闪卡卡组详情 | `http://localhost:3001/flashcard/deck/2` | `manual-assets/2026-04-24/user/50-flashcard-deck-detail.png` |
| 闪卡学习完成页 | `http://localhost:3001/flashcard/study/2` | `manual-assets/2026-04-24/user/51-flashcard-study-complete.png` |
| 小组详情（已加入） | `http://localhost:3001/team/1` | `manual-assets/2026-04-24/user/52-team-detail-joined.png` |

### 5.6 第三轮补充覆盖页

| 页面 | 本地地址 | 截图 |
| --- | --- | --- |
| 随机抽题 | `http://localhost:3001/interview/random` | `manual-assets/2026-04-24/user/53-interview-random.png` |
| 面试收藏 | `http://localhost:3001/interview/favorites` | `manual-assets/2026-04-24/user/54-interview-favorites.png` |
| 智能复习 | `http://localhost:3001/interview/review` | `manual-assets/2026-04-24/user/55-interview-review.png` |
| OJ 做题统计 | `http://localhost:3001/oj/statistics` | `manual-assets/2026-04-24/user/56-oj-statistics.png` |
| OJ 排行榜 | `http://localhost:3001/oj/ranking` | `manual-assets/2026-04-24/user/57-oj-ranking.png` |
| 岗位匹配引擎 2.0 | `http://localhost:3001/job-match-engine` | `manual-assets/2026-04-24/user/58-job-match-engine.png` |
| 简历编辑器（新建） | `http://localhost:3001/resume/editor` | `manual-assets/2026-04-24/user/59-resume-editor.png` |
| 社区收藏 | `http://localhost:3001/community/collections` | `manual-assets/2026-04-24/user/60-community-collections.png` |
| 我的帖子 | `http://localhost:3001/community/my-posts` | `manual-assets/2026-04-24/user/61-community-my-posts.png` |
| 社区用户主页 | `http://localhost:3001/community/users/3` | `manual-assets/2026-04-24/user/62-community-user-profile.png` |
| 朋友圈用户主页 | `http://localhost:3001/moments/user/3` | `manual-assets/2026-04-24/user/63-moments-user-profile.png` |
| 朋友圈我的收藏 | `http://localhost:3001/moments/my-favorites` | `manual-assets/2026-04-24/user/64-moments-my-favorites.png` |
| 文本比对工具 | `http://localhost:3001/dev-tools/text-diff` | `manual-assets/2026-04-24/user/65-dev-tools-text-diff.png` |
| 聚合翻译工具 | `http://localhost:3001/dev-tools/translation` | `manual-assets/2026-04-24/user/66-dev-tools-translation.png` |
| 时薪计算器 | `http://localhost:3001/moyu-tools/salary-calculator` | `manual-assets/2026-04-24/user/67-moyu-salary-calculator.png` |
| 程序员日历 | `http://localhost:3001/moyu-tools/calendar` | `manual-assets/2026-04-24/user/68-moyu-calendar.png` |
| 每日内容 | `http://localhost:3001/moyu-tools/daily-content` | `manual-assets/2026-04-24/user/69-moyu-daily-content.png` |
| Bug 商店 | `http://localhost:3001/moyu-tools/bug-store` | `manual-assets/2026-04-24/user/70-moyu-bug-store.png` |
| 博客主页 | `http://localhost:3001/blog/3` | `manual-assets/2026-04-24/user/71-blog-home-public.png` |
| 我的 CodePen 作品 | `http://localhost:3001/codepen/my` | `manual-assets/2026-04-24/user/72-codepen-my.png` |
| 闪卡管理页 | `http://localhost:3001/flashcard/deck/2/cards` | `manual-assets/2026-04-24/user/73-flashcard-card-editor.png` |
| 闪卡卡组编辑 | `http://localhost:3001/flashcard/deck/2/edit` | `manual-assets/2026-04-24/user/74-flashcard-deck-edit.png` |
| 我的小组 | `http://localhost:3001/team/my` | `manual-assets/2026-04-24/user/75-team-my.png` |
| 知识图谱查看页 | `http://localhost:3001/knowledge/maps/1` | `manual-assets/2026-04-24/user/76-knowledge-map-viewer.png` |
| 模拟面试会话路由直达结果 | `http://localhost:3001/mock-interview/session` | `manual-assets/2026-04-24/user/77-mock-interview-session.png` |
| 模拟面试报告路由直达结果 | `http://localhost:3001/mock-interview/report` | `manual-assets/2026-04-24/user/78-mock-interview-report.png` |
| 成长自动驾驶舱路由结果 | `http://localhost:3001/growth-autopilot` | `manual-assets/2026-04-24/user/79-growth-autopilot.png` |
| 模拟面试真实起始流程 | `http://localhost:3001/mock-interview/config?direction=java` | `manual-assets/2026-04-24/user/80-mock-interview-start-flow.png` |
| 模拟面试开始按钮当前状态 | `http://localhost:3001/mock-interview/config?direction=java` | `manual-assets/2026-04-24/user/81-mock-interview-session-active.png` |
| OJ 赛事中心列表 | `http://localhost:3001/oj/contests` | `manual-assets/2026-04-24/user/82-oj-contests-list.png` |
| 小组创建页 | `http://localhost:3001/team/create` | `manual-assets/2026-04-24/user/83-team-create.png` |
| 闪卡创建卡组页 | `http://localhost:3001/flashcard/deck/create` | `manual-assets/2026-04-24/user/84-flashcard-deck-create.png` |
| 今日闪卡学习 | `http://localhost:3001/flashcard/study` | `manual-assets/2026-04-24/user/85-flashcard-study-root.png` |

### 5.7 第四轮真实提交链路

| 页面 | 本地地址 | 截图 |
| --- | --- | --- |
| 积分页（签到后状态） | `http://localhost:3001/points` | `manual-assets/2026-04-24/user/86-points-checkin-after-api.png` |
| 博客已开通 | `http://localhost:3001/blog` | `manual-assets/2026-04-24/user/87-blog-opened.png` |
| 博客文章已发布 | `http://localhost:3001/blog` | `manual-assets/2026-04-24/user/88-blog-article-published.png` |
| 新博客文章详情 | `http://localhost:3001/blog/5/article/2` | `manual-assets/2026-04-24/user/89-blog-article-detail-created.png` |
| 抽奖结果弹窗 | `http://localhost:3001/lottery` | `manual-assets/2026-04-24/user/90-lottery-draw-result.png` |
| 社区新帖已回流 | `http://localhost:3001/community/my-posts` | `manual-assets/2026-04-24/user/91-community-post-created.png` |
| 社区帖子评论已发布 | `http://localhost:3001/community/posts/173` | `manual-assets/2026-04-24/user/92-community-post-commented.png` |
| 朋友圈新动态已发布 | `http://localhost:3001/moments` | `manual-assets/2026-04-24/user/93-moments-published.png` |
| 朋友圈点赞/收藏/评论后状态 | `http://localhost:3001/moments` | `manual-assets/2026-04-24/user/94-moments-interacted.png` |
| 简历保存回流列表 | `http://localhost:3001/resume` | `manual-assets/2026-04-24/user/95-resume-saved.png` |
| 我的 CodePen 作品回流 | `http://localhost:3001/codepen/my` | `manual-assets/2026-04-24/user/96-codepen-published.png` |
| 新 CodePen 作品详情 | `http://localhost:3001/codepen/2` | `manual-assets/2026-04-24/user/97-codepen-detail-created.png` |
| 小组讨论已发布 | `http://localhost:3001/team/1` | `manual-assets/2026-04-24/user/98-team-discussion-posted.png` |

### 5.8 第五轮模拟面试与聊天室闭环

| 页面 | 本地地址 | 截图 |
| --- | --- | --- |
| 模拟面试真实会话页 | `http://localhost:3001/mock-interview/session?sessionId=15` | `manual-assets/2026-04-24/user/99-mock-interview-session-live.png` |
| 模拟面试答题反馈 | `http://localhost:3001/mock-interview/session?sessionId=15` | `manual-assets/2026-04-24/user/100-mock-interview-answer-feedback.png` |
| 模拟面试完成态 | `http://localhost:3001/mock-interview/session?sessionId=16` | `manual-assets/2026-04-24/user/101-mock-interview-finished.png` |
| 模拟面试报告生成态 | `http://localhost:3001/mock-interview/report?sessionId=16` | `manual-assets/2026-04-24/user/102-mock-interview-report-generated.png` |
| 模拟面试历史回流 | `http://localhost:3001/mock-interview/history` | `manual-assets/2026-04-24/user/103-mock-interview-history-completed.png` |
| 聊天室连接成功 | `http://localhost:3001/chat` | `manual-assets/2026-04-24/user/104-chat-connected.png` |
| 聊天室消息已发送 | `http://localhost:3001/chat` | `manual-assets/2026-04-24/user/105-chat-message-sent.png` |
| 模拟面试 AI 总结已生成 | `http://localhost:3001/mock-interview/report?sessionId=16` | `manual-assets/2026-04-24/user/106-mock-interview-summary-generated.png` |

### 5.9 第六轮学习资产转化与聊天室运营补测

| 页面 | 本地地址 | 截图 |
| --- | --- | --- |
| 模拟面试转学习资产弹窗 | `http://localhost:3001/mock-interview/report?sessionId=16` | `manual-assets/2026-04-24/user/107-mock-interview-transform-dialog.png` |
| 学习资产记录已生成 | `http://localhost:3001/learning-assets` | `manual-assets/2026-04-24/user/108-learning-assets-record-created.png` |
| 学习资产部分发布成功 | `http://localhost:3001/learning-assets` | `manual-assets/2026-04-24/user/109-learning-assets-published.png` |
| 聊天室收到系统公告 | `http://localhost:3001/chat` | `manual-assets/2026-04-24/user/110-chat-announcement-received.png` |
| 聊天室撤回前消息状态 | `http://localhost:3001/chat` | `manual-assets/2026-04-24/user/111-chat-message-before-recall.png` |
| 聊天室撤回成功提示 | `http://localhost:3001/chat` | `manual-assets/2026-04-24/user/112-chat-recall-succeeded.png` |
| 聊天室禁言后发送失败 | `http://localhost:3001/chat` | `manual-assets/2026-04-24/user/113-chat-banned-send-failed.png` |
| 聊天室被踢出后的断开态 | `http://localhost:3001/chat` | `manual-assets/2026-04-24/user/114-chat-kicked-out.png` |

### 5.10 第七轮聊天室图片消息与学习资产终态补测

| 页面 | 本地地址 | 截图 |
| --- | --- | --- |
| 聊天室图片消息已发送 | `http://localhost:3001/chat` | `manual-assets/2026-04-24/user/115-chat-image-uploaded.png` |
| 我的学习资产（审核动作完成后） | `http://localhost:3001/learning-assets` | `manual-assets/2026-04-24/user/116-learning-assets-after-review-updated.png` |

## 6. 管理端已截图验证页面

| 页面 | 本地地址 | 截图 |
| --- | --- | --- |
| 登录 | `http://localhost:3000/login` | `manual-assets/2026-04-24/admin/00-login.png` |
| 仪表板 | `http://localhost:3000/dashboard` | `manual-assets/2026-04-24/admin/01-dashboard.png` |
| 用户管理 | `http://localhost:3000/user` | `manual-assets/2026-04-24/admin/02-user.png` |
| 面试分类管理 | `http://localhost:3000/interview/categories` | `manual-assets/2026-04-24/admin/03-interview-categories.png` |
| 面试题目管理 | `http://localhost:3000/interview/questions` | `manual-assets/2026-04-24/admin/04-interview-questions.png` |
| 模拟面试会话 | `http://localhost:3000/mock-interview/sessions` | `manual-assets/2026-04-24/admin/05-mock-interview-sessions.png` |
| OJ 题目管理 | `http://localhost:3000/oj/problems` | `manual-assets/2026-04-24/admin/06-oj-problems.png` |
| OJ 赛事管理 | `http://localhost:3000/oj/contests` | `manual-assets/2026-04-24/admin/07-oj-contests.png` |
| 简历模板管理 | `http://localhost:3000/resume/templates` | `manual-assets/2026-04-24/admin/08-resume-templates.png` |
| 简历数据总览 | `http://localhost:3000/resume/analytics` | `manual-assets/2026-04-24/admin/09-resume-analytics.png` |
| 知识图谱管理 | `http://localhost:3000/knowledge/maps` | `manual-assets/2026-04-24/admin/10-knowledge-maps.png` |
| 学习资产审核台 | `http://localhost:3000/learning-assets/review` | `manual-assets/2026-04-24/admin/11-learning-assets-review.png` |
| 社区帖子管理 | `http://localhost:3000/community/posts` | `manual-assets/2026-04-24/admin/12-community-posts.png` |
| 社区评论管理 | `http://localhost:3000/community/comments` | `manual-assets/2026-04-24/admin/13-community-comments.png` |
| 朋友圈动态管理 | `http://localhost:3000/moments/list` | `manual-assets/2026-04-24/admin/14-moments-list.png` |
| 聊天消息管理 | `http://localhost:3000/chat/messages` | `manual-assets/2026-04-24/admin/15-chat-messages.png` |
| 登录日志 | `http://localhost:3000/logs/login` | `manual-assets/2026-04-24/admin/16-logs-login.png` |
| 通知管理 | `http://localhost:3000/notification` | `manual-assets/2026-04-24/admin/17-notification.png` |
| 敏感词词库 | `http://localhost:3000/sensitive/words` | `manual-assets/2026-04-24/admin/18-sensitive-words.png` |
| 敏感词统计 | `http://localhost:3000/sensitive/statistics` | `manual-assets/2026-04-24/admin/19-sensitive-statistics.png` |
| 文件存储配置 | `http://localhost:3000/filestorage/storage-config` | `manual-assets/2026-04-24/admin/20-filestorage-storage-config.png` |
| 文件管理 | `http://localhost:3000/filestorage/file-management` | `manual-assets/2026-04-24/admin/21-filestorage-file-management.png` |
| AI 配置与观测 | `http://localhost:3000/system/ai-config` | `manual-assets/2026-04-24/admin/22-system-ai-config.png` |
| 版本管理 | `http://localhost:3000/system/version` | `manual-assets/2026-04-24/admin/23-system-version.png` |
| CodePen 作品管理 | `http://localhost:3000/codepen/pens` | `manual-assets/2026-04-24/admin/24-codepen-pens.png` |
| 摸鱼日历事件管理 | `http://localhost:3000/moyu/calendar-events` | `manual-assets/2026-04-24/admin/25-moyu-calendar-events.png` |
| 积分概览 | `http://localhost:3000/points/index` | `manual-assets/2026-04-24/admin/26-points-index.png` |
| 博客文章管理 | `http://localhost:3000/blog/articles` | `manual-assets/2026-04-24/admin/27-blog-articles.png` |
| 抽奖管理 | `http://localhost:3000/lottery` | `manual-assets/2026-04-24/admin/28-lottery.png` |
| OJ 新增题目 | `http://localhost:3000/oj/problems/create` | `manual-assets/2026-04-24/admin/29-oj-problem-create.png` |
| OJ 新增赛事 | `http://localhost:3000/oj/contests/create` | `manual-assets/2026-04-24/admin/30-oj-contest-create.png` |
| OJ 题目编辑（测试用例弹窗） | `http://localhost:3000/oj/problems/1/edit` | `manual-assets/2026-04-24/admin/31-oj-problem-edit-with-testcase-modal.png` |
| OJ 赛事编辑 | `http://localhost:3000/oj/contests/1/edit` | `manual-assets/2026-04-24/admin/32-oj-contest-edit.png` |
| 知识图谱编辑（节点面板） | `http://localhost:3000/knowledge/maps/1/edit` | `manual-assets/2026-04-24/admin/33-knowledge-map-edit-node-panel.png` |
| 面试题单管理 | `http://localhost:3000/interview/question-sets` | `manual-assets/2026-04-24/admin/34-interview-question-sets.png` |
| 模拟面试方向配置 | `http://localhost:3000/mock-interview/directions` | `manual-assets/2026-04-24/admin/35-mock-interview-directions.png` |
| OJ 标签管理 | `http://localhost:3000/oj/tags` | `manual-assets/2026-04-24/admin/36-oj-tags.png` |
| 简历健康巡检 | `http://localhost:3000/resume/reports` | `manual-assets/2026-04-24/admin/37-resume-reports.png` |
| 学习资产统计 | `http://localhost:3000/learning-assets/statistics` | `manual-assets/2026-04-24/admin/38-learning-assets-statistics.png` |
| 社区分类管理 | `http://localhost:3000/community/categories` | `manual-assets/2026-04-24/admin/39-community-categories.png` |
| 社区标签管理 | `http://localhost:3000/community/tags` | `manual-assets/2026-04-24/admin/40-community-tags.png` |
| 社区用户管理 | `http://localhost:3000/community/users` | `manual-assets/2026-04-24/admin/41-community-users.png` |
| 朋友圈评论管理 | `http://localhost:3000/moments/comments` | `manual-assets/2026-04-24/admin/42-moments-comments.png` |
| 朋友圈统计 | `http://localhost:3000/moments/statistics` | `manual-assets/2026-04-24/admin/43-moments-statistics.png` |
| 聊天在线用户 | `http://localhost:3000/chat/users` | `manual-assets/2026-04-24/admin/44-chat-users.png` |
| 操作日志 | `http://localhost:3000/logs/operation` | `manual-assets/2026-04-24/admin/45-logs-operation.png` |
| 博客分类管理 | `http://localhost:3000/blog/categories` | `manual-assets/2026-04-24/admin/46-blog-categories.png` |
| 博客标签管理 | `http://localhost:3000/blog/tags` | `manual-assets/2026-04-24/admin/47-blog-tags.png` |
| 敏感词白名单 | `http://localhost:3000/sensitive/whitelist` | `manual-assets/2026-04-24/admin/48-sensitive-whitelist.png` |
| 敏感词策略配置 | `http://localhost:3000/sensitive/strategy` | `manual-assets/2026-04-24/admin/49-sensitive-strategy.png` |
| 敏感词来源管理 | `http://localhost:3000/sensitive/source` | `manual-assets/2026-04-24/admin/50-sensitive-source.png` |
| 敏感词版本历史 | `http://localhost:3000/sensitive/version` | `manual-assets/2026-04-24/admin/51-sensitive-version.png` |
| 敏感词配置管理 | `http://localhost:3000/sensitive/config` | `manual-assets/2026-04-24/admin/52-sensitive-config.png` |
| 文件迁移 | `http://localhost:3000/filestorage/migration` | `manual-assets/2026-04-24/admin/53-filestorage-migration.png` |
| 文件存储系统设置 | `http://localhost:3000/filestorage/system-settings` | `manual-assets/2026-04-24/admin/54-filestorage-system-settings.png` |
| CodePen 模板管理 | `http://localhost:3000/codepen/templates` | `manual-assets/2026-04-24/admin/55-codepen-templates.png` |
| CodePen 标签管理 | `http://localhost:3000/codepen/tags` | `manual-assets/2026-04-24/admin/56-codepen-tags.png` |
| CodePen 统计 | `http://localhost:3000/codepen/statistics` | `manual-assets/2026-04-24/admin/57-codepen-statistics.png` |
| 摸鱼每日内容管理 | `http://localhost:3000/moyu/daily-content` | `manual-assets/2026-04-24/admin/58-moyu-daily-content.png` |
| 摸鱼统计分析 | `http://localhost:3000/moyu/statistics` | `manual-assets/2026-04-24/admin/59-moyu-statistics.png` |
| 摸鱼 Bug 商店管理 | `http://localhost:3000/moyu/bug-store` | `manual-assets/2026-04-24/admin/60-moyu-bug-store.png` |
| 积分排行 | `http://localhost:3000/points/users` | `manual-assets/2026-04-24/admin/61-points-users.png` |
| 积分明细 | `http://localhost:3000/points/details` | `manual-assets/2026-04-24/admin/62-points-details.png` |
| 积分发放 | `http://localhost:3000/points/grant` | `manual-assets/2026-04-24/admin/63-points-grant.png` |
| 管理端个人中心 | `http://localhost:3000/profile/index` | `manual-assets/2026-04-24/admin/64-profile-index.png` |
| 管理端编辑资料 | `http://localhost:3000/profile/edit` | `manual-assets/2026-04-24/admin/65-profile-edit.png` |
| 管理端修改密码 | `http://localhost:3000/profile/change-password` | `manual-assets/2026-04-24/admin/66-profile-change-password.png` |
| 积分发放填写态 | `http://localhost:3000/points/grant` | `manual-assets/2026-04-24/admin/67-points-grant-filled.png` |
| 积分发放成功态 | `http://localhost:3000/points/grant` | `manual-assets/2026-04-24/admin/68-points-grant-success.png` |
| 简历模板列表（已补本地模板） | `http://localhost:3000/resume/templates` | `manual-assets/2026-04-24/admin/69-resume-template-created.png` |
| 聊天室在线用户实时态 | `http://localhost:3000/chat/users` | `manual-assets/2026-04-24/admin/70-chat-online-user-detected.png` |
| 聊天消息回流记录 | `http://localhost:3000/chat/messages` | `manual-assets/2026-04-24/admin/71-chat-message-record-created.png` |
| 模拟面试会话列表（真实会话已入库） | `http://localhost:3000/mock-interview/sessions` | `manual-assets/2026-04-24/admin/72-mock-interview-sessions-created.png` |
| 模拟面试会话详情抽屉 | `http://localhost:3000/mock-interview/sessions` | `manual-assets/2026-04-24/admin/73-mock-interview-session-detail-created.png` |
| 学习资产审核队列（真实待审核数据） | `http://localhost:3000/learning-assets/review` | `manual-assets/2026-04-24/admin/74-learning-assets-review-queued.png` |
| 聊天室系统公告发送弹窗 | `http://localhost:3000/chat/messages` | `manual-assets/2026-04-24/admin/75-chat-announcement-filled.png` |
| 聊天室禁言弹窗填写态 | `http://localhost:3000/chat/users` | `manual-assets/2026-04-24/admin/76-chat-ban-dialog-filled.png` |
| 聊天在线用户禁言成功态 | `http://localhost:3000/chat/users` | `manual-assets/2026-04-24/admin/77-chat-user-banned.png` |
| 学习资产审核详情抽屉 | `http://localhost:3000/learning-assets/review` | `manual-assets/2026-04-24/admin/78-learning-assets-review-detail-opened.png` |
| 聊天图片消息回流记录 | `http://localhost:3000/chat/messages` | `manual-assets/2026-04-24/admin/79-chat-image-message-record.png` |
| 学习资产审核台空状态（处理完成后） | `http://localhost:3000/learning-assets/review` | `manual-assets/2026-04-24/admin/80-learning-assets-review-empty-after-actions.png` |

## 7. 用户端全量本地入口索引

以下是用户端路由层的本地入口索引。带 `:id`、`:userId`、`:deckId`、`:questionId` 的页面需要先从对应列表页点进去。

### 7.1 核心业务

- `http://localhost:3001/`
- `http://localhost:3001/interview`
- `http://localhost:3001/interview/random`
- `http://localhost:3001/interview/favorites`
- `http://localhost:3001/interview/review`
- `http://localhost:3001/oj`
- `http://localhost:3001/oj/contests`
- `http://localhost:3001/oj/my-submissions`
- `http://localhost:3001/oj/statistics`
- `http://localhost:3001/oj/playground`
- `http://localhost:3001/oj/ranking`
- `http://localhost:3001/mock-interview`
- `http://localhost:3001/mock-interview/config`
- `http://localhost:3001/mock-interview/session`
- `http://localhost:3001/mock-interview/report`
- `http://localhost:3001/mock-interview/history`
- `http://localhost:3001/job-battle`
- `http://localhost:3001/job-match-engine`
- `http://localhost:3001/career-loop`
- `http://localhost:3001/learning-cockpit`
- `http://localhost:3001/learning-assets`
- `http://localhost:3001/sql-optimizer/workbench`
- `http://localhost:3001/growth-autopilot`

### 7.2 创作与内容

- `http://localhost:3001/resume`
- `http://localhost:3001/resume/templates`
- `http://localhost:3001/resume/editor`
- `http://localhost:3001/community`
- `http://localhost:3001/community/collections`
- `http://localhost:3001/community/my-posts`
- `http://localhost:3001/community/create`
- `http://localhost:3001/blog`
- `http://localhost:3001/blog/editor`
- `http://localhost:3001/codepen`
- `http://localhost:3001/codepen/editor`
- `http://localhost:3001/codepen/my`
- `http://localhost:3001/flashcard`
- `http://localhost:3001/flashcard/study`
- `http://localhost:3001/flashcard/my`
- `http://localhost:3001/flashcard/deck/create`

### 7.3 社区、个人与工具

- `http://localhost:3001/notification`
- `http://localhost:3001/moments`
- `http://localhost:3001/chat`
- `http://localhost:3001/profile`
- `http://localhost:3001/points`
- `http://localhost:3001/plan`
- `http://localhost:3001/team`
- `http://localhost:3001/team/create`
- `http://localhost:3001/team/my`
- `http://localhost:3001/lottery`
- `http://localhost:3001/knowledge`
- `http://localhost:3001/version-history`
- `http://localhost:3001/dev-tools`
- `http://localhost:3001/dev-tools/json`
- `http://localhost:3001/dev-tools/text-diff`
- `http://localhost:3001/dev-tools/translation`
- `http://localhost:3001/moyu-tools`
- `http://localhost:3001/moyu-tools/hot-topics`
- `http://localhost:3001/moyu-tools/salary-calculator`
- `http://localhost:3001/moyu-tools/calendar`
- `http://localhost:3001/moyu-tools/daily-content`
- `http://localhost:3001/moyu-tools/bug-store`

## 8. 管理端全量本地入口索引

### 8.1 首页与基础管理

- `http://localhost:3000/dashboard`
- `http://localhost:3000/user`
- `http://localhost:3000/profile/index`
- `http://localhost:3000/profile/edit`
- `http://localhost:3000/profile/change-password`

### 8.2 内容与运营模块

- `http://localhost:3000/interview/categories`
- `http://localhost:3000/interview/question-sets`
- `http://localhost:3000/interview/questions`
- `http://localhost:3000/mock-interview/sessions`
- `http://localhost:3000/mock-interview/directions`
- `http://localhost:3000/oj/problems`
- `http://localhost:3000/oj/problems/create`
- `http://localhost:3000/oj/contests`
- `http://localhost:3000/oj/contests/create`
- `http://localhost:3000/oj/tags`
- `http://localhost:3000/resume/templates`
- `http://localhost:3000/resume/analytics`
- `http://localhost:3000/resume/reports`
- `http://localhost:3000/knowledge/maps`
- `http://localhost:3000/learning-assets/review`
- `http://localhost:3000/learning-assets/statistics`
- `http://localhost:3000/community/categories`
- `http://localhost:3000/community/tags`
- `http://localhost:3000/community/posts`
- `http://localhost:3000/community/comments`
- `http://localhost:3000/community/users`
- `http://localhost:3000/moments/list`
- `http://localhost:3000/moments/comments`
- `http://localhost:3000/moments/statistics`
- `http://localhost:3000/chat/messages`
- `http://localhost:3000/chat/users`
- `http://localhost:3000/logs/login`
- `http://localhost:3000/logs/operation`
- `http://localhost:3000/notification`
- `http://localhost:3000/blog/articles`
- `http://localhost:3000/blog/categories`
- `http://localhost:3000/blog/tags`
- `http://localhost:3000/lottery`

### 8.3 风控、存储、系统与工具

- `http://localhost:3000/sensitive/words`
- `http://localhost:3000/sensitive/whitelist`
- `http://localhost:3000/sensitive/strategy`
- `http://localhost:3000/sensitive/statistics`
- `http://localhost:3000/sensitive/source`
- `http://localhost:3000/sensitive/version`
- `http://localhost:3000/sensitive/config`
- `http://localhost:3000/filestorage/storage-config`
- `http://localhost:3000/filestorage/file-management`
- `http://localhost:3000/filestorage/migration`
- `http://localhost:3000/filestorage/system-settings`
- `http://localhost:3000/system/ai-config`
- `http://localhost:3000/system/version`
- `http://localhost:3000/codepen/pens`
- `http://localhost:3000/codepen/templates`
- `http://localhost:3000/codepen/tags`
- `http://localhost:3000/codepen/statistics`
- `http://localhost:3000/moyu/calendar-events`
- `http://localhost:3000/moyu/daily-content`
- `http://localhost:3000/moyu/statistics`
- `http://localhost:3000/moyu/bug-store`
- `http://localhost:3000/points/index`
- `http://localhost:3000/points/users`
- `http://localhost:3000/points/details`
- `http://localhost:3000/points/grant`

## 9. 带参数详情页的进入方式

这类页面不建议手填 URL，建议先从列表页进入：

- 题单详情：从 `http://localhost:3001/interview` 点击题单卡片
- 题目学习：从题单详情进入题目
- OJ 题目详情：从 `http://localhost:3001/oj` 点击题目标题
- OJ 提交详情：从 `http://localhost:3001/oj/my-submissions` 点击某次提交
- OJ 赛事详情：从 `http://localhost:3001/oj/contests` 点击赛事卡片
- 社区帖子详情：从 `http://localhost:3001/community` 点击帖子
- 用户主页：从社区 / 朋友圈点击用户名或头像
- 小组详情 / 编辑：从 `http://localhost:3001/team` 或 `http://localhost:3001/team/my` 进入
- 博客主页 / 文章详情：从博客列表进入
- CodePen 作品详情 / 编辑：从 `http://localhost:3001/codepen` 或 `http://localhost:3001/codepen/my` 进入
- 闪卡卡组详情 / 学习 / 编辑 / 管理卡片：从 `http://localhost:3001/flashcard` 或 `http://localhost:3001/flashcard/my` 进入
- 管理端编辑页：统一从列表页点击“编辑”按钮进入，例如 OJ 题目、赛事、知识图谱

## 10. 已发现问题与注意事项

### 10.0 深层验证与真实提交结果

- 面试题库链路已真实打通：`/interview/question-sets/2` 可进入题单详情，`/interview/questions/2/81` 可展开答案、选择掌握程度并提交；浏览器控制台记录 `掌握度已标记`，网络请求 `POST /api/interview/mastery/mark` 返回 `200`。
- 题目切换链路正常：从 `questionId=81` 点击“下一题”进入 `questionId=82`，再点击“返回题单”可以回到题单详情页，已学习标记会在列表中展示。
- OJ 题目详情可正常打开，代码提交链路已走通一次，生成提交记录 `#6`；`/oj/my-submissions` 与 `/oj/submission/6` 均可正常展示。
- OJ 赛事链路已真实打通：`/oj/contests/1` 可查看赛事详情并成功触发报名，网络请求 `POST /api/oj/contests/1/join` 返回 `200`；报名后按钮禁用，赛事成员数从 `1` 变为 `2`。
- 社区、博客、CodePen 深层内容页均可打开：帖子详情 `172`、博客文章 `3/article/1`、作品详情 `1` 已验证，CodePen 点赞后计数从 `0` 增长到 `1`。
- 闪卡公开卡组为空，因此本轮用测试账号创建了私有卡组 `#2` 与闪卡 `#2`，并完成了一次学习提交流程；学习完成页可正常展示。
- 小组链路已真实打通：测试账号成功加入 `team/1`，按钮状态由“加入小组”变为“退出小组”，成员数从 `1` 变为 `2`。
- 管理端第二轮已验证 OJ 新建页、OJ 编辑页、赛事新建页、赛事编辑页、知识图谱编辑页，且题目编辑页中的“测试用例编辑弹窗”与知识图谱节点编辑面板均可正常打开。
- 用户端第三轮已补齐大批剩余直达页：随机抽题、面试收藏、智能复习、OJ 统计/排行榜/赛事中心、岗位匹配、简历编辑、社区收藏/我的帖子/用户主页、朋友圈用户主页/我的收藏、文本比对、聚合翻译、摸鱼工具四个子页、博客主页、CodePen 我的作品、闪卡卡组创建/编辑/卡片管理/今日学习、小组创建/我的小组、知识图谱查看页。
- 模拟面试真实流程补测结果：从 `http://localhost:3001/mock-interview` 选择 `Java 后端` 后，点击“开始 AI 面试”可进入 `http://localhost:3001/mock-interview/config?direction=java`，配置页可正常展示难度、出题模式、题量与面试官风格。
- 直接访问 `http://localhost:3001/mock-interview/session` 与 `http://localhost:3001/mock-interview/report` 时，当前会回落到 `http://localhost:3001/mock-interview`，说明这两个页面依赖已创建的面试会话上下文，不能仅靠手填 URL 进入。
- `http://localhost:3001/growth-autopilot` 当前会跳到 `http://localhost:3001/learning-cockpit?tab=autopilot`，属于驾驶舱页签式入口，不是独立页面。
- 管理端第三轮已补齐绝大多数剩余二级路由，包括：题单管理、方向配置、OJ 标签、简历健康巡检、学习资产统计、社区分类/标签/用户、朋友圈评论/统计、聊天在线用户、操作日志、博客分类/标签、敏感词白名单/策略/来源/版本/配置、文件迁移/系统设置、CodePen 模板/标签/统计、摸鱼每日内容/统计/Bug 商店、积分排行/明细/发放、个人中心/编辑资料/修改密码。
- 第四轮已真实跑通一组“提交型”链路：测试账号先通过 `POST /api/user/points/checkin` 获得 `+50` 积分，再由管理端积分发放页真实提交 `+300` 积分；当前积分余额为 `190`，最近积分明细可看到打卡、后台发放、博客开通、文章发布、抽奖扣分、CodePen 奖励等完整流水。
- 博客链路已从“未开通”推进到真实可用：测试账号成功开通博客，生成 `blogId=2`；随后真实发布文章 `articleId=2`，博客列表和文章详情页均可正常展示。
- 抽奖链路已真实打通：执行一次抽奖后生成记录 `recordId=11`，结果为“未中奖”，并成功扣除 `100` 积分；结果弹窗与抽奖记录表均可正常展示。
- 社区发帖评论链路已真实打通：测试账号发布帖子 `postId=173`，随后在详情页成功发表评论，`/community/my-posts` 能正常回流新帖，帖子评论数已变为 `1`。
- 朋友圈写操作已真实打通：测试账号发布动态 `momentId=6`，随后对该动态完成点赞、收藏、评论；接口返回后的列表状态正确回流，动态列表中可看到 `isLiked=true`、`isFavorited=true` 与最新评论。
- 简历链路已真实打通：在本地补入启用模板 `templateId=1` 后，用户端成功创建简历 `resumeId=1`，并从编辑器跳回简历列表页。
- CodePen 链路已真实打通：测试账号成功发布作品 `penId=2`，`/codepen/my` 能回流新作品，详情页可正常打开；积分明细中还能看到 `创建代码作品 +10` 的奖励流水。
- 小组讨论链路已真实打通：测试账号在 `teamId=1` 成功发布讨论 `discussionId=2`，讨论列表接口能查到这条新记录。
- 模拟面试链路已从“只能进入配置页”推进到“真实会话 + 作答反馈 + 提前结束 + 报告生成 + 历史回流 + AI 总结生成”：本轮新增会话 `sessionId=15` 与 `sessionId=16`，用户端可正常进入 `session` 页面拉取题目、提交答案、生成追问、结束面试并查看报告；其中 `sessionId=16` 已成功生成 AI 总结与学习建议。
- 模拟面试分数与问答记录已落库：`sessionId=15` 总分 `70`，`sessionId=16` 总分 `60`；两次会话都生成了自动追问记录（分别出现 `qaId=86` 与 `qaId=92`），后台运营台可以查看完整答题内容、AI 反馈和追问链路。
- 聊天室 WebSocket 双端联动已真实打通：用户端 `/chat` 成功建立 websocket 连接并发送文本消息 `手册联调消息 2026-04-24 23:19:25`；管理端 `/chat/messages` 可检索到消息 `id=8`，`/chat/users` 在用户页面保持连接期间可以看到测试账号 `codexuser0424` 在线。
- 管理端模拟面试运营台已验证真实数据回流：`/mock-interview/sessions` 当前可直接看到本轮新增会话 `15`、`16`，详情抽屉能展示答题内容、得分、追问与 AI 反馈，说明前后台会话数据链路是通的。

### 10.1 热榜服务存在外部依赖问题

后端启动日志中出现告警：

```text
初始化平台[douyin]热榜数据失败: 500 Internal Server Error
未能提取到有效的Cookie
```

影响：

- `http://localhost:3001/moyu-tools/hot-topics` 可能没有完整热榜数据
- 问题不在前端页面本身，而在外部热榜抓取链路

### 10.2 RAG 相关功能需要单独启动 sidecar

如果不启动 `llamaindex-service`，以下功能只适合做页面级检查，不适合做完整功能验证：

- 管理端 `AI 配置与观测` 中的 RAG 调试
- 模拟面试、SQL 优化、求职作战台中依赖知识库召回的链路

### 10.3 当前环境里 `java` 不在 PowerShell PATH

表现：

- `java -version` 在当前终端里不可直接执行
- 但 Maven 可以识别到 JDK 17

解决：

- 显式设置 `JAVA_HOME`
- 或直接使用 `C:\Program Files\Java\jdk-17\bin\java.exe`

### 10.4 `spring-boot:run` 在当前环境不建议直接使用

本轮实测：

- 直接从根目录执行 `mvn -pl xiaou-application -am spring-boot:run` 会遇到插件前缀解析问题
- 稳定方案是 `install -DskipTests` 后再跑 fat jar

### 10.5 Sa-Token 配置项已过期

启动告警：

```text
配置项已过期，请更换：sa-token.activity-timeout -> sa-token.active-timeout
```

这不会阻断启动，但建议后续把配置名升级掉。

### 10.6 OJ 判题依赖外部 go-judge 服务

本轮使用测试账号对题目 `1` 发起了一次真实提交，生成提交记录 `#6`，最终状态为 `runtime_error`。提交详情中给出的错误是：

```text
调用 go-judge 失败: I/O error on POST request for "http://154.222.18.220:5050/run": 154.222.18.220:5050 failed to respond
```

影响：

- `OJ 提交`、`我的提交`、`提交详情` 页面本身可正常验证
- 但完整 AC / WA / CE 判题结果依赖外部 `go-judge` 服务可用
- 如果要继续深测 OJ 判题，需要把该执行服务改成本地可达地址，或把远端服务恢复

### 10.7 博客与闪卡存在初始测试数据差异

本轮开始时，测试账号 `codexuser0424` 存在两类初始数据差异：

- 访问 `http://localhost:3001/blog` 时会显示“您还未开通博客”
- 访问 `http://localhost:3001/flashcard` 时公开卡组为空

当前处理结果：

- 博客链路已在第四轮通过积分开通解决，当前测试账号的博客已开通，`blogId=2`
- 闪卡链路仍然依赖本轮新建的测试卡组：`http://localhost:3001/flashcard/deck/2`

### 10.8 积分页签到状态字段与前端读取字段不一致

本轮接口与页面联测发现：

- `GET /api/user/points/balance` 返回字段是 `todayCheckedIn`
- 用户端 [points 页](http://localhost:3001/points) 读取的是 `hasCheckedToday`

影响：

- 实际已经签到成功，积分也已到账
- 但积分页按钮文案仍显示“今日打卡”，没有切换成“今日已打卡”
- 这属于前后端字段命名不一致导致的状态展示问题，不影响积分流水真实写入

### 10.9 管理端简历模板创建链路当前被写操作阻塞

本轮为继续验证用户端简历保存，额外核查了管理端模板创建链路，发现两个问题：

- 直接调用 `POST /api/admin/resume/templates` 时返回 `{"code":600,"message":"请先登录"}`，但同一管理员 token 调 `GET /api/admin/resume/templates` 是正常的，说明该写接口当前存在登录态 / 鉴权实现问题
- 从代码可见，管理端 `resume/templates` 页面把 `tags` 与 `techStack` 作为字符串提交，而后端 `ResumeTemplateRequest` 要求的是 `List<String>`，这里还存在前后端 DTO 契约不一致风险

本轮处理方式：

- 直接向本地 MySQL 的 `resume_templates` 表补入了一条启用模板 `templateId=1`
- 这是为了继续验证用户端简历保存链路的本地补数，不代表管理端“新建模板”页面本身已经真正可用

### 10.10 小组详情统计与讨论列表存在回写不同步

本轮已成功创建小组讨论 `discussionId=2`，并能通过 `GET /api/user/team/1/discussions` 查到该记录，但 `GET /api/user/team/1` 返回的 `totalDiscussions` 仍然是 `0`。

影响：

- 讨论发布本身是成功的
- 但小组详情头部统计卡片没有同步反映最新讨论数
- 需要继续排查团队统计字段的异步回写或聚合逻辑

### 10.11 模拟面试转学习资产链路已真实打通

本轮已完成从 `mock_interview` 到 `learning-assets` 再到管理端审核台的真实闭环验证：

- 用户端在 `sessionId=16` 的模拟面试报告页可正常打开“转为学习资产”弹窗，并选择目标资产类型后发起转化
- 新增学习资产记录 `recordId=3`，当前整体状态为 `PARTIAL_PUBLISHED`
- `recordId=3` 共生成 `6` 个候选资产，其中闪卡 `10`、`11` 与练习计划 `13`、`14` 已直接发布
- 首次发布后，知识节点 `12` 与面试题草稿 `15` 进入审核队列；叠加 `recordId=2` 的候选 `6`、`9`，管理端截图 `74-learning-assets-review-queued.png` 与 `78-learning-assets-review-detail-opened.png` 记录的是当时共 `4` 条 `REVIEWING` 数据的真实队列态
- 随后继续完成了管理端审核台三类核心动作的真实验证：
- `candidateId=12` 审核通过并发布到知识图谱 `mapId=1`，生成知识节点 `targetId=3`
- `candidateId=15` 已按备注“手册补测：内容仍需人工润色，先驳回”成功驳回
- `candidateId=6` 已成功合并到已有知识节点 `targetId=1`
- `candidateId=9` 已审核通过并发布到题单 `questionSetId=2`，生成面试题 `targetId=159`
- 截至当前，管理端 `POST /api/admin/learning-assets/candidates/list` 返回 `total=0`，说明 `mock_interview -> learning-assets -> publish -> admin review -> approve/reject/merge` 主链路已完整跑通

### 10.12 聊天室撤回已验证成功，但实时态存在 tempId 同步问题

本轮已完成聊天消息撤回补测，结论如下：

- 直接发送新消息后立刻点“撤回”，前端实时列表里有概率仍保留 `temp_xxx` 临时 ID
- 这时调用 `/api/user/chat/message/recall` 会把字符串型 `tempId` 传给后端，`.tmp/manual/backend-run.out.log` 中已有 `Cannot deserialize value of type java.lang.Long from String \"temp_...\"` 记录
- 在刷新聊天页后，历史消息会回填真实数字 `messageId`，此时通过 UI 执行撤回可以成功
- `112-chat-recall-succeeded.png` 为成功 toast 截图；用户端历史接口 `POST /api/user/chat/history` 已确认被撤回消息不再返回，说明撤回实际已落库生效

### 10.13 聊天室系统公告、禁言与踢出已完成补测

本轮额外验证了聊天室运营动作的用户端回流表现：

- 管理端可从 `/chat/messages` 打开发送系统公告弹窗，用户端 `/chat` 已收到橙色系统公告消息条
- 管理端可从 `/chat/users` 对在线用户发起禁言，用户端随后发送消息会提示“您已被禁言，无法发送消息”
- 用户端被管理员踢出聊天室后，会显示“您已被管理员踢出聊天室”，连接状态切为未连接
- 因此系统公告、禁言、踢出三条聊天室运营链路已完成基础闭环验证

### 10.14 聊天室图片消息链路已真实打通，且已切到当前项目本地文件地址

本轮把聊天图片消息这条链路从“只能出现前端临时态”修到了“真实上传、WebSocket 入库、管理端回流、当前项目本地文件 URL 可访问”：

- 文件上传链路先后修复了 3 个真实阻塞点：
- `LocalStorageStrategy` 在 Undertow / Windows 下 `transferTo()` 后再次读取临时文件，导致 `NoSuchFileException`
- `FileBackupServiceImpl` 的 `@Async` 方法返回 `boolean`，触发 `Invalid return type for async method`
- 用户端聊天室上传成功后只读取 `fileUrl`，但上传接口实际返回的是 `accessUrl`
- 随后又修复了聊天消息入库约束：`chat_messages.content` 不能为 `NULL`，图片消息现在会自动补成 `"[图片]"` 后再入库
- 最新成功图片消息为 `messageId=21`，管理端 `POST /api/admin/chat/messages/list` 已可看到 `messageType=2`
- 最新图片消息的 `imageUrl` 已切到当前项目本地地址：`http://localhost:9999/api/files/...`
- 当前后端已补上 `/api/files/**` 本地静态映射，直接访问该地址返回 `200`
- 早于本次修复产生的历史图片消息（如 `messageId=18~20`）仍保留旧的 `http://localhost:8080/files/...` 地址，这是旧文件记录的历史数据，不影响当前新上传链路

### 10.15 学习资产审核台空状态文案为 `No Data`

本轮补拍 `80-learning-assets-review-empty-after-actions.png` 时确认：

- 管理端 `http://localhost:3000/learning-assets/review` 在候选列表清空后，表格空状态显示的是 Element Plus 默认文案 `No Data`
- 因此如果后续复跑脚本或手工检查时还按 `暂无数据` 去定位，会误以为页面未进入空态
- 当前手册与补测脚本都已按真实页面行为更新

## 11. 建议的后续补测顺序

如果要继续把“所有细分功能”测到更深一层，建议按下面顺序补：

1. 模拟面试与学习资产深化：转化、发布、审核通过/驳回/合并均已验证，后续可补完整 5 题答满、多轮自动追问、不同方向/难度/出题模式组合，以及用户端通知中心/学习资产页对审核结果的细节回流
2. 聊天室运营深化：文本消息、图片消息、撤回、系统公告、禁言、踢出均已验证，后续可补多标签页在线人数波动、断线重连、禁言到期恢复
3. 团队深测：小组打卡/任务/排行、邀请加入、讨论数与打卡数统计回写
4. 简历与模板链路：修复管理端 `resume/templates` 的写接口与 DTO 契约后，补“真正通过后台新建模板 -> 用户端新建简历”的完整正向链路
5. 管理端操作型弹窗与提交：社区分类/标签创建编辑、通知模板编辑、文件迁移动作，以及学习资产审核台更多不同筛选组合/详情编辑态补图
6. OJ 与 AI 依赖链路：把 `go-judge` 改成本地可用地址，补 AC / WA / CE / TLE；启动 RAG sidecar 后补测 `system/ai-config` 与依赖检索的 AI 页面

## 12. 交付物清单

- 手册：`docs/Code-Nest-本地全功能截图测试与操作手册-2026-04-24.md`
- 用户端截图：`docs/manual-assets/2026-04-24/user`
- 管理端截图：`docs/manual-assets/2026-04-24/admin`
- 后端构建日志：`.tmp/manual/backend-build.out.log`
- 后端运行日志：`.tmp/manual/backend-run.out.log`
- 前端运行日志：
  - `.tmp/manual/admin-front.out.log`
  - `.tmp/manual/user-front.out.log`
