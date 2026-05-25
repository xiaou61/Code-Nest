# 验证记录与已知问题

本页来自 2026-04-24 本地全功能截图测试。完整原始记录在 `docs/Code-Nest-本地全功能截图测试与操作手册-2026-04-24.md`。

## 已完成验证

| 类别 | 结果 |
| --- | --- |
| 本地服务 | 后端、用户端、管理端均已拉起并完成截图 |
| 截图数量 | 用户端 118 张，管理端 81 张，共 199 张 |
| 登录链路 | 用户端注册/登录、管理端登录已验证 |
| 面试题库 | 题单、题目学习、掌握度、复习、收藏已验证 |
| OJ | 题目详情、提交记录、赛事报名、赛事题目已验证；判题依赖 go-judge |
| 模拟面试 | 创建会话、作答反馈、追问、提前结束、报告、AI 总结、历史回流已验证 |
| 学习资产 | 模拟面试转资产、候选生成、用户发布、管理端审核、通过、驳回、合并已验证 |
| 聊天室 | WebSocket 连接、文本消息、图片消息、撤回、公告、禁言、踢出已验证 |
| 积分抽奖 | 签到、后台发放、流水、抽奖扣分、抽奖记录已验证 |
| 内容创作 | 博客、社区、动态、CodePen、简历、小组讨论写操作已验证 |
| 管理端 | 大多数二级路由和关键运营动作已截图验证 |

## v2.2.2 基线验证

v2.2.2 版本在 v2.2.1 基础上对团队工作流契约和发布流程做了对齐。以下是基线验证结果。

### 构建验证

| 构建项 | 命令 | 结果 |
| --- | --- | --- |
| 后端聚合构建 | `mvn -pl xiaou-application -am clean package -DskipTests` | BUILD SUCCESS |
| 用户端构建 | `cd vue3-user-front && npm run build` | built in Xms |
| 管理端构建 | `cd vue3-admin-front && npm run build` | built in Xms |
| 文档站构建 | `cd docs-site && npm run build` | built in Xms |

### 后端健康验证

| 检查项 | 命令 | 期望 | 说明 |
| --- | --- | --- | --- |
| 服务健康 | `curl http://localhost:9999/api/actuator/health` | `{"status":"UP"}` | 所有组件正常 |
| 数据库连接 | 查看启动日志 | `HikariPool-1 - Start completed` | MySQL 连接正常 |
| Redis 连接 | 查看启动日志 | 无 Redis 连接错误 | 业务 db3 + Sa-Token db4 |
| Swagger | `curl http://localhost:9999/api/swagger-ui.html` | 返回 HTML | API 文档可访问 |
| Prometheus | `curl http://localhost:9999/api/actuator/prometheus` | 返回指标 | 监控端点可用 |

### 核心链路烟测

| 链路 | 验证方式 | 结果 |
| --- | --- | --- |
| 用户端登录 | `POST /api/user/auth/login` | 返回 Token |
| 管理端登录 | `POST /api/admin/login` | 返回 Token |
| 验证码 | `GET /api/captcha/image` | 返回 captchaKey + 图片 |
| 聊天 ws-ticket | `GET /api/user/chat/ws-ticket` + `Authorization: Bearer <token>` | 返回一次性 ticket |
| 文件上传 | 用户端头像/聊天图片上传 | 上传成功，URL 可访问 |

### 安全边界验证

| 场景 | 验证方式 | 期望 | 结果 |
| --- | --- | --- | --- |
| 未登录访问 | 不带 Token 调业务接口 | `{"code":701}` | 符合 |
| 用户端 Token 调管理端 | 用户端 Token 调 `/api/admin/` | `{"code":703}` | 符合 |
| ws-ticket 复用 | 同一 ticket 连两次 WebSocket | 第二次被拒绝 | 符合 |

### 依赖状态

| 依赖 | 状态 | 说明 |
| --- | --- | --- |
| MySQL | 已启动 | 本地 3306，数据库 `code_nest` |
| Redis | 已启动 | 本地 6379，业务 db3 + Sa-Token db4 |
| go-judge | 未启动 | OJ 判题依赖远端，本地未部署 |
| RAG sidecar | 未启动 | AI RAG 功能未完整验证 |
| Prometheus + Grafana | 未启动 | 监控栈未启动 |

## 已知问题

| 问题 | 影响 | 建议 |
| --- | --- | --- |
| 热榜服务外部 Cookie 依赖失败 | `/moyu-tools/hot-topics` 可能无完整数据 | 调整热榜抓取策略或缓存兜底 |
| RAG 相关能力需要 sidecar | AI 配置页、模拟面试、SQL 优化的知识库召回无法完整验证 | 使用 `scripts/ai/start-ai-dev-stack.ps1` 启动 |
| 当前 PowerShell PATH 未直接包含 `java` | 直接 `java -version` 不可用 | 显式设置 `JAVA_HOME` |
| `spring-boot:run` 在当前环境不稳定 | 启动后端容易失败 | 先 `mvn -pl xiaou-application -am install -DskipTests`，再运行 jar |
| Sa-Token `activity-timeout` 配置已过期 | 启动有告警，不阻断 | 改为 `active-timeout` |
| go-judge 远端不可达 | OJ 完整 AC/WA/CE/TLE 结果无法稳定验证 | 部署本地 go-judge 或恢复远端服务 |
| 积分页签到字段不一致 | 实际签到成功，但按钮文案可能不更新 | 前端兼容 `todayCheckedIn` 或后端补 `hasCheckedToday` |
| 管理端简历模板写接口和 DTO 契约存在风险 | 后台新建模板链路被阻塞 | 修复鉴权与 `List<String>` 字段提交 |
| 小组详情统计不同步 | 讨论成功发布但头部统计未即时更新 | 检查聚合统计回写逻辑 |
| 聊天撤回实时态 tempId 同步问题 | 刚发送后立即撤回可能传字符串临时 ID | `MESSAGE_ACK` 后再允许撤回，或前端映射真实 ID |
| 学习资产审核空态显示 `No Data` | 自动化定位不能按 `暂无数据` 判断 | 测试脚本按真实文案更新 |

## 补测优先级

| 优先级 | 事项 |
| --- | --- |
| P0 | OJ go-judge 本地化、管理端简历模板写链路、积分签到字段对齐 |
| P1 | 聊天 tempId 撤回体验、学习资产通知回流、团队统计回写 |
| P2 | 多标签页聊天室在线人数、禁言到期恢复、AI RAG sidecar 全流程回归 |
| P3 | 管理端更多弹窗类写操作截图，社区分类/标签、通知模板、文件迁移 |

## 交付物

| 类型 | 路径 |
| --- | --- |
| 原始手册 | `docs/Code-Nest-本地全功能截图测试与操作手册-2026-04-24.md` |
| 用户端截图 | `docs/manual-assets/2026-04-24/user` |
| 管理端截图 | `docs/manual-assets/2026-04-24/admin` |
| 文档站操作手册 | `docs-site/manuals` |

## 回归建议

发布前先用 [发布前验证](/guide/release-verification) 判断本次变更需要跑哪些构建、接口和页面烟测。本页负责沉淀真实结果，不替代验证清单。

1. 每个版本至少跑一遍登录、题库、OJ、模拟面试、学习资产、聊天、积分、内容发布。
2. 涉及 AI、文件上传、WebSocket、部署配置或 OJ 判题时，把依赖状态写清楚。
3. 回归结果优先更新本页，再同步拆到对应模块文档。
4. 修复已知问题后，把问题从"已知问题"移动到"已完成验证"，保留版本和验证方式。

## 验证记录格式

每次验证建议按以下格式沉淀，方便后来的人对照：

```text
验证范围：
日期：
版本：
已执行：
通过项：
未验证：
依赖状态：
```

示例：

```text
验证范围：用户端聊天 ws-ticket 和消息发送
日期：2026-04-24
版本：v2.2.2
已执行：用户端 build、后端 package、登录后进入 /chat、发送文本消息
通过项：ws-ticket 获取成功，WebSocket CONNECT 成功，消息 ACK 成功
未验证：多实例广播、禁言到期恢复
依赖状态：Redis(db3/db4) 和 MySQL 已启动，未启动 go-judge 和监控
```

适合沉淀到：

1. 本页。
2. 对应模块页"验证清单"。
3. PR 描述或版本记录。

## 版本验证历史

| 版本 | 日期 | 验证范围 | 结果 | 记录位置 |
| --- | --- | --- | --- | --- |
| v2.2.2 | 2026-04-24 | 全功能截图测试 | 199 张截图，核心链路通过，已知问题 11 条 | 本页 + 原始手册 |
| v2.2.1 | 2026-04 | 团队工作流契约对齐 | 构建通过，接口烟测通过 | git log |

如果你准备把这些结果变成模块 owner 的长期值守资产，继续配合 [模块值守与回归手册](/guide/module-owner-playbook) 使用会更顺。
