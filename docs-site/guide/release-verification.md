# 发布前验证

Code Nest 是多模块后端、两个 Vue 前端、独立文档站和多个可选 sidecar 组成的项目。发布前验证不要只跑一个命令，而要按“改了哪里、影响哪些链路、哪些依赖必须在线”来分层执行。

本页给新维护者一个可直接照抄的验证顺序。它不是要求每次都跑满全量矩阵，而是帮助你用合适的成本覆盖最大的风险。

如果你不是从“变更类型”切入，而是已经知道自己主要改的是某个模块，可以先看 [模块最小回归矩阵](/reference/module-regression-matrix) 再回到本页组合发布前验证项。

如果你已经知道这次最危险的不是成功路径，而是某类失败态，比如权限拒绝、文件 `403`、WebSocket `ERROR`、AI 结构缺失或 OJ 卡状态，可以先对照 [异常路径与失败态索引](/reference/failure-paths) 把补测口径列出来。

## 怎么使用这页

发布前可以按这个顺序走：

1. 先看 `git diff --stat`，确认这次到底改了文档、后端、用户端、管理端还是部署配置。
2. 在“先判断变更类型”里找到对应行，把最低验证项列出来。
3. 如果一次提交同时改了多个域，就把这些验证项合并，不要只跑其中一个。
4. 依赖没有启动时，不要把链路写成“已验证”，而要写清楚“构建已通过，依赖未联通”。
5. 验证通过后，再同步模块页、参考索引、版本历史和验证记录。

一个简单例子：如果你改了聊天室后端限流和用户端聊天页面，就至少要跑后端构建、用户端构建、WebSocket ws-ticket 连接、消息发送、限流失败态，并把结论写回 [验证记录与已知问题](/manuals/verified-scenarios)。

## 先判断变更类型

| 变更范围 | 最低验证 |
| --- | --- |
| 只改 `docs-site/**` | `docs-site npm run build` |
| 只改用户端页面 | `vue3-user-front npm run build`，再手工打开对应路由 |
| 只改管理端页面 | `vue3-admin-front npm run build`，再手工打开对应路由 |
| 改后端 Controller、Service、Mapper、配置 | `mvn -pl xiaou-application -am clean package -DskipTests`，再跑相关接口或页面 |
| 改 AI Runtime、Prompt、Schema、Graph、RAG | AI 单元/回归测试、管理端 AI 配置页、必要时 RAG sidecar |
| 改 OJ 判题 | 后端构建、go-judge 健康、至少一次 `/oj/run` 或 `/oj/test` |
| 改聊天 | 后端构建、用户端构建、WebSocket ws-ticket 连接、消息发送和失败态 |
| 改文件上传 | 后端构建、上传接口、`/api/files/**` 访问、业务归属页面回流 |

如果一次提交同时覆盖多个范围，取这些验证项的并集。

## 基础构建命令

后端聚合构建：

```powershell
mvn -pl xiaou-application -am clean package -DskipTests
```

用户端构建：

```powershell
cd vue3-user-front
npm run build
```

管理端构建：

```powershell
cd vue3-admin-front
npm run build
```

文档站构建：

```powershell
cd docs-site
npm run build
```

前端项目也有 `npm run lint`，但当前脚本带 `--fix`，会直接修改文件。它适合本地整理代码风格，不适合作为只读检查命令直接放进发布脚本里。

## 后端测试口径

当前仓库里已经有一批 JUnit 5 和 Mockito 测试，重点集中在 AI Runtime、OJ、求职作战台、学习资产、SQL 优化和系统 AI 配置等高风险模块。改这些模块时，优先跑对应模块测试。

常见命令：

```powershell
mvn -pl xiaou-ai -am test
```

```powershell
mvn -pl xiaou-oj -am test
```

```powershell
mvn -pl xiaou-mock-interview -am test
```

如果只想复用现有 AI 回归 CI 的测试集合，可以参考 `.github/workflows/ai-regression.yml` 中的命令：

```powershell
mvn -pl xiaou-ai -am "-Dtest=AiSceneRegressionEvalTest,InterviewGraphTest,JobBattleGraphTest,SqlOptimizeGraphTest,AiSqlOptimizeServiceImplTest,AiPromptSpecTest,AiStructuredOutputValidatorTest,LlamaIndexClientTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

后端验证要注意两点：

1. `-DskipTests` 只能证明能编译和打包，不能证明业务逻辑没坏。
2. 涉及数据库、Redis、AI、go-judge 的链路，需要启动依赖后做接口或页面级验证。

## 核心业务烟测

每次版本合并前，建议至少抽测这些链路：

| 链路 | 入口 | 关键预期 |
| --- | --- | --- |
| 用户登录 | `/login`、`/profile` | 登录后能读取当前用户资料 |
| 管理端登录 | `/login`、`/dashboard` | 管理端 Token 和菜单正常 |
| 文件上传 | 头像、聊天图片或文件管理页 | 上传成功，返回 URL 可访问 |
| 内容发布 | 社区、博客、动态、CodePen 任一写操作 | 用户端回流，管理端可见 |
| OJ | `/oj/problem/:id` | 题面展示，运行或自测可返回状态 |
| 聊天 | `/chat` | 先拿 ws-ticket，再连接 WebSocket，发送消息有 ACK |
| 通知 | `/notification` | 未读数、详情、已读动作正常 |
| AI 配置 | `/system/ai-config` | Runtime 面板可打开，配置脱敏展示 |
| 文档站 | `docs-site/.vitepress/dist` 或 preview | 首页、搜索、深层路由可打开 |

完整的业务链路可以参考 [核心链路教程](/manuals/core-workflows)，历史截图和已知问题可以参考 [验证记录与已知问题](/manuals/verified-scenarios)。

## AI 回归

AI 场景最容易出现“接口还通，但结构化输出变形”的问题。改 Prompt、Schema、Graph Runner、RAG Profile 或模型配置时，至少做三层检查：

| 层级 | 怎么查 |
| --- | --- |
| Catalog | `GET /admin/ai/config/schema-catalog`，确认 Prompt、RAG、Schema 仍可被管理端发现 |
| 单场景调试 | 管理端 `/system/ai-config` 的 Prompt 调试或 RAG 调试 |
| 回归 | `POST /admin/ai/config/regression/run` 或运行 AI 回归测试 |

如果新增 AI 场景，要同步更新：

1. Prompt Catalog。
2. 结构化输出 Schema。
3. RAG query/profile。
4. `scene-regression-cases.json`。
5. [AI Runtime](/modules/ai-runtime) 和 [AI Schema 与治理](/reference/ai-schemas)。

## CI 工作流

当前仓库已有两个 GitHub Actions 工作流：

| Workflow | 触发 | 做什么 |
| --- | --- | --- |
| `.github/workflows/docs-site.yml` | `docs-site/**` 或 workflow 变更 | Node 20、`npm ci`、`npm run build`，上传文档站产物 |
| `.github/workflows/ai-regression.yml` | `pom.xml`、`xiaou-common/**`、`xiaou-ai/**` 或 workflow 变更 | Java 17，运行 AI 相关测试集合，失败时上传 Surefire 报告 |

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
| 新版本发布 | [版本历史](/modules/version-history)、[v2.2.0 文档计划](/roadmap/v2.2.0-docs-plan) |

## 发布前最终清单

| 检查项 | 预期 |
| --- | --- |
| Git 状态 | 只有本次变更，或者已明确哪些是用户已有改动 |
| 后端构建 | 受影响模块或聚合应用构建通过 |
| 前端构建 | 受影响前端构建通过 |
| 文档构建 | `docs-site npm run build` 通过 |
| 关键链路 | 至少一个真实页面或接口完成烟测 |
| 外部依赖 | AI、RAG、go-judge、Redis、MySQL 的依赖状态已说明 |
| 文档索引 | 路由、表、错误码、模块页已同步 |
| 已知问题 | 新发现的问题已写入验证记录或模块常见坑 |

## 常见误区

| 误区 | 更稳的做法 |
| --- | --- |
| 后端能打包就认为功能正常 | 打包只能证明编译通过，业务链路还要跑接口或页面 |
| AI 接口返回 `200` 就认为场景可用 | 还要检查结构化输出字段、Schema 校验、RAG 召回和回归样例 |
| 前端能打开首页就认为发布可用 | 要打开本次影响的真实路由，确认接口、权限、空态和失败态 |
| 文档构建通过就认为说明完整 | 还要确认模块页、API 索引、表索引、错误码和版本记录是否同步 |
| 外部依赖没启动时跳过不写 | 应记录“未验证原因”和下一次补测入口，避免后来的人误判 |

如果你正在长期维护某个模块，发版前验证最好不要只按本页临时判断，继续配合 [模块值守与回归手册](/guide/module-owner-playbook) 把模块自己的最小回归组固定下来。
