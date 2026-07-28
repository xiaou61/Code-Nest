# 变更日志

本项目遵循面向读者的变更记录方式，按版本归档重要功能、修复、工程治理、兼容性变化和迁移提示。

格式参考 [Keep a Changelog](https://keepachangelog.com/) 的结构，并结合本项目实际使用 `vX.Y.Z` 版本号。

## [Unreleased]

## [v2.5.1] - 2026-07-28

### Added

- 新增 SRE Recording Rules、Outbox/评测积压与任务年龄告警、队列终态/deadline/租约恢复告警，以及 RCA 失败率和耗时告警。
- 新增自动 provision 的应用生产 Dashboard 与 SRE Runtime Dashboard；数据源使用稳定 UID，零流量时指标保持真实零值。
- 新增幂等 `DEPLOYMENT_SNAPSHOT` 和 `RUNBOOK_SNAPSHOT` 证据；运行手册来自 Jar 内固定目录，构建来源只读发布配置。
- 新增默认只报告的服务器容量治理脚本、每周 systemd timer、生产基线校验、GitHub 外部 uptime 探针和受控 Alertmanager 合成演练。
- 新增确定性队列恢复验证，覆盖 lease 恢复、deadline、最大尝试失败、对应指标，以及恢复后跳过已完成用例。

### Changed

- release bundle 现在同时携带 JAR、双前端、RELEASE 元数据和白名单运维资产；部署会校验 Nginx/Prometheus、启动 Grafana、刷新 systemd，并在失败时恢复全部受管文件。
- 发布脚本只更新应用环境中的三项 SRE 构建溯源，不覆盖监控 `.env`、Alertmanager 本地配置、targets 或 secrets。
- 发布备份、数据库备份和构建 bundle 的默认保留数分别收敛为 4、14 和 2，生产验收默认要求至少 8 GiB 可用空间。
- 后端 Maven、共享设计系统、双前端和文档站统一升级到 `v2.5.1`。

### Security

- Nginx 在用户端与管理端入口统一拒绝公网 `/api/actuator/**` 和 `/api/internal/sre/**`，监控端口继续只绑定 loopback。
- 运维资产采用逐文件白名单安装与漂移比对；发布包明确排除监控本地状态和凭据，合成演练必须显式确认通知并从权限受限文件读取 token。

### Migration

- 本版本不新增数据库迁移。尚未完成 SRE 表升级的环境仍需先按 `sql/v2.5.0` 的既有顺序迁移，再启用数据库 Worker 和 gauge。
- 部署前确认监控 `.env` 已配置 Grafana 镜像和管理员密码，QQ/Webhook secret owner 为 `65534:65534` 且 mode 为 `0400`。

### Notes

- 本版本仍为只读调查和人工处置体系，不包含自动修复，也没有金额、Token 或模型成本预算闸门。
- 当前实现吸收了 OpenSRE 的可恢复队列、证据、Runbook、可观测和运行治理原则，但不宣称功能或协议完全对齐。

## [v2.5.0] - 2026-07-28

### Added

- 新增由 Prometheus、Alertmanager、Blackbox Exporter 和 Grafana 组成的 24x7 监控栈，以及使用独立令牌鉴权的私网告警 Webhook。
- 新增 `xiaou-sre` 模块，持久化原始告警、事故聚合、事故状态、证据快照、事务 Outbox 和处理重试。
- 新增管理端 SRE 事故工作台，支持事故摘要、状态操作、时间线、RCA 历史、调查轨迹、回放来源和人工反馈。
- 新增只读 AI RCA：固定 Prompt/Schema、脱敏上下文、结构化报告校验和可审计运行记录均由后端控制。
- 新增不可变 RCA 评测用例、版本化评测套件、质量门禁和持久化评测队列，支持进度、心跳、租约恢复、指数退避和构建溯源。
- 新增最多 5 轮的有界只读调查；模型只能选择服务端固定的 Prometheus/Loki tool key，重复证据会提前停止。
- 新增 SRE 自身指标，覆盖事故数、Outbox/评测积压、最老任务年龄、运行耗时、调查轮数、重试、租约恢复、deadline 和终态失败。

### Changed

- 告警接收、证据采集、RCA 分析和离线评测从同步页面动作收敛为持久化、可恢复且可观测的后端流程。
- 后端 Maven、共享设计系统、双前端和文档站统一升级到 `v2.5.0`。

### Security

- Webhook 使用独立 token、请求体上限和速率限制，并要求仅通过监控私网直连，不能暴露到公网 Nginx。
- 调查链路不接受模型生成的 PromQL、LogQL、Shell 或 SQL，不提供自动修复及其他系统写操作。

### Migration

- 新环境直接使用已包含 16 张 SRE 表的 `sql/MySql/code_nest.sql` 和最新 `sql/MySql/code_nest_data.sql`。
- 已部署环境按 `sre_incident.sql`、`sre_investigation_run.sql`、`sre_rca_evaluation.sql`、`sre_rca_evaluation_suite.sql`、`sre_rca_evaluation_queue.sql` 的依赖顺序升级，并执行 `sre_agent_permissions.sql`；早期 v2.5.0 预览表结构另需停 RCA 流量后执行一次 `sre_investigation_loop.sql`。
- 迁移期间保持 SRE Worker 和数据库型指标关闭；表结构、私网端点、Webhook token 及评测构建溯源配置确认后，再依次启用对应开关。

### Notes

- 本版本没有金额、Token 或模型成本预算闸门；硬边界由工具白名单、最多 5 轮、客户端超时、响应/上下文上限和查询指纹幂等提供。
- `v2.5.0` 建立的是 Code Nest 当前范围内的只读 SRE 调查与评测闭环，不宣称与 OpenSRE 完全对齐。

## [v2.4.3] - 2026-07-16

### Added

- 新增首次登录目标引导：收集目标岗位、当前学习阶段与每周投入时间，并直接生成首周自动驾驶任务。
- 新增 `GET /user/home/overview` 用户首页聚合接口，以分区降级方式返回首页数据。
- 新增 `growth_autopilot_goal.current_stage` 迁移字段，用于保留首周计划的学习重点。

### Changed

- 求职闭环将自动同步状态展示为常态，手动同步下沉为“异常恢复”菜单操作。
- 首页把“今天的行动”置于核心数据之后，版本更新下沉为页面末端的次级信息。
- 用户端 Element Plus 改为 Vite 组件按需解析，并保留 `v-loading`、消息和确认框的必要样式。
- 后端 Maven、共享设计系统、双前端和文档站统一升级到 `v2.4.3`。

### Migration

- 已部署数据库执行 `sql/v2.4.3/growth_autopilot_onboarding.sql`；该脚本为首周计划增加 `current_stage` 默认列。
- 新增的首页概览接口不替换既有模块接口，现有客户端调用保持兼容。

## [v2.4.2] - 2026-07-16

### Fixed

- 修复移动端导航抽屉被顶部导航容器裁切的问题，抽屉现在覆盖完整视口且内容可滚动。
- 修复窄屏登录页先展示品牌内容、登录表单落在首屏以下的问题。
- 修复首页并行聚合请求在服务不可用时重复弹出全局失败提示的问题，保留模块级降级状态与登录过期处理。

### Changed

- 主导航改为“今天、学习、求职、社区”四个用户目标入口，并将次级入口归并为“练习与工具、创作、更多”。
- 求职闭环中台把“下一步行动”前置到数据概览之前，主操作调整为“开始求职准备”，不再暗示会重置既有会话。
- 面试题库移除“当前页码”指标卡，将可执行的题单列表置于学习热力图之前。
- 后端 Maven、共享设计系统、双前端和文档站统一升级到 `v2.4.2`。

### Migration

- 本版本不新增数据库迁移、环境变量或对外 API 兼容性要求。

## [v2.4.1] - 2026-07-15

### Added

- 新增独立的 `code-nest-design-system` 包，由用户端和管理端共享组件、主题、设计令牌与组合式函数。
- 新增可注入的 `RedisValueStore` 和 Spring 托管的有界 `applicationIoExecutor`。
- 新增学习驾驶舱、WebSocket 票据、Redis 值存储和朋友圈核心服务的聚焦回归测试。
- 重构 VitePress 文档中心，新增分类首页、响应式工程手册主题、动态同步基线和导航完整性审计。

### Changed

- 学习驾驶舱、社区热点与摘要、朋友圈、摸鱼工具、抽奖和验证码等热点链路减少重复查询、缓存往返与无边界并发。
- 缓存、锁、限流和执行器改为依赖注入或模块自有实现，明确基础设施与领域语义边界。
- 129 个 Markdown 页面重组为开始、架构、模块、API、运维和资料六个一级入口。
- 后端 Maven、共享设计系统、管理端前端、用户端前端、文档站和部署示例统一升级到 `v2.4.1`。

### Removed

- 删除双前端内重复的设计系统源码，统一从 `code-nest-design-system` 引用。
- 删除静态 `RedisUtil`、`ThreadPoolUtils` 和 `ConcurrentUtils`，避免全局状态与无边界资源管理。

### Migration

- 本版本不新增数据库迁移、环境变量或对外 API 兼容性要求。
- 前端依赖仍通过仓库内 `file:../code-nest-design-system` 安装，无需发布外部 npm 包。

## [v2.4.0] - 2026-07-14

### Added

- 新增管理员端统一自然语言入口 `POST /admin/agent/chat`，由后端完成 LLM 规划、工具注册、权限策略、预览、强确认、执行和审计。
- 新增通用 `AgentTool` 注册机制；当前 26 个生产工具均通过同一 Registry 和 Orchestrator 运行，无需在前端维护动作目录或业务分组路由。
- 新增 Agent 审计、会话上下文、幂等控制、工具指标、readiness、dry-run、恢复分析和访问预检能力。
- 新增分层全站评测脚本和 CI 门禁，覆盖后端、双前端、AI、RAG、仓库卫生与发布构建。

### Changed

- 管理员 Agent planner 改为根据统一工具 definition/schema 生成候选调用，deterministic resolver 仅作为模型不可用或无有效计划时的兜底。
- AI completion 增加全局预算和场景级覆盖能力，管理员 planner 使用独立输出预算。
- 管理端只保留聊天抽屉、结果展示和确认交互，规划、策略、权限与执行职责全部收口到后端。
- 后端 Maven、管理端前端、用户端前端、文档站和部署示例统一升级到 `v2.4.0`。

### Fixed

- 修复本地文件存储可通过相对路径、绝对路径或符号链接越过配置目录的问题。
- 修复聊天消息写入回读、跨房间回复、图片 URL 长度、重复撤回和空批量删除等契约问题。
- 修复抽奖风控短路、空积分/次数、响应字段不一致和库存补偿边界。
- 修复 OJ 判题错误信息回退和答案前导空白被错误忽略的问题。

### Security

- 所有 Agent 写入和破坏性操作必须经过后端权限策略、预览、精确确认文本、审计状态机与幂等校验。
- 会话上下文存储键按管理员 ID 隔离；统一聊天 DTO 增加长度上限，无操作者归属的历史审计不能继续确认。
- 真实 AI API Key 仅通过测试进程环境变量注入，未写入源码、配置、测试报告或提交历史。

### Migration

- 新部署可直接使用 `sql/MySql/code_nest.sql` 与 `sql/MySql/code_nest_data.sql`。
- 已部署环境按顺序执行 `sql/v2.4.0/admin_agent_audit.sql`、`admin_agent_audit_idempotency.sql`、`admin_agent_session_context.sql` 和 `admin_agent_permissions.sql`。
- 配置 `XIAOU_AI_MAX_COMPLETION_TOKENS` 可调整全局 completion 上限；未配置时使用默认值 `2048`。

## [v2.3.2] - 2026-06-14

### Fixed

- 修复版本时间轴与管理端版本列表分页总数不准确的问题。
- 修复用户端首页首屏因 reveal 状态未解除而大面积空白的问题。

### Changed

- 根据 Git 提交和版本分支重建线上版本历史。
- 后端 Maven、管理端前端、用户端前端、文档站和部署示例统一升级到 `v2.3.2`。

## [v2.3.1] - 2026-06-14

### Added

- 新增统一 CI 工作流，覆盖后端 Maven 打包、用户端构建、管理端构建、文档站构建和脚本语法检查。
- 新增生产部署 GitHub Actions，支持版本分支推送或手动触发后构建 release bundle 并同步到服务器。
- 新增服务器端发布脚本，内置当前版本备份、后端 Jar 替换、双前端静态资源替换、Nginx reload、健康检查和失败回滚。
- 新增生产 CI/CD 文档，记录 GitHub Secrets、触发方式、服务器目录、回滚命令和排障入口。

### Changed

- 统一后端 Maven、管理端前端、用户端前端、文档站和 README 示例版本到 `v2.3.1`。
- 文档站 GitHub 编辑链接改为 `master` 分支，避免继续指向不存在或非默认的 `main` 分支。

### Security

- 部署凭据只通过 GitHub Secrets 注入，仓库仅保留变量名和使用说明，不保存 SSH 私钥、服务器密码或令牌。

## [v2.3.0] - 2026-06-14

### Added

- 新增双前端快速部署脚本，支持一次构建并同步用户端与管理端 dist 到线上目录。
- 新增 You-Deserve 题库迁移脚本与迁移记录文档，保留后续筛选后重新导入的工具链。

### Changed

- 统一后端 Maven、管理端前端、用户端前端、文档站和 README 示例版本到 `v2.3.0`。
- 优化双端顶部导航居中和主题切换交互，将主题配置收敛为下拉式轻量控件。
- 技术社区与朋友圈页面改为占满主内容区，减少桌面端内容区域过窄的问题。
- 管理端登录页移除硬编码账号密码展示，用户重置密码改为管理员显式输入。

### Fixed

- 修复用户端聊天室 WebSocket 地址和开发代理配置，改善线上反复重连问题。
- 修复面试分类管理中分类 ID、状态和树形数据处理不一致导致的操作异常。
- 修复学习与创作菜单图标偏移问题。

### Notes

- You-Deserve 线上试导入数据已按要求回滚删除，当前保留迁移工具与记录，不保留生成 SQL 产物。

## [v2.2.2] - 2026-05-27

### Added

- 新增 GitHub 协作治理文档：贡献指南、安全策略、行为准则、发布流程、Issue 模板和 PR 模板。
- 新增文档站发布交接、环境变量、验证场景、模块手册和维护规范资料。
- 新增 `.gitattributes`，统一跨平台文本换行策略。

### Changed

- 规范 Pull Request、Issue、版本发布和文档同步流程，降低多模块协作时的遗漏风险。
- 统一后端 Maven、管理端前端、用户端前端、文档站和 README 示例版本到 `v2.2.2`。
- 修正 docs-site GitHub Actions 版本分支触发规则。

### Fixed

- 对齐学习小组任务、打卡、讨论、成员、统计等前后端字段契约。
- 清理本地生成脚本、构建日志和 Windows 保留名残留文件污染风险。

## [v2.2.1] - 2026-05-24

### Fixed

- 对齐学习小组任务、打卡、讨论、成员、统计等前后端字段契约。
- 修复小组任务创建和编辑时的任务类型、重复规则、目标值、日期范围等提交参数。
- 修复打卡弹窗的任务选择、补卡日期、图片数组、内容字段和接口参数。
- 修复小组列表排序参数与后端枚举不一致的问题。
- 补齐讨论点赞记录模型、Mapper 和 SQL 表结构。

### Changed

- 优化学习小组列表、详情、成员、排行榜、统计等接口响应字段兼容性。
- 优化小组统计、排行、打卡、讨论和成员服务的数据聚合方式。
- 优化前端操作成功后的任务、打卡和讨论列表刷新逻辑。

### Added

- 新增小组统计值 DTO，用于统一趋势、排行和聚合统计返回结构。
- 新增申请列表中的小组头像字段。
- 新增小组列表待审核申请数量聚合字段。

### Verified

- `npm run build` in `vue3-user-front`
- `mvn -pl xiaou-team -am -DskipTests compile`

### Notes

- Maven 编译期间如出现 `The requested profile "rdc" could not be activated because it does not exist.`，属于当前环境 profile 提示，不影响本次编译结果。

## [v2.1.2]

### Added

- 新增聊天室消息限流和输入中事件降噪。
- 新增前端失败态提示，便于用户识别限流或校验失败。

### Fixed

- 强化文本、图片消息校验，避免非法消息进入数据库。

## [v2.1.1]

### Changed

- 收紧文件接口访问权限。
- WebSocket 握手切换为短期票据机制。
- HTTP 与 WebSocket CORS 来源配置化。

### Fixed

- 补齐聊天室 PONG 心跳和 TYPING 输入中事件链路。
- 对 Markdown 和高风险 HTML 渲染场景接入净化或转义。

## [v2.1.0]

### Added

- 新增 AI 学习成长驾驶舱。
- 新增 AI Runtime 质量与治理中心。
- 新增 Prompt、Schema、RAG、Regression 等治理视图。

## [v2.0.0]

### Changed

- 移除历史 Coze 链路。
- AI 基础设施统一切换到 LangChain4j、LangGraph4j 和 LlamaIndex 分层架构。

### Added

- 新增 AI Prompt 模板化、版本化和可观测治理能力。
- 新增知识库 sidecar 和 RAG 检索调试链路。
