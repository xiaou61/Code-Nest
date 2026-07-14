# Code-Nest 全站测试结果报告

## 结论

截至 2026-07-14，Code-Nest 全站基础门禁、Agent 统一后端回归以及当前 26 个已注册 Agent 工具的真实 AI live 验收均已跑通。

这里的“26 个工具全通过”只表示当前 `AgentToolRegistry` 中的全部生产工具已覆盖，不表示尚未注册为 Agent 工具的全站业务接口已经能被自然语言调用。

真实 API Key 只通过测试进程环境变量注入，没有写入配置、源码或测试报告。

## 汇总

| 测试层 | 结果 | 覆盖内容 | 备注 |
|---|---|---|---|
| `hygiene` | 通过 | `git diff --check`、secret scan、NUL scan | secret scan 已过滤测试占位值，真实密钥仍会失败 |
| `frontend` | 通过 | 双前端 Node contract tests | 共 46 个测试通过 |
| `ai` | 通过 | Java AI regression tests | 共 54 个测试通过 |
| `rag` | 通过 | `llamaindex-service` Python unittest | 临时 venv 安装依赖后 10 个测试通过 |
| `smoke` | 通过 | 前端 contract + Agent no-live eval | 日常最快全站入口已可用 |
| `backend` | 通过 | Maven 多模块测试 | Reactor 23 个模块 SUCCESS；最新复验约 2 分 33 秒，包含 `xiaou-filestorage` 10 个、`xiaou-oj` 20 个、`xiaou-points` 36 个和 `xiaou-chat` 38 个测试 |
| `release` | 通过 | 后端 package、双前端 build、docs build、脚本语法检查 | 首次因 Bash 环境失败，修复后重跑通过 |
| `agent-live` | 通过 | 真实 `gpt-5.5` planner + 完整 26 工具 Registry + 真实只读/写入工具执行 | 全工具 live acceptance 独立通过两次；最新完整运行 `26/26`，写工具包含隔离 MySQL、强确认、审计和结果回查 |

封版基线更新为 `v2.4.0` 后再次执行统一 `all` 门禁和 `release` 门禁，分别以退出码 `0` 通过；最终产物包含 `xiaou-application-v2.4.0.jar`、管理端 dist、用户端 dist 和 VitePress dist。

## Agent 真实后端验收结果

本轮真实验收不是只探测网关，也没有用模型 fixture 代替 planner。测试通过真实 `AgentChatOrchestrator.chat()` 发送自然语言，由真实 `gpt-5.5` 选择工具并执行生产 `AgentTool`。API Key 只进入测试进程，报告和仓库均不保存明文。

| 验收层 | 结果 | 真实断言 |
|---|---|---|
| OpenAI 兼容网关 | 通过 | `/v1/models` 返回 `gpt-5.5`；`/v1/chat/completions` 返回预期文本。根地址返回站点 HTML，因此 Base URL 必须包含 `/v1` |
| 真实 smoke | `2/2` 通过 | 共完成 3 次真实 planner 请求；正确执行 `system.agent.tools.list` 和 `system.agent.runtime.status`，严格 planner 场景无 deterministic fallback |
| 完整生产工具目录 | `26/26` 通过 | 每个注册工具至少有一个自然语言 live 场景；工具名、状态、trace 和 artifact 均通过断言 |
| 解除禁言 | 通过 | `chat.userBan.unban` 返回 `confirm_required -> executed`；预览阶段数据未变化，确认后禁言状态和审计终态均回查成功 |
| 清理过期日志 | 通过 | `system.operationLog.cleanExpired` 返回 `confirm_required -> executed`；只删除 30 天前数据，近期日志保留 |
| 缺少必填输入 | 通过 | “解除禁言但不提供用户编号”返回 `PLAN_CLARIFICATION_REQUIRED`，没有执行写入 |

完整 26 工具 live acceptance 已独立通过两次。最新一次 Surefire 结果为 `1/1`、零失败、零错误，耗时 `1075.929s`；耗时主要来自外部网关多次 `502/524` 后的真实重试，最终没有走本地 fallback。

一次修复前的组合脚本运行中，完整 26 工具 live acceptance 已通过，但同一 Maven 进程里的 smoke 因禁用了网络重试而超时，后端结果为 `211/212`。该次运行没有记为全绿；将 smoke 调整为与生产一致的 60 秒读取超时和 2 次真实 HTTP 重试后，smoke `2/2` 通过，随后无 live 的统一回归也打印 `all checks passed`。

写操作验收使用随机命名的临时 MySQL 数据库，加载真实 Chat、Operation Log 和 Agent Audit MyBatis Mapper，调用真实业务 Service、真实写工具与真实审计服务。测试无论成功或失败都会在 `finally` 删除整个临时数据库，不修改现有 `code_nest` 业务库。

写操作还验证了以下运行轨迹：

- 两个预览响应均为 `confirm_required`，审计状态为 `PREVIEW`，业务 fixture 均未变化。
- 确认文本必须分别精确匹配工具声明的 `确认解除禁言` 和 `确认清理操作日志`。
- 确认后 trace 包含 `audit.confirmed=done`、`tool.executed=done`、`audit.result_recorded=done`。
- 最终禁言记录 `status=0`；过期日志删除、近期日志保留；两条审计均为 `EXECUTED` 并记录结果 JSON。

真实 live 目标测试和完整 Agent 回归均通过：

```powershell
$env:XIAOU_AI_BASE_URL = "https://<openai-compatible-gateway>/v1"
$env:XIAOU_AI_API_KEY = "<real-api-key>"
$env:XIAOU_AI_CHAT_MODEL = "gpt-5.5"
mvn -pl xiaou-system -am "-Dtest=AgentRuntimeLiveAiSmokeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\scripts\agent-runtime-eval.ps1 -LiveAi -AiBaseUrl $env:XIAOU_AI_BASE_URL -AiChatModel $env:XIAOU_AI_CHAT_MODEL
.\scripts\agent-runtime-eval.ps1 -LiveWrite -AiBaseUrl $env:XIAOU_AI_BASE_URL -AiChatModel $env:XIAOU_AI_CHAT_MODEL `
  -MysqlUrl $env:AGENT_TEST_MYSQL_URL -MysqlUsername $env:AGENT_TEST_MYSQL_USERNAME `
  -MysqlPassword $env:AGENT_TEST_MYSQL_PASSWORD
```

报告中的命令刻意不包含 API Key。执行时应使用 `XIAOU_AI_API_KEY` 环境变量或临时参数注入。

## 写操作验收基座扩展状态

写操作 live 测试已从解除禁言单场景重构为通用 `AgentLiveWriteAcceptanceHarness`。基座统一负责真实 AI planner、临时 MySQL 数据库生命周期、MyBatis Mapper 装载、统一编排器以及 `PREVIEW -> CONFIRMED -> EXECUTED` 审计断言；业务场景只提供最小表结构、fixture 和业务结果断言。

当前同一个 `AgentToolRegistry` 同时注册 `chat.userBan.unban` 与 `system.operationLog.cleanExpired`，避免只有一个候选工具时的弱路由测试。新增操作日志场景会创建一条 45 天前的过期日志和一条 2 天内的近期日志，预览阶段断言两条记录都不变，确认后断言只删除超过 30 天的记录。

本轮已验证：

- `OperationLogListAgentToolTest` 共 5 个测试通过，覆盖清理请求解析、破坏性风险声明、预览零写入和类型化 Service 执行。
- `AgentRuntimeLiveWriteAcceptanceTest` 默认模式编译通过并按 opt-in 约定跳过真实外部调用。
- 完整 `scripts/agent-runtime-eval.ps1` 无 live 模式通过，两个写工具都从统一聊天入口进入 `confirm_required`。
- 完整 26 工具 `AgentRuntimeLiveWriteAcceptanceTest` 已使用真实 `gpt-5.5` 与隔离 MySQL 独立通过两次；最新 Surefire 结果为 `1/1`，零失败、零错误、零跳过。
- 测试后独立查询 `information_schema`，`code_nest_agent_it_%` 临时数据库残留数量为 0；进程环境中的 AI Key、MySQL 密码和 live 标志均已清除。

## 统一自然语言规划器回归结果

- `LlmAgentPlanResolver` 现在优先把自然语言、会话上下文和完整工具 schema 交给 LLM；只有模型不可用、响应无效或没有形成计划时，才调用 deterministic resolver 兜底。
- `AgentTool.resolve()` 改为可选默认能力，新工具只实现 definition/schema、预览和执行即可被 LLM 规划，不再被接口强制要求维护关键词匹配。
- 当前生产工具目录包含 26 个 `AgentTool` Bean；`AgentRuntimeAllToolsUnifiedEntryTest` 使用完整目录和统一 `AgentChatOrchestrator.chat()` 验证每个工具的计划、风险、确认/执行状态。
- 本轮 Agent tier 通过：后端 Agent 回归 215 个测试（3 个 live 用例按 opt-in 跳过），前端统一聊天薄边界 11 个测试通过。
- `AgentRuntimeLiveAiSmokeTest` 使用完整 26 工具 Registry 和真实 `gpt-5.5`；最终 `2/2` 通过，共完成 3 次真实 planner 请求，未使用 deterministic fallback。

## 统一 HTTP 入口回归结果

- `AgentChatControllerTest` 当前 `5/5` 通过；用例使用 `MockMvc` 将 JSON 请求真实绑定到 `POST /admin/agent/chat`，并验证超长消息在进入编排器前返回 `400`。
- 该用例经过真实 `AgentChatController`、`LlmAgentPlanResolver`、`AgentToolRegistry`、`AgentPolicyEngine` 和 `AgentChatOrchestrator`，只替换模型响应 fixture、管理员服务和审计外部依赖。
- 测试工具只声明 definition/schema、预览和执行，没有实现关键词 `resolve()`；自然语言请求仍由 planner 候选驱动，schema 外字段在执行前被过滤。
- 响应断言覆盖统一 `Result` 包装、会话 ID、状态、工具名、trace、结构化 artifact、管理员权限和请求长度边界。会话存储回归同时确认相同 `sessionId` 在不同管理员下互相隔离，无归属审计不能继续确认。
- 最新无 live `scripts/agent-runtime-eval.ps1` 通过：Agent 后端 `215` 个测试零失败、`3` 个 live 用例按 opt-in 跳过，AI Prompt/Schema `19/19`、前端薄边界 `11/11` 通过，hygiene 检查通过。

## 积分抽奖后端回归结果

`xiaou-points` 已进入统一 backend tier，不再是仅编译、无业务断言的模块。本轮新增两个测试类，共 36 个测试，零失败、零错误、零跳过：

- 风控责任链 16 个测试，覆盖固定链序、黑名单、积分缺失、全局/用户/IP 限流、Redis fail-open、限流器初始化和多档冷却时间。
- `LotteryServiceImpl` 20 个测试，覆盖熔断、用户锁、风控短路、原子扣分、策略调用、库存扣减、奖励发放、记录与事件顺序、有限库存补偿、无限库存不补偿、中断标记、历史记录契约、统计和剩余次数边界。
- JaCoCo 结果：`LotteryServiceImpl` 行覆盖 `186/187`，核心 `draw()` 行覆盖 `37/37`、分支覆盖 `10/10`；六个风控链核心类行覆盖 `83/83`、分支覆盖 `43/48`。

测试按 RED -> 最小修复 -> GREEN 发现并修复四类问题：空积分值触发 NPE、风控链返回 `false` 后仍继续抽奖、立即响应与历史响应字段不一致、旧限制记录的今日次数为空时剩余次数接口触发 NPE。立即响应和历史响应现共用同一个转换路径，成本、净收益、中奖状态、策略、IP 和设备契约保持一致。

## 聊天消息后端回归结果

`xiaou-chat` 已从无模块测试状态进入统一 backend tier。本轮为 `ChatMessageServiceImpl` 新增 38 个测试，零失败、零错误、零跳过，覆盖文本/图片输入白名单、禁言短路、消息写入与回读、回复摘要、历史顺序、撤回窗口、单删/批量删除和系统公告。

- JaCoCo 结果：`ChatMessageServiceImpl` 行覆盖 `126/128`（98.4%），分支覆盖 `87/94`（92.6%）；未覆盖的两行仅是管理端分页委托。
- 消息写入与完整消息回读现在处于同一事务；回读为空会抛业务异常并回滚，不再把 `null` 交给 WebSocket 调用方。
- 回复消息必须属于当前聊天室且未被软删除，避免跨房间或已删除内容重新进入消息摘要。
- 图片 URL 的有效上限始终不超过数据库 `VARCHAR(500)`，即使外部配置误设得更大也会在访问房间和 Mapper 前拒绝。
- 已软删除消息按不可见处理，不能重复撤回；批量删除影响 0 行会明确失败，不再记录伪成功。

本轮没有声称整个聊天模块完全覆盖。WebSocket 会话、在线状态、聊天限流与禁言管理的独立回归仍属于后续测试范围。

## 文件存储策略回归结果

`xiaou-filestorage` 本轮模块测试共 `10/10` 通过且无失败、无错误、无跳过：`FileStorageServiceImplTest` 4 个测试、`LocalStorageStrategyTest` 5 个真实临时文件系统测试，以及 1 个共享策略生命周期契约测试。

- 正常链路覆盖流式上传、目录创建、下载、存在性、URL、复制、大小查询和删除。
- RED 测试确认本地策略的八类操作直接拼接 `basePath` 与用户路径，`../outside.txt` 可以越界读写；绝对路径、符号链接和 `.` 根路径也分别形成边界风险。
- GREEN 修复将基目录统一规范化为绝对真实路径，所有上传、下载、删除、存在性、URL、复制和大小操作共用同一个安全解析器。
- 解析器拒绝绝对路径、规范化后越界路径、基础目录本身和路径中的符号链接；非法用户路径按现有接口契约返回失败、`null` 或 `false`。
- JaCoCo：`LocalStorageStrategy` 行覆盖 `75/119`（63.0%），分支覆盖 `16/28`（57.1%）；剩余未覆盖主要是初始化失败、连接失败和底层 I/O 异常分支，不代表本地路径边界未测试。
- 共享 `AbstractFileStorageStrategy` 契约验证重新初始化抛异常后会立即失效，后续 `testConnection()` 不再使用旧的可用状态；其 JaCoCo 行覆盖 `20/35`（57.1%）、分支覆盖 `5/10`（50.0%）。
- 非法路径日志从完整异常堆栈收敛为简短 `WARN`，系统 I/O 故障仍保留 `ERROR` 堆栈，避免把可预期输入拒绝放大为日志噪音。

## OJ 判题服务回归结果

`xiaou-oj` 本轮将 `JudgeServiceTest` 扩展到 10 个测试，模块全测试共 `20/20` 通过且无失败、无错误、无跳过。

- RED 测试确认 go-judge 返回空 `stderr`、但 `error` 字段包含真实原因时，编译错误和运行错误都会被记录成空字符串，用户无法看到沙箱故障原因。
- GREEN 修复将编译阶段和运行阶段的错误提取统一到 `getErrorMessage`：优先使用非空 stderr，否则回退到 error 字段。
- accepted 链路同时验证了编译缓存文件传递、逐用例输出比较、耗时/内存统计、提交状态更新和缓存清理，避免修复错误信息时破坏判题主链路。
- 超时和超内存场景验证判题会立即停止，且保留已通过用例数、总用例数、最大耗时和最大内存。
- RED 测试确认 `.strip()` 会同时忽略前导空白，与“只忽略末尾空白”的比较契约不一致，导致本应答案错误的输出被判为通过；现改为 `stripTrailing()`。
- 新增首次 AC 计数/积分副作用隔离、计数或积分异常不污染 accepted 主结果、运行异常转 `SYSTEM_ERROR` 并清理编译缓存的回归场景。
- JaCoCo：`JudgeService` 行覆盖 `116/123`（94.3%），分支覆盖 `40/54`（74.1%）。

## 已执行命令

```powershell
.\scripts\code-nest-eval.ps1 -Tier hygiene
.\scripts\code-nest-eval.ps1 -Tier frontend
.\scripts\code-nest-eval.ps1 -Tier ai
.\scripts\code-nest-eval.ps1 -Tier rag
.\scripts\code-nest-eval.ps1 -Tier smoke
.\scripts\code-nest-eval.ps1 -Tier backend
.\scripts\code-nest-eval.ps1 -Tier release
mvn -pl xiaou-points -am org.jacoco:jacoco-maven-plugin:prepare-agent test org.jacoco:jacoco-maven-plugin:report
mvn -pl xiaou-chat -am org.jacoco:jacoco-maven-plugin:prepare-agent test org.jacoco:jacoco-maven-plugin:report
mvn -pl xiaou-filestorage -am test -q
mvn -pl xiaou-filestorage -am org.jacoco:jacoco-maven-plugin:prepare-agent test org.jacoco:jacoco-maven-plugin:report -q
mvn -pl xiaou-oj -am test -q
mvn -pl xiaou-oj -am org.jacoco:jacoco-maven-plugin:prepare-agent test org.jacoco:jacoco-maven-plugin:report -q
```

RAG sidecar 使用临时虚拟环境验证：

```powershell
python -m venv .tmp\code-nest-rag-venv
$env:CODE_NEST_PYTHON=(Resolve-Path ".tmp\code-nest-rag-venv\Scripts\python.exe").Path
.\scripts\code-nest-eval.ps1 -Tier rag -InstallPythonDeps
```

前端包内测试入口也已验证：

```powershell
npm --prefix vue3-admin-front run test:contracts
npm --prefix vue3-user-front run test:contracts
```

## 中途发现并修复的问题

| 问题 | 处理结果 |
|---|---|
| 中转站根地址返回管理站点 HTML，模型接口被误判为不可用 | 明确使用带 `/v1` 的 OpenAI 兼容 Base URL；真实 `/v1/models` 和 `/v1/chat/completions` 均验证通过 |
| 推理模型没有 completion 上限，大工具目录 Prompt 可持续占用连接直到超时 | 增加全局 `XIAOU_AI_MAX_COMPLETION_TOKENS`，并支持 `AiPromptSpec` 声明场景级预算；管理员 planner 使用 512 |
| “预演某请求”可能直接执行内层目标工具 | planner 统一增加外层主动作规则；预演、预检、校验、解释和恢复分析优先选择元工具，不按具体工具硬编码 |
| 客户端 `sessionId` 直接作为共享存储键，不同管理员同名会话可能串读上下文 | Repository 存储键统一增加管理员 ID 命名空间，响应仍保留原始 `sessionId`；新增双管理员隔离回归 |
| 统一聊天请求缺少长度边界，空归属历史审计仍可进入确认判断 | DTO 增加四个字段长度上限并在 Controller 启用 `@Valid`；审计归属改为严格匹配，空归属直接拒绝 |
| 指标健康验收语句同时要求“告警总览”，与运行时观测工具语义重叠 | 收紧 live fixture 的验收意图，明确只检查错误、阻断和慢调用信号；生产路由未增加测试专用分支 |
| live smoke 将网络重试设为 0，网关偶发超时会直接阻断整套回归 | 保持 deterministic fallback 关闭，但传输层对齐生产的 60 秒读取超时和 2 次重试 |
| secret scan 误报测试占位值 `secret-key` | 已改成命中后过滤测试/文档占位值，真实长随机密钥仍拦截 |
| RAG unittest 首次缺 `fastapi` | 新增 `rag` tier、`-PythonCommand`、`-InstallPythonDeps`，并用临时 venv 跑通 |
| Windows 默认 `bash` 指向 WSL shim，导致 release 失败 | 已改成优先解析 Git Bash，并支持 `CODE_NEST_BASH` / `-BashCommand` |
| 前端缺包内标准测试命令 | 已给两个前端补 `test:contracts` |
| 抽奖风控链返回 `false` 时服务仍继续执行 | 服务入口现在显式检查布尔结果并立即短路 |
| 抽奖立即响应与历史响应字段不一致 | 两条路径改为复用同一个记录转换函数 |
| 积分余额或今日抽奖次数为空时触发 NPE | 在业务读取边界按 0 归一，并增加回归测试 |
| 聊天消息插入后回读为空仍返回 `null`，且写入/回读无事务 | 增加事务边界；回读失败抛业务异常并回滚 |
| 回复可引用其他房间或已删除消息 | 回复元数据只接受当前房间内可见消息 |
| 图片 URL 默认/可配置上限超过数据库列长度 | 有效上限收敛到 500，并在外部查询前校验 |
| 已删除消息可重复撤回、批量删除 0 行仍成功 | 在服务状态边界显式拒绝并增加回归测试 |
| 本地存储路径可通过 `..`、绝对路径或符号链接越过配置目录 | 所有本地策略操作统一经过规范化、包含性和符号链接校验；增加真实临时目录回归测试 |
| go-judge 空 `stderr` 覆盖真实 `error`，导致编译/运行失败原因丢失 | 编译和运行阶段统一使用非空 stderr 优先、error 回退的错误提取逻辑，并增加判题回归测试 |
| 判题输出比较使用 `.strip()`，错误忽略前导空白 | 改为只去除末尾空白，并增加前导空白差异的 WRONG_ANSWER 回归测试 |

## 非阻断观察项

- Maven 每轮提示 requested profile `rdc` 不存在，目前不影响构建和测试。
- Vite/VitePress build 有 chunk size 与 Rollup PURE 注释警告，目前不阻断 release。
- Windows 控制台对部分 ANSI/UTF-8 输出仍可能显示乱码，但测试结果不受影响。
- 部分业务模块仍属于低覆盖或空白覆盖，当前“通过”代表已有测试和构建门禁通过，不代表业务断言已经完全充分。

## CI 门禁接入

主 CI 已复用统一测试入口，不在 workflow 中维护第二份 Java 测试清单：

- Backend job 在 package 前执行 `scripts/code-nest-eval.ps1 -Tier backend`，失败时上传 Surefire 报告。
- Admin/User frontend matrix 在 build 前分别执行包内 `test:contracts`。
- 新增独立 RAG job，使用 Python 3.11、pip cache 和统一 `rag -InstallPythonDeps` 入口。
- Scripts job 执行跨平台 `hygiene`、Python 语法检查和 Bash 语法检查。
- `ci-summary` 现在同时依赖 backend、frontend、docs、rag、scripts；workflow 权限收敛为 `contents: read`。
- 真实 AI Key 与 MySQL live 测试不进入普通 CI，继续保持手动 opt-in，避免长期凭据和外部服务波动阻断提交。

本地验证结果：PyYAML 结构检查通过，官方 `actionlint v1.7.12` 语义检查通过，backend 23 模块 `BUILD SUCCESS`，`xiaou-filestorage` `10/10`、`xiaou-oj` `20/20`、`xiaou-points` `36/36`、`xiaou-chat` `38/38` 通过，双前端 contract `19 + 27` 通过，RAG `10` 通过，hygiene 通过。GitHub-hosted runner 的首次远端结果需要在 workflow 推送后确认，本报告未将本地结果冒充为远端运行结果。

## 尚未执行项

| 项目 | 原因 |
|---|---|
| GitHub-hosted runner 首次 CI | workflow 已完成本地结构、语义和命令验证，需推送后确认远端环境 |
| Redis/对象存储/判题沙箱集成测试 | 当前已覆盖隔离 MySQL，其他外部依赖层后续单独治理 |

## 下一步建议

1. 推送后观察首次远端 CI，并将 `CI Summary` 设置为分支保护 required check。
2. 继续从低覆盖高风险模块补测试：优先推进文件存储其他云策略和 Redis/对象存储/判题沙箱集成边界；本轮已补齐 OJ 首次 AC、副作用隔离、系统异常清理、错误回退、资源限制和答案比较。
3. 后续新增写工具直接复用通用基座，只补业务 schema、fixture 和结果断言，不再复制数据库和确认链代码。
