# Code-Nest 全站测试治理计划

## 1. 目标

把 Code-Nest 的测试从“局部回归”升级为“全站分层测试体系”：

- 后端多模块：能按模块、风险和发布阶段运行测试。
- 前端双端：能覆盖 build、路由、API contract、适配器和调试输出约束。
- AI/Agent：能覆盖 prompt、结构化输出、graph、planner、统一运行时和 opt-in live smoke。
- 数据库/脚本/配置：能检查 SQL schema、部署脚本语法、密钥泄漏、NUL 字节和 diff 空白问题。
- CI/CD：最终把稳定的层接入 CI，把慢/外部依赖层保留为手动或定时触发。

## 2. 分层模型

| Tier | 名称 | 默认 CI | 目的 | 典型命令 |
|---|---|---:|---|---|
| T0 | hygiene | yes | 快速检查仓库健康，防止低级问题进入后续测试 | `git diff --check`、secret scan、NUL scan |
| T1 | smoke | yes | 覆盖关键契约和薄边界，几分钟内给出信号 | 前端 `node --test` contract、Agent no-live eval |
| T2 | module | yes/partial | 按模块运行已有单元/服务/SQL 测试 | Maven module test suites |
| T3 | rag | yes/partial | 独立 Python RAG sidecar 的 API contract 和检索行为 | `python -m unittest discover -s tests -v` |
| T4 | integration | manual/nightly | 需要 DB/Redis/WebSocket/文件存储/判题沙箱的集成验证 | Spring/Web/DB integration tests |
| T5 | ai-live | manual | 真实 AI 中转站、RAG 服务或第三方模型联调 | `-LiveAi` smoke |
| T6 | release | release/manual | 发版前构建产物和全量门禁 | backend package、front build、docs build、scripts syntax |

## 3. 当前覆盖快照

### 3.1 后端模块

| 覆盖等级 | 模块 |
|---|---|
| 高 | `xiaou-system`、`xiaou-ai` |
| 中 | `xiaou-oj`、`xiaou-learning-asset`、`xiaou-mock-interview`、`xiaou-filestorage`、`xiaou-points`、`xiaou-chat` |
| 低 | `xiaou-community`、`xiaou-user`、`xiaou-sensitive`、`xiaou-moyu`、`xiaou-sql-optimizer` |
| 空白 | `xiaou-common`、`xiaou-user-api`、`xiaou-application`、`xiaou-interview`、`xiaou-notification`、`xiaou-moment`、`xiaou-sensitive-api`、`xiaou-knowledge`、`xiaou-version`、`xiaou-blog`、`xiaou-codepen`、`xiaou-resume`、`xiaou-plan`、`xiaou-team`、`xiaou-flashcard` |

空白不等于坏，但代表后续补测优先级要显式管理，不能靠“全量 mvn test”假装覆盖。

### 3.2 前端

- `vue3-admin-front/tests` 已有 API contract、Agent UI boundary、路由、request options、no-debug-console 等直接 `node --test` 测试。
- `vue3-user-front/tests` 已有 captcha/moyu contract、career-loop/home/OJ adapter、导航、request options、no-debug-console 等直接 `node --test` 测试。
- 两个前端 `package.json` 均已提供 `test:contracts`，模块内可直接执行 `npm run test:contracts`；`lint` 当前仍带 `--fix`，不作为只读 release 门禁。

### 3.3 AI/Agent

- AI 已有 prompt、RAG query、structured output、graph runner、regression service 测试。
- Agent 已有统一运行时回归脚本 `scripts/agent-runtime-eval.ps1`。
- `AgentRuntimeAllToolsUnifiedEntryTest` 已要求每个注册工具都能通过统一 `AgentChatOrchestrator.chat()` 入口。
- live LLM smoke 使用 `AGENT_LIVE_AI_TEST=true`、`XIAOU_AI_BASE_URL`、`XIAOU_AI_API_KEY`、`XIAOU_AI_CHAT_MODEL`，默认跳过。

## 4. 推荐执行顺序

日常开发：

1. 修改前先定位影响模块和已有测试。
2. 先跑目标模块测试或目标前端 contract test。
3. 修改后重跑同一命令，确认 RED/GREEN 闭环。
4. 跑 `.\scripts\code-nest-eval.ps1 -Tier smoke`。

Agent/AI 修改：

1. 跑目标 AI/Agent 单测。
2. 跑 `.\scripts\agent-runtime-eval.ps1`。
3. 如涉及真实模型联调，再 opt-in 跑 `-LiveAi`。

发版前：

1. 跑 `.\scripts\code-nest-eval.ps1 -Tier backend`。
2. 跑 `.\scripts\code-nest-eval.ps1 -Tier frontend`。
3. 跑 `.\scripts\code-nest-eval.ps1 -Tier release`。
4. 如本次涉及 AI/Agent，追加 live smoke。

RAG sidecar：

1. 首次执行建议创建临时 venv，避免污染系统 Python。
2. 用 `.\scripts\code-nest-eval.ps1 -Tier rag -InstallPythonDeps` 安装并运行测试。
3. 后续可通过 `CODE_NEST_PYTHON` 或 `-PythonCommand` 指向已准备好的 venv。

## 5. 补测优先级

P0 高风险：

- `xiaou-chat`：WebSocket、消息发送、撤回、删除、禁言、限流、在线状态。
- `xiaou-points`：积分、抽奖库存/概率/并发、奖品状态、用户积分流水。
- `xiaou-oj`：判题链路、测试用例、运行沙箱异常、比赛排名。
- `xiaou-filestorage`：上传、路径、配置切换、异常回滚。

P1 核心业务：

- `xiaou-knowledge`：知识图谱 CRUD、节点/边结构、发布/隐藏。
- `xiaou-plan`：打卡、重复规则、连续天数、积分奖励。
- `xiaou-team`：申请、审核、成员角色、权限。
- `xiaou-notification`：通知发送、已读、批量处理。

P2 平台支撑：

- `xiaou-common`：统一响应、分页、异常、工具类、配置属性。
- `xiaou-application`：应用启动 smoke、关键 Bean wiring。
- API-only 模块：至少保留 DTO/contract 或编译门禁。

## 6. 测试命名和准入规则

- 单元测试：`*Test.java`，不依赖外部服务。
- 集成测试：`*IntegrationTest.java`，可依赖 Spring context、DB/Redis mock 或 Testcontainers。
- Web/API 测试：`*WebTest.java`，覆盖 controller contract 和认证边界。
- SQL contract：`*SchemaSqlTest.java`，确保 migration 和字段契约稳定。
- 外部 live：必须用环境变量开关，默认 skipped。
- 新业务模块新增功能时，至少补一个 service 单测或 API contract 测试。

## 7. CI 演进路线

当前 CI：

- 后端只 package 且 `-DskipTests`。
- 前端只 build。
- AI regression 单独按路径触发。
- docs 和脚本有基础构建/语法检查。

建议演进：

1. CI 增加 T0 hygiene 和前端 `node --test` contract。
2. CI 增加后端 fast module tests，不跑 live/external。
3. AI workflow 扩大到完整 prompt/structured output/graph regression。
4. nightly 跑 integration/release tier。
5. release 分支强制 `Tier release`。

## 8. 当前统一入口

第一版统一入口：

```powershell
.\scripts\code-nest-eval.ps1 -Tier smoke
.\scripts\code-nest-eval.ps1 -Tier backend
.\scripts\code-nest-eval.ps1 -Tier frontend
.\scripts\code-nest-eval.ps1 -Tier rag
.\scripts\code-nest-eval.ps1 -Tier release
```

Windows Bash 说明：

- `release` tier 会检查 `scripts/deploy-release.sh` 语法。
- Windows 下会优先使用 Git Bash，避免误用 `C:\Windows\system32\bash.exe` 的 WSL shim。
- 如需指定路径，可设置 `$env:CODE_NEST_BASH` 或传 `-BashCommand`。

RAG 临时 venv 示例：

```powershell
python -m venv .tmp\code-nest-rag-venv
$env:CODE_NEST_PYTHON=(Resolve-Path ".tmp\code-nest-rag-venv\Scripts\python.exe").Path
.\scripts\code-nest-eval.ps1 -Tier rag -InstallPythonDeps
```

Agent live：

```powershell
$env:XIAOU_AI_API_KEY="<real-api-key>"
.\scripts\code-nest-eval.ps1 -Tier agent -LiveAi -AiBaseUrl "https://yaoshanapi.com/v1" -AiChatModel "gpt-5.5"
```

## 9. 下一步

- 给两个前端补 `test:contracts` script。
- 给 CI 增加 T0/T1。
- 从 P0 高风险模块开始补缺失测试，优先 `xiaou-chat`、`xiaou-points`、`xiaou-oj`、`xiaou-filestorage`。
- 每补一个模块，把命令加入 `scripts/code-nest-eval.ps1` 对应 tier。

## 10. 已验证基线

截至 2026-07-09：

- `hygiene`：通过。secret scan 已过滤测试占位值，真实密钥仍拦截。
- `frontend`：通过，46 个 Node contract tests 全绿。
- `ai`：通过，53 个 Java AI regression tests 全绿。
- `rag`：通过，临时 venv 安装依赖后 10 个 unittest 全绿。
- `smoke`：通过，前端 46 个 contract tests 与 Agent no-live eval 全绿。
- `backend`：通过，Maven reactor 23 个模块 SUCCESS，用时约 2 分 38 秒。
- `release`：通过，后端 package、双前端 build、docs build、Python/Bash 脚本语法检查均通过。
- `agent-live`：通过，真实 `gpt-5.5` 执行工具目录和运行时状态两个只读工具，并在随机临时 MySQL 库完成解除禁言写入、强确认和 `EXECUTED` 审计闭环。

当前主要剩余缺口不是“已有测试能不能跑”，而是“部分业务模块还没有足够的业务断言”。下一阶段应优先把空白/低覆盖模块纳入可重复测试矩阵，而不是继续扩散临时脚本。

非阻断观察项：

- Maven 每轮提示 profile `rdc` 不存在，需要确认是否来自本机 Maven 配置或 CI 配置。
- Vite/VitePress build 有 chunk size 与 Rollup PURE 注释提示，当前不阻断。
- Windows 控制台仍可能对部分 ANSI/UTF-8 输出显示乱码，测试结果不受影响。
- 写操作 live 验收采用随机临时 MySQL 库和专用 fixture，测试结束删除数据库，不修改现有业务库。
