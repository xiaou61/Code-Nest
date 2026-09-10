---
artifact: project-rules
status: active
configured: true
schema_version: 2
---

# Code-Nest 项目常驻规范

本文件保存经用户确认、跨工作项长期有效的项目事实与约束。

本文件与仓库根 `SKILL.md`（`code-nest-project-spec`）冲突时以本文件为准。`SKILL.md` 已知有 4 处失效声明（启动入口写成 `xiaou-application`、文档根写成 `docs/`、`Result` 路径前缀写成 `xiaou-common/`、启动命令 `-pl xiaou-application`），尚未回修。

模块级设计细节见 `.agent/specs/`，本文件只记录跨任务长期有效的事实与硬约束。

## 项目事实

- 版本：`VERSION` = 2.5.8；根 `pom.xml` 的 `<revision>` = v2.5.8；4 个 `package.json`（两端前端 + `code-nest-api-contract` + `code-nest-design-system`）version = 2.5.8。
- 后端：Maven 多模块，Java 17，Spring Boot 3.4.4，Undertow，端口 9999，统一上下文路径 `/api`。
- 关键依赖：Sa-Token 1.44.0、MyBatis-Spring-Boot 3.0.3、Druid 1.2.20、Redisson 3.24.3、PageHelper 6.1、LangChain4j 1.13.0、LangGraph4j 1.8.13。
- 分层：`xiaou-bootstrap` 为启动层（唯一启用 `spring-boot-maven-plugin`，`mainClass = com.xiaou.bootstrap.CodeNestApplication`）；`xiaou-application` 既是业务聚合层（34 个模块依赖），也**实际承载** growthcoach / home / learning 三个业务域、9 张 `growth_*` 表与 2 台 `@Scheduled` 任务。
- 两套前端均支持 Electron 打包，入口在各自 `electron/`；`sql/` 采用「基线 + 版本增量」结构。

## MUST

### 路径与配置

- 启动主类：`xiaou-bootstrap/src/main/java/com/xiaou/bootstrap/CodeNestApplication.java`
- 主配置：`xiaou-bootstrap/src/main/resources/application.yml`；另有 `application-dev.yml`、`application-docker.yml`、`application-prod.yml`
- 私密配置：`xiaou-bootstrap/src/main/resources/application-sec.yml`（已被 `.gitignore` 排除，不得提交，不得把其中内容写入任何工件）
- 统一返回体：`xiaou-common-core/src/main/java/com/xiaou/common/core/domain/Result.java`、`ResultCode.java`
- 鉴权：`xiaou-common-security/src/main/java/com/xiaou/common/config/SaTokenConfig.java`、`satoken/StpAdminUtil.java`、`satoken/StpUserUtil.java`、`satoken/StpInterfaceImpl.java`、`annotation/RequireAdmin.java`
- 前端请求入口：`vue3-admin-front/src/utils/request.js`、`vue3-user-front/src/utils/request.js`
- 文档根目录：`AI-DOCS/`；仓库无 `docs/`
- 共享包：`code-nest-api-contract`（错误码归类与请求选项）、`code-nest-design-system`（主题与组件），通过 `file:../` 被两端前端引用，**不是独立发布产物**

### 启动与验证

- 后端编译：`mvn -pl xiaou-bootstrap -am clean package -DskipTests`
- 后端启动：`mvn -pl xiaou-bootstrap -am spring-boot:run`
- 管理端：`cd vue3-admin-front && npm run dev`（端口 3000，代理至 `http://localhost:9999`）
- 用户端：`cd vue3-user-front && npm run dev2`（端口 3001，代理至 `http://localhost:9999`）
- 前端 lint：两端 `npm run lint`
- 禁止对 `xiaou-application` 使用 `spring-boot:run` 或指望它产出可执行 jar：该模块未启用 `spring-boot-maven-plugin`。

### 路由分区与鉴权（按源码实测）

框架级拦截（`SaTokenConfig`，`addPathPatterns("/**")` + 排除 `/error`、`/favicon.ico`）**只有四组**：

| 前缀 | 校验 |
| --- | --- |
| `/auth/**`、`/admin/**` | `StpAdminUtil.checkLogin()`（放行 `login`/`register`/`refresh`） |
| `/user/**` | `StpUserUtil.checkLogin()`（放行 `user/auth/*` 及一批 `/user/team/**` 匿名查询） |
| `/captcha/**` | 显式放行 |
| `/v3/api-docs/**`、`/swagger-ui/**`、`/swagger-ui.html` | 显式放行 |

**其余前缀不在框架拦截范围内**，共 36 个 Controller。路径必须由方法级注解或控制器自身校验保护：

| 前缀（Controller 数） | 保护方式 |
| --- | --- |
| `/sensitive/{whitelist,strategy,version,statistics,source,homophone,similar-char}`（7）、`/log`（1） | `@RequireAdmin` 切面（`AdminAuthAspect` → `checkLogin` + `checkRole("admin")`） |
| `/internal/sre/alertmanager/v1/**` | 自带 `SreWebhookAuthenticationFilter` 令牌校验 |
| `/captcha` | 有意公开 |
| `/sensitive` | 敏感词检测入口，有意公开 |
| `/community/**`（8）、`/oj/**`（4）、`/moyu/**`（5）、`/interview/**`（5）、`/flashcard/**`（3）、`/resume/**`（2）、`/pub/flashcard/**`、`/pub/knowledge/**`、`/notification`、`/file`、`/version` | **无框架保护**，依赖控制器内手工 `checkLogin()`，覆盖情况不一致（详见 `.agent/specs/` 对应章节） |

- 新增对外接口时，**必须**明确它落在上表哪一类，并显式写清所需登录态；位于 `/user`、`/admin`、`/auth` 之外的新前缀不会自动获得登录校验。
- 鉴权工具复用 `StpAdminUtil` / `StpUserUtil`；管理端方法优先 `@RequireAdmin`。
- 前端 token key：用户端 `user_token`（localStorage `user_token`/`user_info`），管理端 `token`（Cookies + localStorage `userInfo`/`tokenExpireTime`）。

### 返回体与业务码

所有后端接口统一返回 `Result<T>`。`ResultCode` 全集（以源码为准，不要只记 200/701–704）：

- 2xx/4xx/5xx 通用：200、400、401、403、404、405、408、409、415、429、500、503
- 业务段：600 业务处理失败、601 参数校验失败、602 数据不存在、603 数据已存在、604 操作不被允许
- 鉴权段：**701 Token 无效、702 Token 过期、703 权限不足、704 账号禁用、705 登录失败**
- 文件段：801–805

前端语义分工（不得混用）：`701`/`702` 触发「登录过期」提示，`703`/`704` 中 **`704` 会强制登出**，`705` 属业务错误、不得触发过期流程；`code-nest-api-contract` 的 `classifyErrorKind` 把 703/704 都归类为 `authorization`，与拦截器行为不同，改动时两侧要一起看。

### 落点与生成物

- 新业务逻辑落在对应业务模块，**不要**塞进 `xiaou-bootstrap`。
- `xiaou-application` 里的 growthcoach / home / learning 是历史既成事实，**不作为新代码的先例**；新业务域一律建独立 `xiaou-*` 模块。
- 新增业务模块必须同步根 `pom.xml` 的 `<modules>` 与 `xiaou-application/pom.xml` 依赖。
- OJ 特例：Mapper XML 位于 `xiaou-oj/src/main/java/com/xiaou/oj/mapper/`（该模块无 `resources/mapper`）。
- SQL：基线 `sql/MySql/code_nest.sql`、`code_nest_data.sql`；结构变更写增量到 `sql/vX.Y.Z/`，避免破坏性 `DROP`。
- 禁止手改：`pom-xml-flattened`、`target/`、`dist/`、`out/`、`node_modules/`。

### 版本同步

- 改版本必须同步：`VERSION`、根 `pom.xml` 的 `<revision>`，以及 **4 个** `package.json`（`vue3-admin-front`、`vue3-user-front`、`code-nest-api-contract`、`code-nest-design-system`）——`release/manifest.json` 的 `projections.packageJson` 已把这 4 个纳入断言。
- 版本核对命令：`scripts/check-version-consistency.py`（仅 8 行，是 `release_manifest.py validate` 的兼容别名，不是独立规则集）。架构约束：`scripts/check-architecture.py`。

### 工具链边界

- 本仓库的 git 传输需经本地代理 `http://127.0.0.1:7897`（已写入 `.git/config` 的 `http.proxy`/`https.proxy`）；直连会被限速到 15–50 KiB/s 并频繁断流。

## 已知风险与待修复

以下条目是**已核实的缺陷台账，不是设计约定**，不得当作可以照做的模式。修复需要独立工作项与回归验证，不在本文件的批准范围内。

1. **鉴权默认放行**：36 个 Controller 不在框架拦截前缀内（见上表），其中社区/OJ/摸鱼/面试/闪卡/简历/通知/文件等大量写操作依赖控制器自证登录，覆盖不一致。已知具体缺口：`InterviewQuestionSetPublicController` 的 `userId != null &&` 短路使未登录用户绕过题单权限校验；`AdminMockInterviewController` 无任何 `@RequireAdmin` 且无 Service 层。
2. **权限模型与鉴权脱钩**：`StpInterfaceImpl` 对 admin 恒返回角色/权限 `["admin"]`、对 user 恒返回 `["user"]`，无按用户查询；`sys_role`/`sys_permission`/`sys_admin_role`/`sys_role_permission` 不参与鉴权，仅用于回显。因此 `@RequireAdmin` 实际只能区分「管理员 vs 普通用户」，任何细粒度权限都落不了地；角色/权限关联表也没有写入入口。
3. **操作日志无写入者**：`@Log` 注解无对应切面，`SysOperationLogService.saveOperationLog` 在生产路径无调用点，操作日志页面数据来源缺失。
4. **生产配置断链**：`application-prod.yml` 仅一行注释（无数据源/Redis），`application-sec.yml` 不在仓库中（仅被 `optional:` 导入），实际可用的全环境变量化 profile 只有 `docker`；`xiaou.cors.allowed-origin-patterns` 在所有 yml 中无覆盖。
5. **内容安全覆盖不全且兜底语义相反**：敏感词检测只作用于 community / moment / blog 与 `/sensitive/check`，`xiaou-team` 讨论与打卡、文件上传内容、OJ、简历均无检测；异常时 `SensitiveCheckServiceImpl` 返回拒绝，而 `SensitiveWordUtils` 默认放行。
6. **同一概念多套实现**：点赞/收藏在 community / moment / codepen 有三套语义不一致的实现（重复点赞抛异常 vs `INSERT IGNORE` 幂等；计数递减有的带 `> 0` 保护、有的没有）；复习调度存在两套算法（简化艾宾浩斯无 EF 与真 SM-2），掌握度刻度分裂为 1–4 与 1–3。
7. **`llamaindex-service` 名实不符且默认无鉴权**：`requirements.txt` 声明 `llama-index` 但源码零引用，实为自研词法检索；`LLAMAINDEX_SERVICE_API_KEY` 为空时全部接口（含文档增删）放行。
8. **发布链路脆弱点**：`deploy-release.sh` 的 `validate_stage` 硬编码 `sql/v2.5.3/production_governance.sql`，SQL 目录演进会阻断发布；配置漂移检测不含 `alertmanager.local.yml` 与 `docker/monitoring/.env`。
