---
artifact: spec
status: active
scope: 运维、可观测性与发布流程
---

# 运维、可观测性与发布

本规格描述 Code-Nest 的 SRE 领域模块（`xiaou-sre`）、部署拓扑（`deploy/`、`docker/`）、发布与运维脚本（`scripts/`）以及顶层发布文档（`RELEASE.md`）。当前版本 2.5.8（`VERSION`；根 `pom.xml` 的 `<revision>` 为 `v2.5.8`，见 `.agent/rules/always.md`）。

`xiaou-sre` 不是运维后台 UI，也不是监控数据面本体：它是"告警接收 → 事故聚合 → 证据采集 → 只读 RCA 调查 → RCA 离线评测"的后端领域服务；Prometheus/Grafana/Alertmanager/blackbox 由 `docker/monitoring` 的独立容器栈提供，管理界面由 `vue3-admin-front` 的 operations 路由切片承载。

## 总览

| 组件 | 职责一句话 | 关键入口（文件/目录） |
| --- | --- | --- |
| 启动壳 | 唯一可执行 JAR 与运行时配置 | `xiaou-bootstrap`（端口 9999，context-path `/api`） |
| xiaou-sre | 告警/事故/证据/RCA/评测的领域服务 | `xiaou-sre/`，`xiaou-sre/src/main/java/com/xiaou/sre/**` |
| 反向代理 | 双前端静态托管 + `/api` 反代 + 内部端点封禁 | `deploy/nginx/code-nest-production.conf` |
| 进程管理 | 后端服务与每周容量治理定时器 | `deploy/systemd/code-nest-capacity-governance.{service,timer}` |
| 监控栈编排 | Prometheus/Alertmanager/Grafana/导出器 | `docker/monitoring/docker-compose.yml` |
| 告警规则与拨测 | 录制规则、12 条告警、blackbox http_2xx | `docker/monitoring/alert_rules.yml`、`blackbox/config.yml` |
| 发布编排（CI） | 干净树校验 → 构建 → 打包 → 烟测 → 触发部署 | `scripts/ci-server-build-deploy.sh` |
| 发布执行（服务器） | 备份 → 替换 → 健康检查 → 失败回滚 | `scripts/deploy-release.sh` |
| 手工发布入口 | 本地构建 + scp/ssh 或 paramiko 推送 | `scripts/deploy-production.py`、`scripts/deploy-frontends.py` |
| 版本单一事实源 | manifest 与其全部投影一致性 | `scripts/release_manifest.py`、`release/manifest.json` |
| 架构门禁 | Maven 无法表达的模块缝 | `scripts/check-architecture.py` |
| 数据库迁移 | checksum ledger + advisory lock | `scripts/db-migrate.py` |
| 发布文档 | 版本号、分支、检查项、回滚策略 | `RELEASE.md` |

## xiaou-sre（xiaou-sre）

**职责与边界**

- 191 个 Java 文件 / 4 个 Controller / 17 个 Mapper / 36 个测试文件；`resources/` 仅 `mapper/*.xml`（17 个）与 `sre/runbooks.json`。
- 依赖面极窄（`xiaou-sre/pom.xml`）：仅 `xiaou-common-core`、`-web`、`-security`、`-persistence` 与 `micrometer-core`。不依赖 `xiaou-ai`、`xiaou-system`；反向由 `check-architecture.py` 强制（`xiaou-ai` 必须依赖 `xiaou-sre`）。
- 入站数据面：Alertmanager webhook（`/internal/sre/alertmanager/v1/alerts`）。出站读数据面：按开关调用 Prometheus/Loki HTTP API（`client/SrePrometheusClient`、`client/SreLokiClient`）。
- 不拥有：指标抓取与存储（Prometheus）、看板（Grafana）、通知投递（Alertmanager 的 email/webhook）、进程与主机层（systemd/node-exporter）。

**核心领域对象**（`domain/`，19 个类）

| 对象 | 说明 |
| --- | --- |
| `SreAlertEvent` | 单条告警事件，按 `(fingerprint, startsAt)` 去重 |
| `SreIncident` / `SreIncidentAlertRelation` | 事故及其与告警的多对多关联 |
| `SreIncidentEvidence` | 事故证据条目 |
| `SreOutboxEvent` | 事务性 outbox 事件（`INCIDENT` + `EVIDENCE_COLLECTION_REQUESTED`） |
| `SreInvestigationRun/Step/Artifact/Feedback` | 只读 RCA 调查运行、步骤、产物与人工反馈 |
| `SreRcaEvaluationCase/Suite/SuiteVersion/SuiteCase/Run/RunCase/Result` | 离线评测用例、套件与版本快照、运行与逐例结果 |

**对外接口**

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/internal/sre/alertmanager/v1/alerts` | 机器 webhook；成功返回 202 ACCEPTED；`SreValidationException` → 400 |
| GET | `/api/admin/sre/incidents/summary` | 事故汇总 |
| GET | `/api/admin/sre/incidents` | 分页列表（state/severity/service 过滤，pageSize ≤ 100） |
| GET | `/api/admin/sre/incidents/{id}` | 详情 |
| GET | `/api/admin/sre/incidents/{id}/evidence` | 证据列表 |
| GET | `/api/admin/sre/incidents/{id}/investigation-context` | 调查上下文 |
| POST | `/api/admin/sre/incidents/{id}/ack` | 确认（写操作日志） |
| POST | `/api/admin/sre/incidents/{id}/resolve` | 关闭事故 |
| POST | `/api/admin/sre/incidents/{id}/rca` | 触发只读 RCA（`SreRcaTriggerSource.ADMIN_API`） |
| GET | `/api/admin/sre/incidents/{id}/rca-runs` | RCA 历史（limit ≤ 50） |
| GET | `/api/admin/sre/incidents/{id}/rca-runs/{runId}` | RCA 详情 |
| PUT | `/api/admin/sre/incidents/{id}/rca-runs/{runId}/feedback` | 人工评价 |
| GET | `/api/admin/sre/incidents/{id}/rca-runs/{runId}/evaluation-sample` | 评测样本导出（需先反馈） |
| POST | `/api/admin/sre/incidents/{id}/rca-runs/{runId}/evaluation-cases` | 反馈提升为评测用例 |
| GET/POST | `/api/admin/sre/rca-evaluations/cases`、`/suites`、`/suites/{id}/versions` | 用例与套件及版本 |
| GET | `/api/admin/sre/rca-evaluations/suite-versions/{versionId}` | 套件版本详情 |
| POST/GET | `/api/admin/sre/rca-evaluations/runs` | 入队（202）与历史 |
| GET | `/api/admin/sre/rca-evaluations/runs/{runId}`、`/gate` | 运行详情与质量门禁结论 |

所有 admin 端点均带 `@RequireAdmin`；写操作带 `@Log` 且关闭请求/响应体落盘。

**数据表**（`sql/v2.5.0/*.sql`、基线 `sql/MySql/code_nest.sql`，共 16 张）

`sre_alert_event`、`sre_incident`、`sre_incident_alert_relation`、`sre_outbox_event`、`sre_incident_evidence`、`sre_investigation_run`、`sre_investigation_artifact`、`sre_investigation_step`、`sre_investigation_feedback`、`sre_rca_evaluation_case`、`sre_rca_evaluation_suite`、`sre_rca_evaluation_suite_version`、`sre_rca_evaluation_suite_case`、`sre_rca_evaluation_run`、`sre_rca_evaluation_run_case`、`sre_rca_evaluation_result`。第 17 个 Mapper（`SreOperationalMetricsMapper`）服务数据库 gauge，无独立业务表。

**关键流程**（`SreAlertIngestionServiceImpl` → `SreOutboxEventProcessorImpl`）

1. webhook 批量校验：`alerts` 非空、单批 ≤ 100 条、`source` ≤ 32 字符、`fingerprint` 必填 ≤ 128 字符、`status` 仅 `firing|resolved`、时间戳必须 ISO-8601、`rawPayload` ≤ 1,000,000 字符、labels/annotations ≤ 100 条且 key ≤ 128 / value ≤ 2000。
2. 去重键 `(fingerprint, startsAt)` 命中且状态相同 → 计为 duplicate 并返回；状态变化则更新事件载荷。
3. 事故聚合键 `incidentKey = service|alertName`；`service` 取 label `service` 或 `job`，`severity` 默认 `warning`；`incidentNo` 形如 `SRE-<12 位大写十六进制>`；已 RESOLVED 的事故在再次 FIRING 时 `reopen`。
4. 告警恢复时用 `countActiveByIncidentId == 0` 判定事故转 RESOLVED。
5. 同事务写入 outbox 事件 `EVIDENCE_COLLECTION_REQUESTED`（state `PENDING`，attempts 0）。
6. Outbox 处理器在同一事务内执行 `collect(event)` 并 `markSucceeded`；异常回滚，交由下一轮按退避重试（进程中断可恢复）。
7. Alertmanager 的 `Instant` 统一按 `Asia/Shanghai` 转 `LocalDateTime` 落库（源码注释明确说明防止 8 小时偏移）。

**依赖关系**：Micrometer 指标 + Mapper + `SreLokiClient`/`SrePrometheusClient`（受开关控制）+ `SreReadOnlyInvestigationToolService`（RCA 只读工具）。运维证据来源为 classpath `sre/runbooks.json` 与 build provenance 快照（`DEPLOYMENT_SNAPSHOT`、`RUNBOOK_SNAPSHOT`，快照 ≤ 32,000 字符），即证据采集是**只读**的，不执行任何运维动作。

**约束与注意事项**

- 默认全部关闭：`application.yml` 中 `xiaou.sre.metrics.enabled=false`、`webhook.enabled=false`、`outbox.enabled=false`、`evaluation.enabled=false`、`prometheus.enabled=false`、`loki.enabled=false`。默认部署下只有 admin 只读接口与按需触发的 RCA 生效。
- 关键开关与密钥：`XIAOU_SRE_WEBHOOK_TOKEN`、`XIAOU_SRE_WEBHOOK_MAX_REQUESTS_PER_MINUTE`（默认 120）、`XIAOU_SRE_WEBHOOK_MAX_BODY_BYTES`（默认 2,000,000）、`XIAOU_SRE_OUTBOX_{BATCH_SIZE,MAX_ATTEMPTS,LEASE_SECONDS,RETRY_BACKOFF_SECONDS,MAX_RETRY_BACKOFF_SECONDS}`、`XIAOU_SRE_EVALUATION_MAX_{CASES_PER_RUN,DURATION_SECONDS}`。
- build provenance 三件套 `XIAOU_SRE_EVALUATION_{SOURCE_REVISION,BUILD_ID,BUILD_VERSION}` 由部署脚本写入环境文件（见下），是"评测绑定某次构建"的锚点。
- 时间字段语义已在源码中显式约定，改动时区会破坏对账。

## 部署与发布

**部署拓扑**

| 层 | 归属 | 要点 |
| --- | --- | --- |
| 反向代理 | `deploy/nginx/code-nest-production.conf` | `:80`/`:81`→`/var/www/code-nest-user`，`:82`→`/var/www/code-nest-admin`；`/api/` 反代 `127.0.0.1:9999`（keepalive 64，超时 300s）；显式 `return 404` 封禁 `/api/actuator`、`/api/actuator/`、`/api/internal/sre`、`/api/internal/sre/`；`server_name`/`/admin/` 跳转硬编码 `36.140.150.167` |
| 应用进程 | `deploy/systemd` + 服务器文件 | `/opt/code-nest/app/app.jar`；`code-nest-capacity-governance.timer` 每周日 03:30（Asia/Shanghai，`RandomizedDelaySec=15m`）触发 oneshot `--apply`，服务带 `ProtectSystem=strict`、`ReadWritePaths=/opt/code-nest/...` |
| 监控栈 | `docker/monitoring/docker-compose.yml` | 6 个服务全部 `network_mode: host`，全部绑定回环：Prometheus 19090、Alertmanager 19093/19094、Grafana 3000、node-exporter 19100、blackbox 19115；Alertmanager 以 UID 65534 运行并从 `./secrets/` 只读挂载 `qq_smtp_auth_code`、`sre_webhook_token` |
| 说明 | 仓库实际布局 | 任务描述中的 `deploy/{env,monitoring,ai,go-judge}` 不存在；实际为 `deploy/{nginx,systemd}` 与 `docker/{env,monitoring,ai,go-judge,Dockerfile}` |

目录约定（`deploy-release.sh` 默认值）：`/opt/code-nest/{app,ops,monitoring,bin,backups/releases}`、`/var/www/code-nest-{user,admin}`、`/etc/code-nest/code-nest.env`、`/etc/nginx/conf.d/code-nest.conf`、`/etc/systemd/system`。

**发布流程与脚本调用顺序**

1. `scripts/ci-server-build-deploy.sh`：`git status --porcelain` 干净树校验（除非 `CODE_NEST_ALLOW_DIRTY_BUILD=true`）→ `release_manifest.py validate` → `mvn -B -pl <artifacts.backend.module> -am clean package -DskipTests -Drevision=<mavenRevision>` → 双前端 `npm ci --ignore-scripts` + `npm run build` → 组装 stage（`backend/`、`admin/`、`user/`、`ops/`、`scripts/`、`sql/`、`VERSION`、`RELEASE`、`release-manifest.json`）→ `tar -czf` → `release-smoke-test.py <bundle>` → `sudo -n /opt/code-nest/actions-runner/deploy-from-workspace.sh <version> <reload_nginx> <bundle>`（该脚本在服务器上、不在仓库中）。
2. `scripts/deploy-release.sh deploy <bundle>`（服务器端唯一权威执行者）：`validate_archive_entries`（拒绝绝对路径与 `..`）→ 解包 → `validate_stage`（校验必需产物，含硬编码 `sql/v2.5.3/production_governance.sql`；拒绝符号链接）→ 读取 `RELEASE` 的 `version/sha/build_id` 并校验格式与 `CODE_NEST_RELEASE_VERSION` 一致 → `release-smoke-test.py` → 可选 `db-migrate.py`（仅 `CODE_NEST_RUN_MIGRATIONS=true`）→ `backup_current` → 替换 app.jar/RELEASE/双前端 → `install_deployment_helper`（自我刷新）→ `install_operational_assets` → `update_app_provenance` → 权限收口 → `nginx -t` → `validate-config.sh` → `systemctl daemon-reload` + 定时器启用 → `compose up -d` + `restart prometheus` + 就绪等待 → `systemctl reload nginx` → `systemctl restart code-nest.service` + 健康检查（30 次 × 2s）→ `verify-production-baseline.sh`。任一步失败 → `rollback_from_backup` 并返回同一退出码；成功 → `cleanup_old_backups`（保留 4 份）。
3. 手工路径：`deploy-production.py`（本地 `mvn`+双前端构建 → 组装 → 烟测 → scp bundle 与 `deploy-release.sh` 到 `/tmp` → ssh 执行 `deploy <bundle>`）；`deploy-frontends.py`（paramiko 密码登录，上传 `dist` 归档，远端备份到 `/var/backups/code-nest-<app>-<stamp>`，默认 `nginx -t && systemctl reload nginx`）。
4. 安全护栏：`assert_safe_app_root`/`assert_safe_target_dir`/`assert_safe_backup_dir`/`assert_safe_operational_targets` 拒绝写入 `/`、`/opt`、`/var`、`/root`、`/home` 等；`sync_dir` 先清空目标目录再拷贝。

**环境与密钥注入**

- 应用侧：`/etc/code-nest/code-nest.env` 是唯一应用环境文件，部署时被 `update_app_provenance` 原子改写（保留 mode/uid/gid），写入 `XIAOU_SRE_EVALUATION_{SOURCE_REVISION,BUILD_ID,BUILD_VERSION}`（`build_version` 去掉 `v` 前缀）。该文件被完整备份与回滚。systemd unit（`code-nest.service`）不在仓库内，其 `EnvironmentFile` 与 JVM 参数**未确认**。
- 监控侧：`docker/monitoring/.env`（mode 600）承载镜像地址与端口；secret 文件 `secrets/qq_smtp_auth_code`、`secrets/sre_webhook_token`（mode 400，uid/gid 65534）；Alertmanager 用 `smtp_auth_password_file` 与 `credentials_file: /run/secrets/sre_webhook_token` 读取，不落明文。
- webhook 令牌经 `Authorization: Bearer` 头传给 `http://127.0.0.1:9999/api/internal/sre/alertmanager/v1/alerts`；应用侧读取 `XIAOU_SRE_WEBHOOK_TOKEN`。
- CI 侧：代理与 Node 路径通过 `CODE_NEST_HTTP_PROXY`/`CODE_NEST_HTTPS_PROXY`/`CODE_NEST_NODE_HOME` 注入；SSH/密码凭据走 `CODE_NEST_DEPLOY_{HOST,USER,PORT,KEY,PASSWORD}` 环境变量，`deploy-frontends.py` 声明密码永不落盘。

**版本与发布清单的关系**：`release/manifest.json` 是唯一事实源，`release_manifest.py validate` 同时校验定义（`manifestSchema=1`、必需字段、路径安全、`bundle.requiredPaths` 无大小写碰撞且含 `RELEASE`/`versionFile`/`manifestPath`/backend jar/双端 `index.html`/必需迁移）与投影（`VERSION`、根 `pom.xml` 的 `<revision>` 与 `<modules>`、全部 `pom-xml-flattened`、双前端 `package.json`、`package-lock.json` 顶层与链接包版本、`projections.environment` 指定的 env 键值）。`version=2.5.8` 派生 `releaseVersion=mavenRevision=docker.tag=v2.5.8`；`database.schemaVersion=v2.5.3` 与当前版本解耦。`bundle.requiredPaths` 被 `release-smoke-test.py` 与服务器端 `validate_stage` 双重使用。

**回滚与基线校验**

- 回滚：`deploy-release.sh rollback <backup-dir>` 恢复 app.jar、RELEASE、环境文件、双前端与 ops/monitoring/systemd/bin 托管文件，然后重跑 nginx/monitoring/systemd/服务健康检查；备份目录元数据 `metadata.env` 记录版本与路径。数据库迁移**不做破坏性回滚**（`RELEASE.md`）。
- `verify-production-baseline.sh` 逐项 PASS/FAIL/WARN 校验：必需命令；`code-nest.service` 与 `nginx.service` active；`/api/actuator/health` 返回 `status=UP`；`:81/`、`:82/` 返回 200；双端口上 `/api/actuator`、`/api/actuator/health`、`/api/internal/sre`、`/api/internal/sre/alertmanager/v1/alerts` 均返回 404（内部端点不得外露）；Prometheus `/api/v1/targets` 至少 4 个 target 且全部 `up`（最多 12 次 × 5s 重试）；Grafana `/api/health` 的 `database=ok`（`--require-grafana` 时失败、否则 WARN）；`RELEASE` 的 `version`/`sha`（`sha` 用前缀比对）与期望值一致；`monitoring/secrets` 目录 711、`.env` 600、两个 secret 400 且非空、所有权 65534:65534；`GRAFANA_ADMIN_PASSWORD` 非占位符且 ≥ 16 位；`/opt/code-nest` 可用空间 ≥ 8 GiB；15 个受管文件与 `/opt/code-nest/ops` 参考副本逐字节一致（`cmp -s`，即配置漂移检测）。

## 可观测性

- **指标**：`xiaou-sre` 用 Micrometer 记录 `xiaou.sre.alerts.ingested|ingestion.errors|duplicates`、`evidence.collections|collection.duration.by_source|collection.errors`、`investigation.runs|duration|tool.calls|tool.duration`、`queue.events`、`incidents.open`（Gauge）、`outbox.lease.recoveries|worker.runs|worker.duration|events|event.duration`（`metrics/SreMetricsRecorder.java`、`SreOutboxMetricsRecorder.java`）。指标记录失败一律 `log.warn` 降级，不影响主流程。数据库 gauge 由 `xiaou.sre.metrics.enabled` 控制（默认 false，源码注释说明依赖完整 SRE 迁移）。
- **抓取**：Prometheus `job=code-nest` 走 `/api/actuator/prometheus`，10s 间隔，target 来自 `targets/code-nest.local.yml`；`job=node-exporter`、`job=blackbox-http`（module `http_2xx`，接受 200/204/301/302）。
- **告警**：`alert_rules.yml` 定义 6 条录制规则与 12 条告警，覆盖可用性（`CodeNestTargetDown`、`CodeNestPublicEndpointDown`）、应用（5xx>5%、P95>2s、堆使用>85%）、主机（磁盘<15%/<5%、CPU>90%）与 SRE 运行时（Outbox 停滞、评测队列停滞、`terminal_failure`、`deadline_exceeded`、租约恢复>3 次/15m、RCA 失败率>25%、RCA 耗时>120s）。
- **路由**：Alertmanager 按 `alertname/job/instance` 分组，critical 1h / 默认 4h 重复；critical 抑制同 alertname+instance 的 warning；接收器 `qq-email-and-sre-webhook` 同时发 QQ 邮件与 SRE webhook（`send_resolved: true`）。
- **日志**：`xiaou-sre` 内未见 Loki 之外的日志汇聚配置；`SreLokiClient` 用于 RCA 只读查询（默认关闭，回看 15m、最大范围 60m）。日志落盘/轮转由宿主层负责，**未确认**。
- **看板**：`docker/monitoring/grafana/dashboards/code-nest-application.json`、`code-nest-sre.json`，通过 `provisioning/dashboards/code-nest.yml` 与 `datasources/prometheus.yml` 自动装载；Grafana 禁止匿名、禁止注册、禁止嵌入。
- **外部拨测**：`scripts/external-uptime-check.sh` 对 `http://36.140.150.167:81/`、`:82/` 重试 3 次、间隔 10s、连接超时 5s、总超时 15s（可被 `CODE_NEST_UPTIME_*` 覆盖）。
- **端到端自检**：`scripts/sre-alertmanager-e2e.py` 随发布包下发到 `/opt/code-nest/bin`，内容**未读**。

## 质量门禁脚本

| 脚本 | 检查规则 | CI 用法 |
| --- | --- | --- |
| `check-architecture.py` | ① 只有 `xiaou-bootstrap` 可启用 `spring-boot-maven-plugin`；② 新增对遗留 `xiaou-common` 的依赖仅限 7 个白名单模块，且白名单可缩小不容冗余；③ `xiaou-common-core` 不得依赖内部模块，其余 foundation 模块只能依赖 `xiaou-common-core`；④ `xiaou-sre` 不得依赖 `xiaou-ai`/`xiaou-system`，`xiaou-ai` 必须依赖 `xiaou-sre`；⑤ `xiaou-resilience` 不得依赖领域模块，`xiaou-application`/`xiaou-system` 必须依赖它，4 个聚合器必须用 `ResilientExecutor` 且不得自行实现超时/降级；⑥ `xiaou-application` 不得存放启动类与运行时 YAML，跨模块 mapper/domain import 必须走 adapter；⑦ `xiaou-sre` 不得 import `xiaou-ai`/`xiaou-system`，Alertmanager 传输 DTO 仅限 internal controller 包；`xiaou-ai` 的 SRE 依赖仅限 `com.xiaou.ai.sre`；`xiaou-system` 只保留 1 个白名单 adapter；⑧ 通知只能 import `com.xiaou.notification.api`（且 `xiaou-common` 不得残留通知类型）；⑨ 缓存只能依赖 `CacheStore`/`TextStateStore`，禁止 `StringRedisTemplate`/`RedisTemplate`，Redisson 直连需 8 个白名单 adapter；⑩ 共享契约 `code-nest-api-contract` 三件套存在、双前端以 `file:../code-nest-api-contract` 引用、`request.js` 不得重复解包响应码、路由入口 < 120 行且六个路由切片齐全、通知页用 `createdTime` 与共享 DTO；通知 controller 不得暴露 `domain`；`GlobalExceptionHandler` 不得一律返回 200 且必须有 `ResultHttpStatusAdvice` | 打印 `architecture violation:` 并退出 1；`RELEASE.md` 列为发布验证项 |
| `check-version-consistency.py` | 8 行兼容入口，`raise SystemExit(release_manifest_main(["validate"]))`，即等价于 `release_manifest.py validate`（定义 + 全投影一致） | `RELEASE.md` 各版本验证清单均引用它；`always.md` 要求改版本后运行 |
| `release_manifest.py validate` | manifest 定义与仓库投影一致；`get <field>` 供构建脚本取值；`sync <X.Y.Z> [--schema-version]` 反向同步 manifest、`VERSION`、根 pom、flattened pom、双前端 `package.json`、lock、env 投影 | CI 首个门禁；构建脚本用 `get` 取模块/路径/版本 |
| `release-smoke-test.py` | 打开 bundle：拒绝绝对路径、`..` 与非普通成员；必须含 `release-manifest.json` 且其定义合法、`bundle.manifestPath` 为其自身、`requiredPaths` 全部存在；`VERSION` 与 `RELEASE.version` 必须等于 manifest 的 `version`/`releaseVersion`，`RELEASE.schema_version` 必须等于 manifest 的 schema，`RELEASE.sha` 必须为 40 位十六进制或 `unknown` | 构建端与部署端各执行一次 |
| `db-migrate.py` | checksum ledger：`--dry-run` 不连接即列待执行项；`--apply` 前校验已应用记录的 sha256，不一致直接退出；失败写 `FAILED` 与错误信息；`--retry-failed` 仅人工核验后使用；`--baseline [--baseline-to vX.Y.Z]` 标记已手工执行的历史迁移 | 需显式 `CODE_NEST_RUN_MIGRATIONS=true` 才在发布中被调用 |
| `server-capacity-governance.sh` | 默认只读报告；`--apply` 才删除。保留 4 份发布备份、14 份数据库备份（`*.sql.gz`）、2 份构建 bundle，清理超过 1440 分钟的孤儿 stage，可选 `--include-build-outputs`/`--include-runner-cache`；`/tmp/code-nest-capacity-governance.lock` 互斥 | 由 systemd timer 每周执行 |
| `external-uptime-check.sh` | 对外端点 HTTP 可用性重试判定 | 独立拨测，未在发布链中调用 |
| `sre-alertmanager-e2e.py` | 未读（内容**未确认**） | 作为托管文件下发到 `/opt/code-nest/bin` |

## 跨模块观察

**共性模式**

- "带白名单的架构断言"：`check-architecture.py` 对每类例外都维护显式 allowlist，并额外报"白名单可缩小"，避免豁免永久化。
- "单一事实源 + 多投影校验"：`release/manifest.json` 派生 5 类投影，任何一处漂移都会阻断发布；`check-version-consistency.py` 只是它的别名以保留历史命令。
- "先干跑、再显式开启写操作"：`db-migrate.py --dry-run`、`server-capacity-governance.sh`（默认只读）、`xiaou.sre.*` 全系列开关默认 false、`CODE_NEST_RUN_MIGRATIONS=false` 同一设计取向。
- "双端校验"：同一批规则在构建端与部署端各跑一次（smoke test、`validate_stage`、`validate_archive_entries`），不信任上游产物。
- "参考副本 + 逐字节漂移检测"：受管文件安装到 `/opt/code-nest/ops`（参考）与运行位置（生效），由 `verify-production-baseline.sh` 用 `cmp -s` 比对。

**潜在风险**

1. `validate_stage` 硬编码 `sql/v2.5.3/production_governance.sql`：数据库目录演进（如新增 v2.5.9 目录或重命名）会直接阻断所有发布，属脆弱耦合。
2. 漂移检测存在盲区：`check_managed_config_drift` 只覆盖 15 个受管文件，不含 `monitoring/alertmanager/alertmanager.local.yml`（compose 实际挂载的文件）与 `monitoring/.env` 内容，也不含 `/etc/code-nest/code-nest.env` 的键集合；生产真实 Alertmanager 配置因此完全不在版本控制内。
3. 多环境硬编码：`deploy/nginx` 同时存在 `code-nest-production.conf`（`server_name 36.140.150.167`）与 `code-nest-113.44.190.45.conf`，发布包只携带 production 一份；两份配置的漂移无自动检查。
4. 端口重叠：Grafana 默认 3000（回环）与管理端开发端口 3000（`always.md`）相同，在生产同机调试时会冲突。
5. `code-nest.service` unit 不在仓库：JVM 参数、`EnvironmentFile`、重启策略无法被评审，也无法纳入漂移检测；`RELEASE.md` 仅记录"仍使用 `/opt/code-nest/app/app.jar`"。
6. 备份保留策略存在两处独立实现（`KEEP_RELEASES=4` 与 `KEEP_RELEASE_BACKUPS=4`），未相互校验；磁盘阈值 8 GiB 与容量脚本也无交叉约束。
7. 迁移失败语义严格（`FAILED` 阻断后续发布 + 人工核验后 `--retry-failed`），但数据库侧无破坏性回滚能力，只能依赖迁移前备份。
8. `deploy-frontends.py` 使用密码认证与 `AutoAddPolicy`（不校验主机密钥），且可绕过发布包直接覆盖 `/var/www`；`deploy-production.py` 默认同样不启用 `--strict-host-key-checking`。

**与 `.agent/rules/always.md` 的不一致/需补充之处**

- `always.md` 只要求同步 `VERSION`、根 `pom.xml` 的 `<revision>`、两端 `package.json`；实际门禁范围更大（`pom-xml-flattened`、`package-lock.json`、`release/manifest.json`、env 投影、Docker tag）。建议在 `always.md` 中把 `release/manifest.json` 明确为版本单一事实源。
- `always.md` 未记录 `xiaou-sre` ↔ `xiaou-ai` 的依赖方向规则（sre 不得依赖 ai/ai 必须依赖 sre），该规则仅存在于 `check-architecture.py`。
- `always.md` 记录的"关键依赖"不含 Micrometer；`xiaou-sre` 直接依赖 `micrometer-core`，可观测性属其模块边界的一部分。
- `RELEASE.md` 最新独立小节为 v2.5.6，而 `VERSION`/`<revision>` 已是 2.5.8；v2.5.7、v2.5.8 的发布说明未在该文件出现（是否在 `CHANGELOG.md` 覆盖**未确认**）。
- `always.md` 的"私密配置 `application-sec.yml` 不得写入工件"与发布链一致：`check-architecture.py` 明确排除 `application-sec.yml` 的运行时 YAML 检查，`deploy-release.sh` 只打包 `sql/`、`ops/`、`scripts/` 与构建产物，不含 `resources`。

## 铁律

1. 本文件只记录实际读取过的文件内容；未读或未确认项已显式标注为"未确认"。
2. 任何部署/迁移/容量治理脚本在本规格编写期间均未执行；仓库内除本文件外未被修改。

**未确认清单**：`db-migrate.py` 完整实现细节；`sre-alertmanager-e2e.py` 全部内容；`server-capacity-governance.sh` 的完整清理对象清单；`external-uptime-check.sh` 的判定细节；`code-nest.service` unit 与 `deploy-from-workspace.sh`；`.github/workflows` 中的 CI/生产部署工作流；`deploy/nginx/code-nest-113.44.190.45.conf`；`docker/{ai,go-judge,env}`、`docker/Dockerfile` 细节；`docker/monitoring/README.md`、`validate-config.sh`、`compose.sh` 内部逻辑；`sre/runbooks.json` 的 runbook 条目；28 个 Service 中未抽样的部分与全部 Mapper SQL；`sre_*` 表的列级结构；Grafana 两个 dashboard 的面板明细；`SreWebhookAuthenticationFilter` 与只读工具白名单实现。
