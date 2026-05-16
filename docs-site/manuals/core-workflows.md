# 核心链路教程

本页按“入口、动作、API、数据、验证”整理主要业务闭环。适合回归测试、演示和交接。

## 注册登录到首页

| 步骤 | 操作 | 验证 |
| --- | --- | --- |
| 1 | 打开 `/register` 或 `/login` | 页面渲染，验证码加载 |
| 2 | 注册或登录用户账号 | `POST /user/auth/register` 或 `POST /user/auth/login` 返回成功 |
| 3 | 进入首页 `/` | 用户端路由守卫通过 |
| 4 | 打开 `/profile` | 当前用户资料可读取和更新 |

相关表：`user_info`。相关文档：[鉴权与用户体系](/modules/auth)、[用户账户与个人中心](/modules/user-account)。

## 面试题学习闭环

| 步骤 | 操作 | 验证 |
| --- | --- | --- |
| 1 | 打开 `/interview` | 题单列表展示 |
| 2 | 进入 `/interview/question-sets/:id` | 题单详情和题目列表展示 |
| 3 | 进入题目学习页 | 答案可展开，上一题下一题可切换 |
| 4 | 提交掌握度 | `POST /interview/mastery/mark` 返回 `200` |
| 5 | 打开 `/interview/review` | 复习统计和待复习列表更新 |

相关表：`interview_question_set`、`interview_question`、`interview_learn_record`、`interview_mastery_record`、`interview_mastery_history`。

## OJ 刷题与赛事

| 步骤 | 操作 | 验证 |
| --- | --- | --- |
| 1 | 打开 `/oj` | 题目列表展示 |
| 2 | 进入 `/oj/problem/:id` | 题面、代码编辑器和提交区可用 |
| 3 | 运行或提交代码 | `POST /oj/run`、`POST /oj/test` 或 `POST /oj/submit` 返回提交结果 |
| 4 | 打开 `/oj/my-submissions` | 提交记录回流 |
| 5 | 打开 `/oj/submission/:id` | 判题详情展示 |
| 6 | 打开 `/oj/contests/:id` 并报名 | `POST /oj/contests/{id}/join` 成功，报名状态更新 |

相关表：`oj_problem`、`oj_test_case`、`oj_submission`、`oj_contest`、`oj_contest_participant`。完整判题依赖 go-judge 服务。

## 模拟面试到学习资产

| 步骤 | 操作 | 验证 |
| --- | --- | --- |
| 1 | 打开 `/mock-interview` 选择方向 | 方向列表来自 `/user/mock-interview/directions` |
| 2 | 进入 `/mock-interview/config` | 难度、题量、风格可配置 |
| 3 | 创建会话 | `POST /user/mock-interview/create` 返回 session |
| 4 | 在 `/mock-interview/session?sessionId=...` 作答 | AI 评分、反馈和追问生成 |
| 5 | 结束面试 | 报告可在 `/mock-interview/report?sessionId=...` 展示 |
| 6 | 生成总结 | AI 总结和学习建议回流 |
| 7 | 转为学习资产 | 学习资产记录和候选资产生成 |
| 8 | 管理端审核 | `/learning-assets/review` 可通过、驳回或合并候选资产 |

相关表：`mock_interview_session`、`mock_interview_qa`、`learning_asset_record`、`learning_asset_candidate`、`learning_asset_publish_log`。相关文档：[模拟面试与求职作战台](/modules/mock-interview-job-battle)、[学习资产](/modules/learning-assets)。

## 内容发布闭环

| 功能 | 用户端动作 | 后台验证 |
| --- | --- | --- |
| 社区 | `/community/create` 发帖，详情页评论 | `/community/posts`、`/community/comments` |
| 动态 | `/moments` 发布、点赞、收藏、评论 | `/moments/list`、`/moments/comments`、`/moments/statistics` |
| 博客 | `/blog` 开通博客，`/blog/editor` 发文 | `/blog/articles`、`/blog/categories`、`/blog/tags` |
| CodePen | `/codepen/editor` 发布作品 | `/codepen/pens`、`/codepen/statistics` |
| 简历 | `/resume/editor` 保存简历 | `/resume/analytics`、`/resume/reports` |
| 小组讨论 | `/team/:id` 发布讨论 | 数据表 `study_team_discussion` 和用户端回流 |

写操作的完整验证口径是：接口成功、用户端回流、管理端可见。

## 积分与抽奖

| 步骤 | 操作 | 验证 |
| --- | --- | --- |
| 1 | 打开 `/points` | 积分余额和流水展示 |
| 2 | 执行每日签到 | `POST /user/points/checkin` 生成积分明细 |
| 3 | 管理端 `/points/grant` 发放积分 | 用户余额和 `user_points_detail` 更新 |
| 4 | 打开 `/lottery` 抽奖 | 生成 `lottery_draw_record`，扣减积分和库存 |
| 5 | 管理端 `/lottery` 查看记录 | 奖品配置、抽奖记录、限制规则可见 |

相关文档：[积分与抽奖](/modules/points)。

## 聊天室运营

| 步骤 | 操作 | 验证 |
| --- | --- | --- |
| 1 | 用户端打开 `/chat` | WebSocket `/ws/chat?token=...` 连接成功 |
| 2 | 发送文本或图片消息 | 消息入库，`MESSAGE_ACK` 回流 |
| 3 | 管理端 `/chat/messages` 检索消息 | 消息列表可见 |
| 4 | 发送系统公告 | 用户端收到 `SYSTEM` |
| 5 | 禁言用户 | 用户端发送失败并提示 |
| 6 | 踢出用户 | 用户端收到 `KICK_OUT` 并断开 |
| 7 | 用户撤回消息 | 用户端和历史消息列表同步更新 |

协议细节见 [WebSocket 协议](/reference/websocket)。

## AI 配置、RAG 与回归

| 步骤 | 操作 | 验证 |
| --- | --- | --- |
| 1 | 启动后端和 RAG sidecar | `/admin/ai/config/rag-service/health` 可达 |
| 2 | 打开管理端 `/system/ai-config` | Runtime、Prompt、Schema、RAG 面板展示 |
| 3 | 执行 Prompt 调试 | `POST /admin/ai/config/prompt-debug` 返回模型结果和 Schema 信息 |
| 4 | 执行 RAG 调试 | `POST /admin/ai/config/rag-debug` 返回检索节点 |
| 5 | 导入样例知识 | `POST /admin/ai/config/rag-service/sample-import` 成功 |
| 6 | 运行回归 | `POST /admin/ai/config/regression/run` 生成回归结果 |
| 7 | 查看治理概览 | `/system/ai-governance` 汇总质量状态 |

相关文档：[AI Runtime](/modules/ai-runtime)、[AI Schema 与治理](/reference/ai-schemas)。
