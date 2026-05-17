# 测试与回归

这页说明 Code Nest 里“改完之后怎么证明没坏”。它和 [发布前验证](/guide/release-verification) 的区别是：发布前验证告诉你发版前跑什么，本页告诉你日常开发时怎么选择测试层级、怎么补回归样例、怎么记录结果。

## 分层思路

| 层级 | 目的 | 适合场景 |
| --- | --- | --- |
| 编译构建 | 证明代码能编译、依赖能解析 | 每次后端或前端改动 |
| 单元测试 | 证明纯逻辑、状态机、规则计算没坏 | AI、OJ、积分、学习资产、SQL 优化 |
| 集成测试 | 证明 Controller、Service、Mapper、配置能协作 | 接口、数据库脚本、权限 |
| 页面烟测 | 证明真实页面能走通 | 用户端、管理端、上传、聊天 |
| 依赖联调 | 证明外部服务可用 | RAG、go-judge、Redis、MySQL、对象存储 |
| 回归记录 | 证明结果可追溯 | 版本合并、已知问题修复 |

不要把“构建通过”当成“业务可用”。构建只覆盖语法和依赖，不能覆盖业务规则、权限、外部依赖和页面状态。

## 后端基础命令

后端聚合构建：

```powershell
mvn -pl xiaou-application -am clean package -DskipTests
```

按模块跑测试：

```powershell
mvn -pl xiaou-ai -am test
```

```powershell
mvn -pl xiaou-oj -am test
```

```powershell
mvn -pl xiaou-mock-interview -am test
```

```powershell
mvn -pl xiaou-learning-asset -am test
```

```powershell
mvn -pl xiaou-sql-optimizer -am test
```

```powershell
mvn -pl xiaou-system -am test
```

如果只改了某个测试类，可以加 `-Dtest=类名` 缩小范围。

## 现有测试重点

| 模块 | 现有测试方向 |
| --- | --- |
| `xiaou-ai` | Prompt、Schema、RAG、Graph、AI 回归、指标 |
| `xiaou-oj` | 赛事规则、排名、提交、Controller、SQL 脚本 |
| `xiaou-mock-interview` | 模拟面试、求职作战台、Career Loop 状态机 |
| `xiaou-learning-asset` | 学习资产转化、发布、SQL 脚本 |
| `xiaou-sql-optimizer` | SQL 优化服务 |
| `xiaou-system` | AI 配置、回归运行状态 |
| `llamaindex-service` | Python sidecar 接口和服务逻辑 |

前端仓库当前没有统一 `npm test` 脚本。`vue3-user-front/tests` 下有适配器类测试文件，但日常最低验证仍应以 `npm run build` 和真实页面烟测为准。

## AI 回归

AI 场景最容易出现“接口还通，但输出结构变了”的问题。当前 CI 的 AI 回归命令在 `.github/workflows/ai-regression.yml` 中，核心命令是：

```powershell
mvn -pl xiaou-ai -am "-Dtest=AiSceneRegressionEvalTest,InterviewGraphTest,JobBattleGraphTest,SqlOptimizeGraphTest,AiSqlOptimizeServiceImplTest,AiPromptSpecTest,AiStructuredOutputValidatorTest,LlamaIndexClientTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

改这些内容时要跑 AI 回归：

| 变更 | 额外检查 |
| --- | --- |
| Prompt | 场景输出是否仍满足预期 |
| Schema | 必填字段、枚举、数组结构是否稳定 |
| RAG Profile | sidecar 健康、召回内容和空召回兜底 |
| Graph | 节点顺序、失败分支、结构化输出 |
| 模型配置 | 管理端 Runtime、Prompt 调试、回归记录 |

如果新增 AI 场景，要同步 [AI Runtime](/modules/ai-runtime)、[AI Schema 与治理](/reference/ai-schemas) 和回归样例。

## OJ 回归

OJ 至少要覆盖三层：

| 层级 | 验证 |
| --- | --- |
| 规则 | 赛事时间、报名、语言、排名、罚时 |
| 后端 | 题目、测试用例、提交、题解、赛事接口 |
| 外部依赖 | go-judge 可访问，编译器可用 |

常用命令：

```powershell
mvn -pl xiaou-oj -am test
```

页面烟测：

1. 打开 `/oj/problem/:id`。
2. 点运行或自测。
3. 点正式提交。
4. 查看提交详情和状态。

如果 go-judge 未启动，只能说“页面和接口构建已验证”，不能说判题链路已验证。

## 前端回归

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

前端页面烟测至少看：

| 场景 | 检查 |
| --- | --- |
| 新页面 | 路由可打开，刷新不 404 |
| 新接口 | Network URL、状态码、响应结构正确 |
| 登录态 | 未登录、Token 过期、权限不足都有处理 |
| 表单 | 必填、非法输入、提交成功、提交失败 |
| 列表 | 空态、分页、筛选、重置 |
| 上传 | 成功、失败、URL 回显、权限 |
| `v-html` | 走 `renderMarkdown` 或 `escapeHtml + sanitizeHtml` |

前端 `lint` 脚本当前带 `--fix`，会直接改文件。它适合本地整理风格，不适合作为只读 CI 检查口径。

## 文档回归

文档站构建：

```powershell
cd docs-site
npm run build
```

当前 `.github/workflows/docs-site.yml` 会在 `docs-site/**` 或 workflow 变更时执行：

1. Node 20。
2. `npm ci`。
3. `npm run build`。
4. 上传 `docs-site/.vitepress/dist`。

文档改动至少检查：

| 检查 | 说明 |
| --- | --- |
| 内部链接 | VitePress 构建能发现大多数断链 |
| 标题层级 | 页面目录是否清楚 |
| 路线图 | 新增大页要更新 v2.2.0 文档计划 |
| 导航 | 新页面要接入 sidebar 或相关索引 |
| 证据 | 命令、路径、接口不要凭记忆写 |

## 结果怎么记录

验证结果建议写成下面这种格式：

```text
验证范围：用户端聊天 ws-ticket 和消息发送
已执行：用户端 build、后端 package、登录后进入 /chat、发送文本消息
通过项：ws-ticket 获取成功，WebSocket CONNECT 成功，消息 ACK 成功
未验证：多实例广播、禁言到期恢复
依赖状态：Redis 和 MySQL 已启动，未启动监控
```

适合沉淀到：

1. [验证记录与已知问题](/manuals/verified-scenarios)。
2. 对应模块页“验证清单”。
3. PR 描述或版本记录。

## 选择测试的速查表

| 改了什么 | 至少跑什么 |
| --- | --- |
| 只改文档 | `docs-site npm run build` |
| 后端普通业务 | `mvn -pl xiaou-application -am clean package -DskipTests` + 相关接口 |
| AI Runtime | AI 回归命令 + 管理端场景调试 |
| OJ | `mvn -pl xiaou-oj -am test` + go-judge 联调 |
| 模拟面试/求职 | `mvn -pl xiaou-mock-interview -am test` + 页面流程 |
| 学习资产 | `mvn -pl xiaou-learning-asset -am test` + 审核/发布流程 |
| 用户端页面 | 用户端 build + 真实路由 |
| 管理端页面 | 管理端 build + 真实后台路由 |
| 文件上传 | 后端构建 + 上传 + `/api/files/**` 访问 |
| WebSocket | 后端构建 + 用户端构建 + ws-ticket + ACK/失败态 |
