# 变更日志

本项目遵循面向读者的变更记录方式，按版本归档重要功能、修复、工程治理、兼容性变化和迁移提示。

格式参考 [Keep a Changelog](https://keepachangelog.com/) 的结构，并结合本项目实际使用 `vX.Y.Z` 版本号。

## [Unreleased]

## [v2.5.7] - 2026-08-05

### Fixed

- 显式标记 `SreOutboxWorker` 的五参数生产构造器，避免多个公共构造器让 Spring 回退到不存在的默认构造器并阻断应用启动。
- 生产工作流使用 sudoers 批准的绝对路径执行容量治理脚本，避免相对参数与服务器精确命令授权不匹配。

### Added

- 新增最小 Spring 容器 wiring 回归测试，直接验证 `SreOutboxWorker` 的生产依赖可以完成 Bean 装配。
- 发布契约测试新增容量治理 sudo 命令检查，防止生产工作流重新使用未授权的相对路径。

### Migration

- 无数据库迁移和新增生产环境变量；数据库 schema 基线仍为 `v2.5.3`。

### Verification

- SRE Worker wiring 与行为测试、生产构建脚本契约、发布清单一致性及完整发布门禁。

## [v2.5.6] - 2026-08-05

### Added

- 新增 `xiaou-bootstrap` 独立启动壳、五个职责单一的 `xiaou-common-*` 基础模块和统一聚合韧性模块 `xiaou-resilience`。
- 新增 `@code-nest/api-contract`，共享双前端响应解包、错误分类、分页与通知 DTO。
- 新增 `release/manifest.json` 与 `scripts/release_manifest.py`，统一版本、数据库基线、Docker 标签和后端/前端/文档制品布局。
- 新增架构门禁与发布清单契约测试，持续检查模块依赖、领域所有权、前端契约和版本投影。

### Changed

- `xiaou-application` 从启动聚合改为应用编排；运行配置、启动监听器和可执行 JAR 统一归入 `xiaou-bootstrap`。
- Growth Coach、首页和学习驾驶舱的跨领域数据访问改为端口与持久化适配器，聚合查询统一使用有界韧性执行。
- SRE RCA/评测从 `xiaou-system` 归入 `xiaou-sre`；通知实体、Mapper、异步配置和发布接口从公共模块归入 `xiaou-notification`。
- 缓存调用统一依赖 `CacheStore` / `TextStateStore`，具体 Redis 客户端限制在缓存适配层或显式领域适配器中。
- `Result` 错误统一映射语义化 HTTP 状态；认证为 401、权限/禁用为 403、普通业务拒绝为 422，同时保留 701-705 等业务码。
- 用户端和管理端路由按业务域切片，入口文件分别缩减为组合注册层。
- CI、生产工作流、本机构建、release bundle 和文档同步统一从发布清单读取元数据。
- bundle 内清单使用无大小写冲突的 `release-manifest.json`，清单门禁拒绝 Windows/Linux 不一致的文件与目录路径组合。

### Migration

- 无数据库迁移和新增生产环境变量；数据库 schema 基线仍为 `v2.5.3`。
- 后端制品路径从 `xiaou-application/target` 改为 `xiaou-bootstrap/target`。

### Verification

- 架构/发布清单门禁、后端模块测试、API contract、双前端契约与生产构建、文档构建及发布脚本契约。

## [v2.5.5] - 2026-08-04

### Added

- 新增用户侧只读成长能力图谱：将 OJ、题目掌握、模拟面试、项目作品、代码审查、计划执行和求职阶段证据汇总为问题解决、面试表达、项目交付、持续执行和求职准备五类能力节点。
- 能力图谱提供能力分数、可信度、近期趋势、证据引用、能力关系和来自既有短板洞察的补强项，并新增 `/growth-capabilities` 用户端页面。

### Changed

- Growth Coach 导航和自动驾驶入口增加能力图谱跳转；客户端事件默认版本升级为 `2.5.5`。
- 能力图谱仅做只读投影，不创建任务、不修改计划、不新增数据库表，也不把模型建议当作能力事实。
- 能力图谱聚合请求复用同一批有界证据，避免短板洞察重复触发证据投影和数据库读取。

### Verification

- 能力图谱后端单元测试、用户端契约测试、管理端契约测试、用户端生产构建和 Maven 多模块定向测试通过。

## [v2.5.4] - 2026-08-04

### Added

- 合并远程 master 的生产 SRE 评测、容量治理、外部探针和运维资产，同时保留 v2.5.3 的 Growth Coach、迁移 ledger、发布烟测与 SSRF/AI 并发防护。
- SRE 指标、告警接收和 Outbox worker 统一输出低基数运行指标，补充队列积压、租约恢复、耗时和状态计数。

### Changed

- 发布构建统一校验 `VERSION`、迁移清单和发布包内容；生产部署脚本支持显式迁移执行，默认仍不自动写库。
- master 合并后的发布包同时携带运维资产、完整 SQL、迁移执行器和发布烟测脚本，便于回滚与审计。

### Verification

- 版本一致性、Python 语法、shell contract、后端多模块测试、SRE 定向测试、双前端契约测试和发布包 smoke test。

## [v2.5.3] - 2026-08-04

### Added

- 新增统一 `VERSION` 基线、版本一致性检查和发布包烟测，后端、双前端、设计系统、文档站与生产产物统一使用 `v2.5.3`。
- 新增带 SHA-256 checksum ledger 的数据库迁移执行器，支持 `--dry-run`、`--baseline` 和 `--apply`，迁移包随 release bundle 一起交付。
- 新增生产治理迁移，为成长事件补充 `schemaVersion`、客户端版本、入口页面和索引，并记录迁移执行耗时与失败原因。
- SRE Outbox 暴露队列积压、处理中数量、处理耗时、成功/重试/失败和租约恢复指标，指标标签限制为固定事件类型和结果。
- Growth Coach 数据源改为有界并发读取，增加超时、来源状态、耗时指标和 5 秒本地短缓存；成长事件补充来源白名单和行动/结果事件契约。

### Changed

- 生产构建默认拒绝 dirty worktree；发布脚本增加磁盘余量检查、备份容量上限、失败回滚和可选迁移执行。
- 发布服务器端复核 bundle 路径、版本元数据、归档类型和发布烟测，避免只信任本地构建阶段的校验。
- 敏感词远程来源增加 SSRF 防护：仅允许 HTTP(S)，拒绝内网/回环/云元数据地址，禁止重定向，并限制连接、读取超时与响应体大小。
- AI 运行时增加最大并发调用数、permit 等待超时、拒绝指标和 operation id 日志，避免突发流量耗尽线程与连接资源。

### Migration

- 新环境或已确认的历史环境使用 `python scripts/db-migrate.py --apply` 按版本顺序执行迁移。
- 已经手工执行过历史 SQL 的环境先执行 `--baseline --baseline-to v2.5.2`，再执行 `--apply`；生产部署默认不自动写库，只有显式设置 `CODE_NEST_RUN_MIGRATIONS=true` 才会执行。
- 已应用的 SQL 文件不得修改；checksum 不一致会阻止发布并要求恢复原文件或新增迁移版本。

### Verification

- Maven 多模块编译与定向后端测试通过。
- 用户端契约测试 46 项、管理端契约测试 20 项通过；用户端、管理端和文档站生产构建通过。
- 版本一致性、Python 语法、迁移 dry-run、发布脚本 shell syntax 和发布包 smoke test 通过。

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
