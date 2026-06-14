# 发布前验证

Code Nest 是多模块后端、两个 Vue 前端、独立文档站和多个可选 sidecar 组成的项目。发布前验证不要只跑一个命令，而要按"改了哪里、影响哪些链路、哪些依赖必须在线"来分层执行。

本页给新维护者一个可直接照抄的验证顺序。它不是要求每次都跑满全量矩阵，而是帮助你用合适的成本覆盖最大的风险。

如果你不是从"变更类型"切入，而是已经知道自己主要改的是某个模块，可以先看 [模块最小回归矩阵](/reference/module-regression-matrix) 再回到本页组合发布前验证项。

如果你已经知道这次最危险的不是成功路径，而是某类失败态，比如权限拒绝、文件 `403`、WebSocket `ERROR`、AI 结构缺失或 OJ 卡状态，可以先对照 [异常路径与失败态索引](/reference/failure-paths) 把补测口径列出来。

如果这次改动会触发通知、排行、积分、日志或用户端回流，也建议先看 [事件、通知与回流索引](/reference/event-backflow-index)，避免只验证主接口，却漏了副作用。

## 怎么使用这页

发布前可以按这个顺序走：

1. 先看 `git diff --stat`，确认这次到底改了文档、后端、用户端、管理端还是部署配置。
2. 在"先判断变更类型"里找到对应行，把最低验证项列出来。
3. 如果一次提交同时改了多个域，就把这些验证项合并，不要只跑其中一个。
4. 依赖没有启动时，不要把链路写成"已验证"，而要写清楚"构建已通过，依赖未联通"。
5. 验证通过后，再同步模块页、参考索引、版本历史和验证记录。

一个简单例子：如果你改了聊天室后端限流和用户端聊天页面，就至少要跑后端构建、用户端构建、WebSocket ws-ticket 连接、消息发送、限流失败态，并把结论写回 [验证记录与已知问题](/manuals/verified-scenarios)。

## 先判断变更类型

| 变更范围 | 最低验证 |
| --- | --- |
| 只改 `docs-site/**` | `cd docs-site && npm run build`，再检查搜索和深层路由 |
| 只改用户端页面 | `cd vue3-user-front && npm run build`，再手工打开对应路由 |
| 只改管理端页面 | `cd vue3-admin-front && npm run build`，再手工打开对应路由 |
| 改后端 Controller、Service、Mapper、配置 | `mvn -pl xiaou-application -am clean package -DskipTests`，再跑相关接口或页面 |
| 改 AI Runtime、Prompt、Schema、Graph、RAG | AI 单元/回归测试 + 管理端 AI 配置页 + 必要时 RAG sidecar |
| 改 OJ 判题 | 后端构建 + go-judge 健康 + 至少一次 `/oj/run` 或 `/oj/test` |
| 改聊天 | 后端构建 + 用户端构建 + WebSocket ws-ticket 连接 + 消息发送和失败态 |
| 改文件上传 | 后端构建 + 上传接口 + `/api/files/**` 访问 + 业务归属页面回流 |

如果一次提交同时覆盖多个范围，取这些验证项的并集。

## 基础构建命令

后端聚合构建：

```powershell
mvn -pl xiaou-application -am clean package -DskipTests
```

构建成功标志：控制台显示 `BUILD SUCCESS`，产物在 `xiaou-application/target/xiaou-application-*.jar`。

用户端构建：

```powershell
cd vue3-user-front
npm ci
npm run build
```

构建成功标志：控制台显示 `vite v5.x.x building for production` → `built in Xms`，产物在 `vue3-user-front/dist/`。

管理端构建：

```powershell
cd vue3-admin-front
npm ci
npm run build
```

构建成功标志：同上，产物在 `vue3-admin-front/dist/`。

文档站构建：

```powershell
cd docs-site
npm ci
npm run build
```

构建成功标志：控制台显示 `vitepress v1.x.x building for production` → `built in Xms`，产物在 `docs-site/.vitepress/dist/`。

前端项目也有 `npm run lint`，但当前脚本带 `--fix`，会直接修改文件。它适合本地整理代码风格，不适合作为只读检查命令直接放进发布脚本里。

## 后端测试口径

当前仓库里已经有一批 JUnit 5 和 Mockito 测试，重点集中在 AI Runtime、OJ、求职作战台、学习资产、SQL 优化和系统 AI 配置等高风险模块。改这些模块时，优先跑对应模块测试。

### 模块级测试命令

| 模块 | 命令 | 测试类数量 | 测试重点 |
| --- | --- | --- | --- |
| `xiaou-ai` | `mvn -pl xiaou-ai -am test` | 35+ | Prompt、Schema、RAG、Graph、回归、指标 |
| `xiaou-oj` | `mvn -pl xiaou-oj -am test` | 6 | 赛事规则、排名、提交、Controller、SQL |
| `xiaou-mock-interview` | `mvn -pl xiaou-mock-interview -am test` | 5 | 模拟面试、求职作战台、Career Loop |
| `xiaou-learning-asset` | `mvn -pl xiaou-learning-asset -am test` | 5 | 资产转化、发布、审核、统计 |
| `xiaou-sql-optimizer` | `mvn -pl xiaou-sql-optimizer -am test` | 1 | SQL 优化服务 |
| `xiaou-system` | `mvn -pl xiaou-system -am test` | 2 | AI 配置、回归运行状态 |

如果只想跑某个测试类，加 `-Dtest=类名` 缩小范围：

```powershell
mvn -pl xiaou-oj -am -Dtest=ContestRuleValidatorTest test
```

### AI 回归完整命令

改 AI 相关代码时，跑完整回归套件：

```powershell
mvn -pl xiaou-ai -am "-Dtest=AiSceneRegressionEvalTest,InterviewGraphTest,JobBattleGraphTest,SqlOptimizeGraphTest,AiSqlOptimizeServiceImplTest,AiPromptSpecTest,AiStructuredOutputValidatorTest,LlamaIndexClientTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

如果新增 AI 场景，要同步更新：
1. Prompt Catalog。
2. 结构化输出 Schema。
3. RAG query/profile。
4. `scene-regression-cases.json`。
5. [AI Runtime](/modules/ai-runtime) 和 [AI Schema 与治理](/reference/ai-schemas)。

后端验证要注意两点：

1. `-DskipTests` 只能证明能编译和打包，不能证明业务逻辑没坏。
2. 涉及数据库、Redis、AI、go-judge 的链路，需要启动依赖后做接口或页面级验证。

## 后端健康检查

后端启动后（端口 9999，context-path `/api`），用 Actuator 端点验证各依赖：

```powershell
# 基础健康
curl http://localhost:9999/api/actuator/health
# 期望: {"status":"UP"}

# 详细组件状态（diskSpace、redis、dataSource 等）
curl http://localhost:9999/api/actuator/health
# 如果返回 {"status":"UP","components":{"diskSpace":{"status":"UP"},"redis":{"status":"UP"},...}}
# 表示所有组件正常

# Prometheus 指标（确认采集端点可用）
curl http://localhost:9999/api/actuator/prometheus
# 期望: 返回 Prometheus 格式的指标数据
```

组件健康解读：

| 组件 | status=DOWN 原因 | 修复 |
| --- | --- | --- |
| `dataSource` | MySQL 连接失败 | 检查 `spring.datasource.url` / 密码 / MySQL 是否运行 |
| `redis` | Redisson 连接失败 | 检查 `spring.data.redis.redisson.config` / Redis 是否运行 |
| `diskSpace` | 磁盘空间不足 | 清理磁盘或修改 `management.health.diskspace.threshold` |

如果某个组件 DOWN，整个 health 端点返回 status=DOWN，Prometheus 也会记录。

## 接口级验证

登录是大部分验证的前置条件。先获取 Token：

```powershell
# 用户端登录
curl -X POST http://localhost:9999/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test123"}'
# 期望: {"code":200,"data":{"token":"xxx",...}}

# 管理端登录
curl -X POST http://localhost:9999/api/admin/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
# 期望: {"code":200,"data":{"token":"xxx",...}}
```

拿到 Token 后，后续请求 Header 格式为 `Authorization: Bearer <token>`。

验证码接口（公开，不需要 Token）：

```powershell
curl http://localhost:9999/api/captcha/image
# 期望: {"code":200,"data":{"captchaKey":"xxx","captchaImage":"base64..."}}
```

Swagger 页面：

```powershell
curl http://localhost:9999/api/swagger-ui.html
# 期望: 返回 Swagger UI HTML 页面
```

## 核心业务烟测

每次版本合并前，建议至少抽测这些链路：

| 链路 | 入口 | 关键预期 | 验证方法 |
| --- | --- | --- | --- |
| 用户登录 | `/api/user/login` | 返回 Token 和用户信息 | `curl -X POST` + Body |
| 管理端登录 | `/api/admin/login` | 返回 Token 和管理员信息 | `curl -X POST` + Body |
| 验证码 | `/api/captcha/image` | 返回 captchaKey 和图片 | `curl GET` |
| 文件上传 | 头像、聊天图片或文件管理页 | 上传成功，返回 URL 可访问 | 用户端上传 → 检查 `/api/files/**` |
| 内容发布 | 社区、博客、动态、CodePen 任一写操作 | 用户端回流，管理端可见 | 创建帖子 → 检查列表和详情 |
| OJ | `/oj/problem/:id` | 题面展示，运行或自测可返回状态 | 用户端打开题目 → 点运行 |
| 聊天 | `/chat` | ws-ticket → WebSocket 连接 → 消息有 ACK | 先拿 ticket → 连 ws → 发消息 |
| 通知 | `/api/notification` | 未读数、详情、已读动作正常 | 触发一个通知 → 检查未读数 |
| AI 配置 | `/admin/ai-config` | Runtime 面板可打开，配置脱敏展示 | 管理端打开 AI 配置页 |
| 文档站 | `docs-site/.vitepress/dist` 或 preview | 首页、搜索、深层路由可打开 | 本地 preview 或部署后检查 |

### WebSocket 聊天室验证步骤

聊天室是最容易出问题的链路，因为涉及 ws-ticket + WebSocket + ACK 三步：

1. 获取 ws-ticket：

```powershell
curl -X GET http://localhost:9999/api/chat/ws-ticket \
  -H "Authorization: Bearer <user-token>"
# 期望: {"code":200,"data":{"ticket":"xxx"}}
```

2. 用 WebSocket 客户端连接：

```text
ws://localhost:9999/api/ws/chat?ticket=<ticket>
```

3. 发送一条消息，确认收到 ACK 或消息广播。

4. 验证失败态：同一 ticket 连第二次应被拒绝。

完整的业务链路可以参考 [核心链路教程](/manuals/core-workflows)，历史截图和已知问题可以参考 [验证记录与已知问题](/manuals/verified-scenarios)。

## AI 回归

AI 场景最容易出现"接口还通，但结构化输出变形"的问题。改 Prompt、Schema、Graph Runner、RAG Profile 或模型配置时，至少做三层检查：

| 层级 | 怎么查 | 命令或入口 |
| --- | --- | --- |
| Catalog | 确认 Prompt、RAG、Schema 仍可被管理端发现 | `GET /admin/ai/config/schema-catalog` |
| 单场景调试 | 管理端 Prompt 调试或 RAG 调试 | 管理端 `/system/ai-config` |
| 回归 | 运行 AI 回归测试集合 | `POST /admin/ai/config/regression/run` 或命令行 |

AI 回归命令行：

```powershell
mvn -pl xiaou-ai -am "-Dtest=AiSceneRegressionEvalTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

改这些内容时要跑 AI 回归：

| 变更 | 额外检查 |
| --- | --- |
| Prompt | 场景输出是否仍满足预期 |
| Schema | 必填字段、枚举、数组结构是否稳定 |
| RAG Profile | sidecar 健康、召回内容和空召回兜底 |
| Graph | 节点顺序、失败分支、结构化输出 |
| 模型配置 | 管理端 Runtime、Prompt 调试、回归记录 |

如果新增 AI 场景，要同步更新：
1. Prompt Catalog。
2. 结构化输出 Schema。
3. RAG query/profile。
4. `scene-regression-cases.json`。
5. [AI Runtime](/modules/ai-runtime) 和 [AI Schema 与治理](/reference/ai-schemas)。

## 安全边界验证

安全相关的验证不只看"接口返回 200"，还要看"不该通的确实不通"：

| 测试场景 | 验证点 | 怎么测 | 期望结果 |
| --- | --- | --- | --- |
| 管理端未登录 | 返回 701 | 用户端 Token 调管理端接口 | `{"code":701}` |
| 非管理员调管理接口 | 返回 703 | 普通用户 Token 调 `/api/admin/` | `{"code":703}` |
| 前端传 userId | 被忽略 | 发送伪造 userId，确认后端从 Token 取 | 接口返回真实 userId |
| 私有文件访问 | 返回拒绝或空 | 未登录或无权限用户访问 `is_public=0` 文件 | 返回 403 或空 |
| ws-ticket 复用 | 连接失败 | 用同一 ticket 连两次 WebSocket | 第二次连接被拒绝 |
| 文件类型限制 | 返回拒绝 | 上传 `.exe` 或 `.sh` 文件 | 返回错误码 803 |

错误码含义（来自 `GlobalExceptionHandler`）：

| 错误码 | 含义 |
| --- | --- |
| 701 | Token 无效或缺失 |
| 702 | Token 已过期 |
| 703 | 权限不足（`@RequireAdmin` 拒绝） |
| 704 | 账号被封禁 |
| 705 | 登录失败（用户名密码错误） |

更多安全边界细节见 [权限与安全边界](/guide/security-boundaries) 和 [权限注解与角色边界索引](/reference/permission-boundaries)。

## CI 工作流

当前仓库已有两个 GitHub Actions 工作流：

| Workflow | 触发条件 | 做什么 |
| --- | --- | --- |
| `.github/workflows/docs-site.yml` | `docs-site/**` 或 workflow 变更 | Node 20 + `npm ci` + `npm run build`，上传文档站产物 |
| `.github/workflows/ai-regression.yml` | `pom.xml`、`xiaou-common/**`、`xiaou-ai/**` 或 workflow 变更 | Java 17 + 运行 AI 测试集合，失败时上传 Surefire 报告 |

这些 CI 不能覆盖整个项目。改用户端、管理端或普通后端业务时，仍应在本地跑对应构建和手工烟测。

## 发布记录同步

功能验证通过后，还要同步文档和版本信息：

| 变更 | 要同步 |
| --- | --- |
| 新页面 | [前端路由索引](/reference/frontend-routes)、操作手册 |
| 新接口 | [API 路由索引](/reference/api-routes) |
| 新表或字段 | [数据表索引](/reference/database-tables) |
| 新错误码或事件 | [响应体与错误码](/reference/response-errors)、[WebSocket 协议](/reference/websocket) |
| 新 AI 场景 | [AI Schema 与治理](/reference/ai-schemas)、[AI Runtime](/modules/ai-runtime) |
| 新风险或补测结论 | [验证记录与已知问题](/manuals/verified-scenarios) |
| 新版本发布 | [版本历史](/modules/version-history)、[版本公告与发版交接模板](/guide/version-release-handoff-template) |

## 发布前最终清单

| # | 检查项 | 验证方法 | 预期 |
| --- | --- | --- | --- |
| 1 | Git 状态干净 | `git status` | 只有本次变更 |
| 2 | 后端构建通过 | `mvn -pl xiaou-application -am clean package -DskipTests` | `BUILD SUCCESS` |
| 3 | 用户端构建通过 | `cd vue3-user-front && npm run build` | `built in Xms` |
| 4 | 管理端构建通过 | `cd vue3-admin-front && npm run build` | `built in Xms` |
| 5 | 文档构建通过 | `cd docs-site && npm run build` | `built in Xms` |
| 6 | 后端健康 | `curl http://localhost:9999/api/actuator/health` | `{"status":"UP"}` |
| 7 | 登录链路 | 用户端和管理端登录 | 返回 Token |
| 8 | 关键业务 | 至少一个真实页面或接口完成烟测 | 操作成功 |
| 9 | 安全边界 | 701/703/705 失败态 | 返回正确错误码 |
| 10 | 外部依赖 | AI/RAG/go-judge/Redis/MySQL 状态已说明 | 写明"已验证"或"未验证原因" |
| 11 | CORS 配置 | 生产 `XIAOU_CORS_ALLOWED_ORIGIN_PATTERNS` 只含真实前端域名 | 不含 localhost |
| 12 | 密钥安全 | JWT/MySQL/Redis/AI Key 不用默认值 | 不含 123456/sk-xxxx |
| 13 | Redis 持久化 | AOF 或 RDB 已开启 | Sa-Token 登录态不会在重启后丢失 |
| 14 | 文档索引 | 路由/表/错误码/模块页已同步 | 无断链 |
| 15 | 已知问题 | 新发现的问题写入验证记录 | 有记录 |

## 常见误区

| 误区 | 更稳的做法 |
| --- | --- |
| 后端能打包就认为功能正常 | 打包只能证明编译通过，业务链路还要跑接口或页面 |
| AI 接口返回 `200` 就认为场景可用 | 还要检查结构化输出字段、Schema 校验、RAG 召回和回归样例 |
| 前端能打开首页就认为发布可用 | 要打开本次影响的真实路由，确认接口、权限、空态和失败态 |
| 文档构建通过就认为说明完整 | 还要确认模块页、API 索引、表索引、错误码和版本记录是否同步 |
| 外部依赖没启动时跳过不写 | 应记录"未验证原因"和下一次补测入口，避免后来的人误判 |
| 只跑 `npm run build` 就认为前端没问题 | 构建只证明 TypeScript/Vue 编译通过，真实页面还可能有运行时错误 |

如果你正在长期维护某个模块，发版前验证最好不要只按本页临时判断，继续配合 [模块值守与回归手册](/guide/module-owner-playbook) 把模块自己的最小回归组固定下来。

## 验证记录格式

验证结果建议写成下面这种格式，方便后来的人对照：

```text
验证范围：用户端聊天 ws-ticket 和消息发送
日期：2025-xx-xx
版本：v2.3.0
已执行：用户端 build、后端 package、登录后进入 /chat、发送文本消息
通过项：ws-ticket 获取成功，WebSocket CONNECT 成功，消息 ACK 成功
未验证：多实例广播、禁言到期恢复
依赖状态：Redis(db3/db4) 和 MySQL 已启动，未启动 go-judge 和监控
```

适合沉淀到：
1. [验证记录与已知问题](/manuals/verified-scenarios)。
2. 对应模块页"验证清单"。
3. PR 描述或版本记录。

## 选择验证的速查表

| 改了什么 | 至少跑什么 |
| --- | --- |
| 只改文档 | `cd docs-site && npm run build` + 搜索 + 深层路由 |
| 后端普通业务 | `mvn -pl xiaou-application -am clean package -DskipTests` + 相关接口 |
| AI Runtime | AI 回归命令 + 管理端场景调试 |
| OJ | `mvn -pl xiaou-oj -am test` + go-judge 联调 |
| 模拟面试/求职 | `mvn -pl xiaou-mock-interview -am test` + 页面流程 |
| 学习资产 | `mvn -pl xiaou-learning-asset -am test` + 审核/发布流程 |
| 用户端页面 | 用户端 build + 真实路由 |
| 管理端页面 | 管理端 build + 真实后台路由 |
| 文件上传 | 后端构建 + 上传 + `/api/files/**` 访问 |
| WebSocket | 后端构建 + 用户端构建 + ws-ticket + ACK/失败态 |
| 安全边界 | 701/703/705 + 私有文件 + ws-ticket 复用 + 文件类型 |

## 相关文档

| 文档 | 说明 |
| --- | --- |
| [测试与回归](/guide/testing-regression) | 测试策略 |
| [模块最小回归矩阵](/reference/module-regression-matrix) | 回归测试覆盖 |
| [独立部署](/guide/deploy) | 部署流程 |
| [版本公告与交接模板](/guide/version-release-handoff-template) | 版本交接 |
