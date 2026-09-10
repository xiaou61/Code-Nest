---
artifact: spec
status: active
scope: 整体架构与公共平台层
---

# 整体架构与公共平台层

Code-Nest 后端是单体 Maven 多模块 Spring Boot 3.4.4 / Java 17 工程（根 `pom.xml`，`revision = v2.5.8`，36 个 `<module>`）。形态是"一个进程、一个 Spring 上下文、按包边界切模块"：`xiaou-bootstrap` 是唯一可执行装配层，`xiaou-application` 把 34 个业务模块聚进同一 classpath，其余模块各自贡献 `com.xiaou.<模块>.**` 下的 controller/service/mapper，由 `scanBasePackages = "com.xiaou"` 统一扫描。

模块间不是 RPC，而是同 JVM 内的接口注入；跨模块契约靠 `xiaou-user-api` / `xiaou-sensitive-api` 这类"只有接口和 DTO 的 jar"表达。

## 模块总览

| 模块 | 职责一句话 | 关键入口（包/类/文件） |
| --- | --- | --- |
| `xiaou-bootstrap` | 唯一启动/装配层：主类、配置、共享线程池 | `com.xiaou.bootstrap.CodeNestApplication`、`StartupApplicationListener`、`config/ApplicationTaskExecutorConfig`、`resources/application*.yml` |
| `xiaou-application` | 不只聚合：承载 growth-coach 领域 + 首页/学习驾驶舱聚合 API 及其表与 Mapper | `com.xiaou.web.IndexController`、`web.growthcoach.**`、`web.home.**`、`web.learning.**`、`resources/mapper/*.xml` |
| `xiaou-common` | 名义"通用工具"，实为公共子模块胖聚合 + 敏感词静态门面 | `com.xiaou.common.utils.SensitiveWordUtils`、`enums/package-info` |
| `xiaou-common-core` | 零框架依赖基元：统一返回体、业务码、业务异常、分页、通用工具 | `core/domain/Result`、`ResultCode`、`PageRequest`、`PageResult`、`exception/BusinessException`、`constant/Constants` |
| `xiaou-common-web` | HTTP 横切：全局异常、Result→HTTP 映射、CORS、本地文件映射、IP 工具 | `exception/GlobalExceptionHandler`、`web/ResultHttpStatusAdvice`、`web/ResultHttpStatusMapper`、`config/CorsConfig`、`config/LocalFileResourceConfig` |
| `xiaou-common-security` | 鉴权横切：Sa-Token 路由规则、双账号体系工具、`@RequireAdmin` 切面 | `config/SaTokenConfig`、`satoken/StpAdminUtil`、`StpUserUtil`、`StpInterfaceImpl`、`aspect/AdminAuthAspect` |
| `xiaou-common-cache` | Redis 访问抽象：KV/文本状态存储接口与 Redisson 实现 | `cache/CacheStore`、`RedisValueStore`、`TextStateStore`、`RedisTextStateStore`、`config/RedisConfig` |
| `xiaou-common-persistence` | 持久化基础设施：P6Spy SQL 格式化、PageHelper 包装 | `config/P6SpyLogger`、`utils/PageHelper` |
| `xiaou-resilience` | 无框架依赖的容错执行器：限时 + 降级语义 | `resilience/ResilientExecutor`、`ResilientResult`、`ResilientStatus` |
| `xiaou-user-api` | 用户域契约 jar：只放接口与 DTO | `com.xiaou.user.api.UserInfoApiService`、`dto/SimpleUserInfo` |
| `xiaou-sensitive-api` | 敏感词域契约 jar：只放接口与 DTO | `com.xiaou.sensitive.api.SensitiveCheckService`、`dto/SensitiveCheckRequest`、`SensitiveCheckResponse` |

## 整体架构

### 模块分层与依赖方向

- 依赖自上而下单向：`xiaou-bootstrap` → `xiaou-application` →（common-* / resilience / user-api）→ `xiaou-common-core`；boot 层只装配不放业务。
- `xiaou-application/pom.xml` 声明 34 个模块依赖（common 系列 7 + system/sre/user-api/user 4 + 23 个业务模块）+ `micrometer-core`，**未启用 `spring-boot-maven-plugin`**（与 `always.md` 禁令一致）。
- 依赖最窄的底层是 `xiaou-common-core`（lombok/hutool/jackson/fastjson2/spring-core/slf4j）与 `xiaou-resilience`（spring-context + slf4j）。
- 反向耦合一例：`xiaou-common` → `xiaou-sensitive-api`，`SensitiveWordUtils` 用 `@Autowired` setter 把 `SensitiveCheckService` 注入静态字段（service-locator 式反模式）。
- `xiaou-application` 另对 11 个业务模块的服务接口做直接注入（`UserHomeOverviewService` 构造器引用 community/interview/knowledge/mockinterview/moment/plan/points/version），这部分无契约 jar 隔离。

### 请求链路：HTTP → 过滤器/拦截器 → 鉴权 → 统一返回 → 异常处理

1. **容器**：`spring-boot-starter-undertow`（pom 显式排除 tomcat），端口 `9999`，context-path `/api`（`application.yml`）。
2. **过滤器**：`CorsConfig#corsFilter`（`/**`，allowCredentials true，maxAge 3600）；`LocalFileResourceConfig` 把 `/files/**` 映射到本地上传根。
3. **拦截器**：`SaTokenConfig implements WebMvcConfigurer` 注册 `SaInterceptor`，`addPathPatterns("/**")`，排除 `/error`、`/favicon.ico`；内部只对三组前缀校验登录：`/auth/**`、`/admin/**`（排除 login/register/refresh）→ `StpAdminUtil.checkLogin()`；`/user/**`（排除 `/user/auth/*` 与约 10 行硬编码 team 白名单）→ `StpUserUtil.checkLogin()`；`/captcha/**`、`/v3/api-docs/**`、`/swagger-ui/**` 显式 `stop()`。
4. **默认不鉴权**：不在上表前缀内的路径（`/oj/**`、`/community/**`、`/version/**`、`/sensitive/**`、`/files/**`）不受拦截器保护，须 Controller 自行 `checkLogin()` 或用 `@RequireAdmin`。
5. **方法级鉴权**：`@RequireAdmin`（`common-security/annotation`）由 `AdminAuthAspect#around` 处理 → `StpAdminUtil.checkLogin()` + `checkRole("admin")`，异常原样上抛；全仓约 378 处使用。
6. **统一返回体**：`Result<T>{code,message,data,timestamp}`，工厂 `success/error/result`，`isSuccess()` 只认 `ResultCode.SUCCESS(200)`；已读的 5 个 `xiaou-application` Controller 均直接返回 `Result.success(...)`，`/user/**` 接口在方法体首行额外调 `StpUserUtil.checkLogin()`。
7. **双层状态语义**：`GlobalExceptionHandler`（`@RestControllerAdvice`）逐个 `@ExceptionHandler` 标注 `@ResponseStatus`；`ResultHttpStatusAdvice implements ResponseBodyAdvice<Result<?>>` 对 `!isSuccess()` 的响应体再调 `ResultHttpStatusMapper.resolve(code)`（包级私有 final 类）覆盖 HTTP 状态，非业务码回落 `HttpStatus.resolve`，兜底 422。
8. **映射摘要**：`BusinessException`→422（回显 `e.getCode()`）；`NotLoginException`→401（NOT_TOKEN/INVALID→701，TIMEOUT/BE_REPLACED/KICK_OUT→702）；`NotPermission`/`NotRole`→403+703；`DisableService`→403+704；参数校验类→400+601；文件超限→413+805；`Exception`→500+500。

### 配置体系

| 文件 | 内容要点 |
| --- | --- |
| `application.yml` | `spring.config.import: "optional:application-sec.yml"`；`profiles.active: dev`；multipart 100MB；Jackson 时间格式；`spring.security.user` 置空；legacy `jwt.*`（`XIAOU_JWT_SECRET`，注释称兼容旧代码）；`sa-token.*`（token-name `Authorization`、timeout 604800、is-concurrent true、is-share false、token-style uuid、is-read-cookie false、is-read-header true、token-prefix `Bearer`）；`mybatis.mapper-locations` = `classpath*:mapper/**/*.xml` + `classpath*:com/xiaou/*/mapper/*.xml`；`type-aliases-package: com.xiaou.*.domain`；pagehelper(mysql)；logback-spring.xml；springdoc；management(health,info,metrics,prometheus，`show-details: never`，HTTP 直方图 0.5/0.95/0.99)；`xiaou.ai.*`、`xiaou.growth-coach.*`、`xiaou.sre.*`、`xiaou.sensitive.source.*`、`community.*`、`oj.judge.*` |
| `application-dev.yml` | P6Spy 代理驱动 `jdbc:p6spy:mysql://localhost:3306/code_nest`，`root` + **明文口令硬编码**（联调默认值，值见该文件，此处不复制）；Redisson `database: 3`；`sa-token.alone-redis.database: 4`（Token 与业务数据分库） |
| `application-docker.yml` | 全环境变量化（`XIAOU_MYSQL_*`、`XIAOU_REDIS_*`、`XIAOU_SA_TOKEN_REDIS_*`），默认主机 `mysql`/`redis`，驱动切 `com.mysql.cj.jdbc.Driver` |
| `application-prod.yml` | 仅一行注释，无任何有效配置 |
| `application-sec.yml` | 被 `optional:` 导入；本次 glob **未在仓库中找到该文件**（`always.md` 记为 gitignore 排除的私密配置） |

- 开关集中在 `xiaou.growth-coach.*`：`enabled/preview-enabled/confirm-enabled`、`code-artifact.enabled`、`github-oauth.enabled`、`code-review.enabled`、`proactive-nudge.enabled` 默认 `false`，`evidence-projection.enabled` 默认 `true`；绑定类 `GrowthCoachProperties`（`@Data @Component @ConfigurationProperties("xiaou.growth-coach")`，6 个静态嵌套类）。
- `xiaou.sre.operational-evidence.enabled` 默认 `true`，其余 sre 子能力（metrics/webhook/outbox/evaluation/prometheus/loki）默认 `false`——"默认关闭、显式开启"。

## 模块详情

### xiaou-bootstrap（xiaou-bootstrap）

- **职责与边界**：唯一装配层，仅 3 个类 + 4 个 yml + `logback-spring.xml`/`banner.txt`/`spy.properties`；`CodeNestApplication` 为 `@SpringBootApplication(scanBasePackages="com.xiaou", exclude=UserDetailsServiceAutoConfiguration)` + `@EnableAsync` + `@EnableScheduling` + `@MapperScan({"com.xiaou.*.mapper","com.xiaou.web.growthcoach.mapper"})`。核心领域对象与数据表：无。
- **对外接口**：无 HTTP 接口；导出 `applicationIoExecutor`（core 8 / max 24 / queue 200 / CallerRunsPolicy / 关闭等待 30s）。
- **关键流程 / 依赖 / 约束**：启动 → 扫描 `com.xiaou` → `@MapperScan` 绑定各模块 Mapper → `ApplicationReadyEvent` 由 `StartupApplicationListener` 打印环境/端口/context-path/Swagger 地址。依赖 `xiaou-application` + 4 个 common 子模块 + mysql-connector-j(runtime) + undertow + springdoc + sa-token-alone-redis + sa-token-redis-jackson + jedis + actuator + micrometer-registry-prometheus。唯一启用 `spring-boot-maven-plugin`（`repackage`，`mainClass=com.xiaou.bootstrap.CodeNestApplication`）；`BootstrapAssemblyTest` 断言 scanBasePackages/exclude/注解/`@MapperScan` 取值，等于把装配契约写进测试；`@MapperScan` 显式列出 `com.xiaou.web.growthcoach.mapper`，是为 `xiaou-application` 开的后门。

### xiaou-application（xiaou-application）

- **职责与边界**：**不是纯聚合器**。`src/main` 111 个 Java 文件、`src/test` 22 个、`resources/mapper` 10 个 XML，**无自己的 `application.yml`**；三个真实功能域 `com.xiaou.web.growthcoach`（AI 成长教练，最大）、`web.home`（用户首页聚合）、`web.learning`（学习驾驶舱），加 `web.IndexController`；身份是"跨模块 BFF/编排 + growth-coach 领域实现"。
- **核心领域对象**：`domain/` 下 `GrowthEvidence`、`GrowthCodeArtifact`、`GrowthCodeReviewRecord`、`GrowthCoachActionRun`、`GrowthCoachActionEvent`、`GrowthCoachNudge`、`GrowthJourneyEvent`、`GrowthEvidenceProjectionCursor`、`GrowthGithubConnection`、`GrowthAnalyticsSnapshot`；DTO 约 30 个。
- **数据表**（由 mapper XML 反推）：`growth_journey_event`、`growth_evidence`、`growth_evidence_projection_cursor`、`growth_coach_action_run`、`growth_coach_action_event`、`growth_coach_nudge`、`growth_code_artifact`、`growth_code_review_record`、`growth_github_connection`；`GrowthAnalyticsMapper.xml` 另只读引用 `career_application_record`（归属模块未确认）。

**对外接口**（5 个 Controller，前缀含 context-path `/api`）：

| 类 | 路由前缀 | 主要方法 |
| --- | --- | --- |
| `IndexController` | `/` | `GET /`（欢迎语） |
| `UserGrowthCoachController` | `/user/growth-coach` | `GET /today-action` `/briefing` `/evidence` `/evidence-profile` `/skill-insights` `/capability-graph` `/career-next-action` `/application-outcomes` `/job-battle-gap` `/job-preparation-loop` `/job-market-signal` `/weekly-review`；`POST /career-next-action/{actionId}/complete`、`POST /journey-events`；`GET/POST /code-artifacts`、`POST /code-artifacts/preview`、`DELETE /code-artifacts/{artifactId}`；`POST /code-reviews`、`GET /code-reviews/latest`、`DELETE /code-reviews/{reviewId}`；`GET /github-connection`、`POST /github-connection/authorize`、`DELETE /github-connection`、`GET /github-connection/callback`；`POST /plan-adjustments/preview`、`POST /plan-adjustments/{runId}/confirm`、`POST /plan-adjustments/{runId}/cancel`、`GET /plan-adjustments/{runId}` |
| `AdminGrowthAnalyticsController` | `/admin/growth-analytics` | `GET /overview?days=1..90`（`@RequireAdmin`） |
| `UserHomeOverviewController` | `/user/home` | `GET /overview` |
| `LearningCockpitController` | `/user/learning-cockpit` | `GET /overview?targetRole&weeklyHours` |

- **关键流程**：首页聚合 `UserHomeOverviewService#getOverview` 把 11 个来源各自 `ResilientExecutor.executeAsync(..., 3s, applicationIoExecutor)` 并发限时执行，`allOf().join()` 后逐段读 `ResilientResult`，任一失败只把对应 section 标为不可用；growth-coach 另有证据投影（9 个 `GrowthEvidenceAdapter` 按 `sourceKey()` 采集变化 → `GrowthEvidenceProjectorService` 落库 → `GrowthEvidenceCompensationScheduler` 补偿）与 `GrowthCoachNudgeScheduler` 主动提醒，cron 取自 `xiaou.growth-coach.*`。
- **依赖关系**：34 个模块依赖 + `micrometer-core`；内部用 `port/`（`GrowthCareerDataPort`、`GrowthLearningResourcePort`、`GrowthEvidenceSourceCatalog`、`UserHomePresencePort`、`LearningRankSnapshotPort`）隔离外部持久化，`adapter/` 提供 MyBatis 与他模块实现。
- **约束与注意事项**：`type-aliases-package: com.xiaou.*.domain` 不覆盖 `com.xiaou.web.growthcoach.domain`，故 growth mapper XML 必须写全限定 `resultType`（如 `...domain.GrowthAnalyticsSnapshot`）；入口受开关 + 限流双重保护（`GrowthCoachFeatureGuard` → `GrowthCoachProperties` + `GrowthCoachRateLimiter`），关闭时抛 `BusinessException`；GitHub OAuth 回调只做固定跳转、不回传 code/错误详情。

### xiaou-common（xiaou-common）

- **职责与边界**：命名与内容不符。全模块 2 个文件：`utils/SensitiveWordUtils.java`（静态门面：`@Component` + 静态字段 + `@Autowired` setter 注入 `SensitiveCheckService`，内含本地缓存）+ `enums/package-info.java`（占位）。
- **核心领域对象 / 数据表**：`SensitiveWordUtils.SensitiveCheckResult{hit,processedText,allowed,riskLevel,action}`（builder 风格）；无表。
- **对外接口**：静态 `checkText`、`checkTextBatch`、`containsSensitiveWords`、`replaceSensitiveWords`、`isAllowed`、`clearCache`、`getCacheStats`。
- **关键流程 / 依赖 / 约束**：静态调用 → 本地缓存命中即返回 → 否则委托 `SensitiveCheckService` → 回写缓存。聚合 5 个 `xiaou-common-*` + web/validation/springdoc/redisson/mybatis/mysql/druid/p6spy/sa-token/jjwt/hutool/aop/pagehelper/actuator/micrometer/fastjson2 + `xiaou-sensitive-api`；pom 把整棵公共依赖树传递出去（`xiaou-application` 因此同时依赖 `xiaou-common` 与各 common-*），静态注入使单测与多实现替换困难。

### xiaou-common-core（xiaou-common-core）

- **职责与边界**：最底层纯 POJO/工具层，无 web、无持久化、无 Redis。
- **核心领域对象 / 数据表**：`Result<T>`、`ResultCode`（枚举）、`PageRequest`（接口）、`PageResult<T>`、`BusinessException`（extends RuntimeException，带 code）、`StatusEnum`、`exception/ai/*` 5 个异常、`annotation/Log`、`constant/Constants`、`utils/{AvatarUtils,DateHelper,JsonUtils,ManualPageHelper,RelativeTimeUtil}`；无表。
- **对外接口**：`Result.success()/success(T)/success(String,T)/error()/error(String)/error(Integer,String)/result(ResultCode[,T])`、`isSuccess()`；`ResultCode.getByCode(Integer)` 线性查找，未命中回落 `ERROR(500)`。
- **关键流程 / 依赖 / 约束**：依赖 lombok、hutool-all、jackson-databind、fastjson2、spring-core、slf4j-api。`ResultCode` 同时承载传输语义码（400/401/403/404/405/408/409/415/429/500/503）与业务码（600–604、701–705、801–805），与 `ResultHttpStatusMapper` 构成需同步维护的双向映射；`Constants.TOKEN_EXPIRE_TIME = 7200L` 与 `sa-token.timeout = 604800` 不一致（疑为遗留）。

### xiaou-common-web（xiaou-common-web）

- **职责与边界**：HTTP 横切，6 个类 + 1 个测试；无自有领域对象、无表。
- **对外接口**：`GlobalExceptionHandler`（约 20 个 `@ExceptionHandler`）、`ResultHttpStatusAdvice`、`ResultHttpStatusMapper.resolve(Integer)`（包级私有 final）、`CorsFilter` bean、`/files/**` 静态映射、`IPUtil`。
- **关键流程 / 依赖 / 约束**：见"请求链路"第 6、7、8 步；依赖 `xiaou-common-core`、spring-webmvc、validation、springdoc-openapi-starter-webmvc-api、sa-token-spring-boot3-starter。CORS 白名单来自 `@Value("${xiaou.cors.allowed-origin-patterns:...}")`，**默认只含 localhost:3000/3001/5173**；全仓 `*.yml` grep `cors` 零命中，即所有 profile 均未覆盖该键，生产依赖外部注入。

### xiaou-common-security（xiaou-common-security）

- **职责与边界**：鉴权横切，8 个类；无表（会话落 Sa-Token 独立 Redis `database: 4`）。
- **核心领域对象**：`RequireAdmin`（注解，含 `message`）、`SaTokenConfig`、`StpAdminUtil`/`StpUserUtil`（各持 `new StpLogic("admin")`/`("user")`）、`StpInterfaceImpl`、`AdminAuthAspect`、`SaTokenUserUtil`、`PasswordUtil`。
- **对外接口**：两套同构静态 API：`login/logout/isLogin/checkLogin/getLoginId*/getTokenValue*/kickout*/getSession*/set/get/delete/hasRole/checkRole/hasPermission/checkPermission/disable/isDisable/untieDisable/getDisableTime`。
- **关键流程 / 依赖 / 约束**：请求 → `SaInterceptor` 路由匹配 → `checkLogin()` → 未登录抛 `NotLoginException` → 全局处理器转 401/701|702；依赖 `xiaou-common-core`、sa-token-spring-boot3-starter、spring-webmvc、aop、aspectjweaver、hutool、lombok。`/user/team/**` 匿名白名单硬编码在 Java 内，新增匿名接口必须改此文件；`StpInterfaceImpl` 的权限/角色硬编码为 `["admin"]`/`["user"]`，无按用户查询，故 `@RequireAdmin` 只能区分"管理员 vs 普通用户"。

### xiaou-common-cache（xiaou-common-cache）

- **职责与边界**：KV 与文本状态的 Redis 访问边界，5 个类 + 2 个测试；无表（业务数据用 Redisson `database: 3`）。
- **核心领域对象**：`CacheStore`（接口）、`RedisValueStore`、`TextStateStore`（接口）、`RedisTextStateStore`、`RedisConfig`（空 `@Configuration`，自定义示例被注释掉）。
- **对外接口**：`CacheStore` → `find/take/put/put(ttl)/delete/exists/expire/increment/increment(ttlWhenCreated)/counter/setCounter/keys`；`TextStateStore` → `find/put/put(ttl)/delete`。
- **关键流程 / 依赖 / 约束**：纯适配，无编排；依赖 `xiaou-common-core`、redisson-spring-boot-starter、spring-boot-starter-data-redis、commons-pool2。`RedisConfig` 无实际 bean，将来加 `@Bean` 需自行 `@ConditionalOnMissingBean`（类内注释已提示）；Sa-Token 走 `sa-token-alone-redis` + Jedis 独立连接池，与业务 Redisson 分离，勿混淆两组调优参数。

### xiaou-common-persistence（xiaou-common-persistence）

- **职责与边界**：仅 2 个类，是 MyBatis/Druid/P6Spy/PageHelper 的依赖载体 + SQL 日志格式化；无表。
- **核心领域对象**：`P6SpyLogger implements MessageFormattingStrategy`（按耗时打 🐢/⚠️/⏱️/⚡ 标记并框线格式化）、`utils/PageHelper`。
- **对外接口**：`P6SpyLogger#formatMessage(...)`；`PageHelper` 静态分页方法（未逐行读取）。
- **关键流程 / 依赖 / 约束**：dev profile 用 `P6SpyDriver` 代理 → P6Spy 捕获 SQL → 格式化；`application.yml` 已关闭 MyBatis 原生 SQL 日志并设 `com.xiaou: INFO` 以避双写。依赖 `xiaou-common-core`、mybatis-spring-boot-starter 3.0.3、druid-spring-boot-starter 1.2.20、p6spy 3.9.1、pagehelper-spring-boot-starter、lombok。不含 Mapper 扫描配置（由 `xiaou-bootstrap` 的 `@MapperScan` 与 `mapper-locations` 负责）；docker profile 不用 P6Spy 驱动，日志差异属预期。

### xiaou-resilience（xiaou-resilience）

- **职责与边界**：跨模块可复用的容错执行器，刻意只依赖 spring-context + slf4j；无表。
- **核心领域对象**：`ResilientResult<T>{value,status,elapsedNanos}`、`ResilientStatus{SUCCESS, EMPTY, FAILED, TIMEOUT}`（`value==null` 记 EMPTY）。
- **对外接口**：`ResilientExecutor#execute(String,Supplier)`、`#executeAsync(String,Supplier,Duration,Executor)`（`@Component`）。
- **关键流程 / 依赖 / 约束**：`supplyAsync` + `orTimeout` → `handle` 判定 `TimeoutException`→TIMEOUT、`Error` 直接抛出、其他→FAILED 并 `log.warn`（只记异常类名）。失败被吞成状态值，调用方必须显式检查 `status`/`hasValue()`，否则静默降级；参数校验严格（空 operation、非正 timeout 直接抛 `IllegalArgumentException`）；目前消费者为 `xiaou-application`（首页聚合）与 `xiaou-system`。

### xiaou-user-api（xiaou-user-api）

- **职责与边界**：用户域契约 jar，只依赖 lombok(optional)，无 Spring Bean，避免循环依赖（pom 注释明示）；无表。
- **核心领域对象**：`SimpleUserInfo`（DTO）。
- **对外接口**：`UserInfoApiService#getSimpleUserInfo(Long)`、`#getUserDisplayName(Long)`、`#getSimpleUserInfoBatch(List<Long>)`。
- **关键流程 / 依赖 / 约束**：无编排；实现由 `xiaou-user/.../UserInfoApiServiceImpl` 提供，同 JVM 注入。依赖方：`xiaou-user`（实现）、`xiaou-application`、`xiaou-blog`、`xiaou-codepen`、`xiaou-moment`、`xiaou-plan`、`xiaou-oj`、`xiaou-flashcard`、`xiaou-notification`、`xiaou-points`、`xiaou-team`。契约与实现分离，但无编译期约束阻止使用方直接依赖 `xiaou-user` 实现类；新增跨模块用户能力应优先扩此接口。

### xiaou-sensitive-api（xiaou-sensitive-api）

- **职责与边界**：敏感词域契约 jar，依赖 spring-context、lombok、jackson-annotations，不含实现；无表。
- **核心领域对象**：`SensitiveCheckRequest`、`SensitiveCheckResponse`（DTO）。
- **对外接口**：`SensitiveCheckService#checkText`、`#checkTextBatch`、`#containsSensitiveWords(String,String)`、`#replaceSensitiveWords(String,String)`、`#isAllowed(String,String,Long,Long)`、`#refreshWordLibrary()`。
- **关键流程 / 依赖 / 约束**：无编排；实现由 `xiaou-sensitive/.../SensitiveCheckServiceImpl` 提供。依赖方：`xiaou-common`（静态门面）、`xiaou-sensitive`（实现）、`xiaou-blog`、`xiaou-codepen`、`xiaou-moment`。客户端有两条路径（`SensitiveWordUtils` 静态门面带缓存 vs 直接注入 `SensitiveCheckService` 无缓存），语义一致性取决于调用方选择，属双入口风险。

## 跨模块观察

**共性模式**

1. **契约 jar**（`xiaou-user-api`/`xiaou-sensitive-api`）：接口 + DTO、极窄依赖、实现在业务模块，用 Maven 依赖方向替代 RPC；目前仅两例。
2. **Port/Adapter**：`xiaou-application` 用 `port/` + `adapter/persistence|module` 隔离他模块持久化；`GrowthEvidenceAdapter`（9 个实现）是策略+注册表式扩展点。
3. **降级优先**：`ResilientExecutor` + `sections` 状态字段把"部分失败"当正常返回而非异常。
4. **双层状态语义**：业务码（`Result.code`）与 HTTP 状态（`ResultHttpStatusAdvice`）解耦，前端依赖 200/701/702/703/704。
5. **能力开关默认关闭**：growth-coach 与 SRE 高风险能力均 `false` 起步，靠环境变量开启。

**重复与冗余**

- 根 `pom.xml` 定义 `pagehelper.version = 6.1`，但 `dependencyManagement` 硬编码 `pagehelper-spring-boot-starter 2.1.1`，该属性未被引用（死配置）。
- `xiaou-common` 与 5 个 `xiaou-common-*` 依赖重复声明（`xiaou-application` 同时依赖两者）。
- `StpAdminUtil` 与 `StpUserUtil` 为逐方法复制（各约 315 行），差异仅 `loginType`。
- `ResultCode` 与 `ResultHttpStatusMapper` 维护同一份码→语义映射。
- springdoc/fastjson2 版本在 `xiaou-common`、`xiaou-common-core` 内以字面量重复声明，未走根属性。

**潜在风险**

1. **open-by-default 路由**：拦截器只覆盖 `/auth`、`/admin`、`/user` 三组前缀；`xiaou-sensitive` 7 个 `/sensitive/**` Controller 靠 `@RequireAdmin` 兜底，漏标注解即暴露管理功能。
2. **应用层归属漂移**：`always.md` 规定业务逻辑不得塞进 `xiaou-bootstrap` 或 `xiaou-application`，而 `xiaou-application` 实持 111 个主源码文件、9 张 `growth_*` 表、独立 mapper XML 与定时任务——规则与实现需对齐其一。
3. **`application-prod.yml` 为空**：prod profile 无 datasource/redis 配置，`application-sec.yml` 又不在仓库中，生产可用性完全依赖部署外挂文件；`docker` 反而是唯一全环境变量化的 profile。
4. **`xiaou.cors.allowed-origin-patterns` 无任何 yml 覆盖**：生产未注入时只允许 localhost 源（前端跨域失败的潜在原因）。
5. **dev 明文口令**：`application-dev.yml` 把数据库口令以明文硬编码（此处不复制值）；`application.yml` 中 OJ 沙箱 `go-judge-url: http://154.222.18.220:5050` 为硬编码公网地址。
6. **legacy `jwt.*` 残留**：仍配置 HS512 secret 与过期时间，但鉴权已全走 Sa-Token，误用会产生两套令牌语义。
7. **`StpInterfaceImpl` 无真实权限来源**：任何细粒度权限设计都无法落地。
8. **`type-aliases-package` 与包结构错配**：`com.xiaou.*.domain` 匹配不到 `com.xiaou.web.growthcoach.domain`，靠全限定 `resultType` 硬撑，新增 growth 实体易漏写别名。
9. **`ResilientExecutor` 静默降级**：FAILED/TIMEOUT 只记异常类名，缺业务标识与用户维度，排障困难。

**与 `.agent/rules/always.md` 的对照**

| 规则 | 现状 |
| --- | --- |
| 分层、启动类、9999/`/api`、`Result` 路径、Sa-Token 工具类路径 | 一致（文件均已核对存在） |
| "业务逻辑不得塞进 `xiaou-bootstrap` 或 `xiaou-application`" | **不一致**：`xiaou-application` 承载完整 growth-coach/home/learning 业务 |
| `application-sec.yml` 位于 `xiaou-bootstrap/src/main/resources/` | 仓库中不存在该文件（glob 未命中），仅被 `optional:` 导入 |
| 返回码 200/701/702/703/704 不得改动 | 一致：`ResultCode`、`GlobalExceptionHandler`、`ResultHttpStatusMapper` 三处对齐 |
| 路由分区（`/auth`、`/admin`、`/user`，公共 `/oj`、`/community`、`/version`） | 基本一致，但 `/sensitive/**` 既未被规则登记也未被拦截器覆盖（第三类"仅注解保护"路由） |
| 新增模块须同步根 `pom.xml` 与 `xiaou-application/pom.xml` | 一致（36 个 module、34 个依赖均已登记） |

## 铁律

1. 本文件只记录已读取文件中可指回路径的事实；标注"未确认"处不得据以改动代码。
2. 修改 `Result`/`ResultCode`/`SaTokenConfig`/`ResultHttpStatusMapper` 属跨模块契约变更，必须同步前端拦截器与全局异常处理测试。
3. 新增受保护路由前先确认它落在 `/auth|/admin|/user` 前缀内，否则必须显式加注解并复核拦截器规则。
4. 新增业务能力不得继续堆入 `xiaou-application`；若确需扩展，先更新 `always.md` 落点规则与 `scripts/check-architecture.py`。
5. 生产配置（datasource、redis、`xiaou.cors.*`）必须外部注入补齐，禁止依赖 `application-prod.yml` 当前内容。
