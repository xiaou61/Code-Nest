# 监控与观测

Code Nest 的生产监控基线由 Prometheus、Alertmanager、Grafana、Node Exporter 和 Blackbox Exporter 组成。基础设施/应用指标负责发现“系统有没有问题”，SRE Runtime 指标负责发现“告警、证据、调查和持久化队列是否停滞”，AI Runtime 页面继续负责模型调用和结构化输出质量。

如果你已经看到告警、用户大面积失败或服务不可用，先看 [事故响应](/operations/incident-response)。本页更适合确认“问题有没有发生、发生在哪、趋势是不是在恶化”。

如果你已经知道是哪条告警响了，但不想临场再想“第一分钟该做什么”，直接去看 [告警 Runbook](/operations/alert-runbooks)。

## 资料入口

| 路径 | 说明 |
| --- | --- |
| `AI-DOCS/Deployment/监控告警/Prometheus监控部署指南.md` | Prometheus 原始部署文档 |
| `docker/monitoring/docker-compose.yml` | Prometheus + Grafana 编排 |
| `docker/monitoring/prometheus.yml` | Prometheus scrape 配置 |
| `docker/monitoring/alert_rules.yml` | 告警规则示例 |
| `docker/monitoring/grafana/dashboards` | 自动 provision 的应用与 SRE Dashboard |
| `scripts/verify-production-baseline.sh` | 生产健康、边界、权限、容量与配置漂移检查 |
| `scripts/sre-alertmanager-e2e.py` | 固定合成告警的 firing/幂等/evidence/resolved 演练 |
| `xiaou-bootstrap/src/main/resources/application.yml` | Actuator/Micrometer 配置 |
| `xiaou-ai/src/main/java/com/xiaou/ai/metrics` | AI Runtime 指标聚合 |
| `/system/ai-governance` | AI 质量治理中心 |
| `/system/ai-config` | AI 配置、回归、指标和 RAG 调试 |

## 快速启动监控组件

```bash
cd docker/monitoring
cp .env.example .env
# 准备本地 targets、Alertmanager 配置和 mode 0400 的 secrets 后：
./scripts/validate-config.sh
./scripts/compose.sh --env-file .env -f docker-compose.yml up -d
```

默认地址：

| 服务 | 地址 | 默认账号 |
| --- | --- | --- |
| Prometheus | `http://127.0.0.1:19090` | 无，仅 loopback |
| Alertmanager | `http://127.0.0.1:19093` | 无，仅 loopback |
| Grafana | `http://127.0.0.1:3000` | 从 `.env` 注入，不提供仓库默认密码 |

启动后打开 Prometheus Targets：

```text
http://127.0.0.1:19090/targets
```

确认 `code-nest` 任务状态为 `UP`。

## 应用指标端点

后端启用了 Spring Boot Actuator 和 Micrometer Prometheus。

| 端点 | 用途 |
| --- | --- |
| `/api/actuator/health` | 应用健康检查 |
| `/api/actuator/metrics` | 指标列表 |
| `/api/actuator/prometheus` | Prometheus scrape 指标 |

`application.yml` 中已暴露：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

Prometheus 当前配置：

| 配置 | 值 |
| --- | --- |
| 全局采集间隔 | `15s` |
| `code-nest` 采集间隔 | `10s` |
| 指标路径 | `/api/actuator/prometheus` |
| 生产 target | `127.0.0.1:9999`，由 `targets/code-nest.local.yml` 提供 |
| 外部标签 | `monitor=code-nest-monitor`、`environment=production` |

生产 Compose 使用 Linux host network，使 Prometheus 能访问只绑定 loopback 的 Java 进程。所有监控 UI 和 exporter 仍必须绑定 `127.0.0.1`，通过 SSH tunnel 访问，不在公网安全组开放。

## Grafana 配置

Grafana 启动时自动 provision 数据源和 Dashboard，无需在 UI 手工导入：

| UID | 名称 | 用途 |
| --- | --- | --- |
| `code-nest-application` | Code Nest Application Production | HTTP、JVM、主机和可用性，8 个 panels |
| `code-nest-sre` | Code Nest SRE Runtime | Outbox/评测积压、运行耗时、调查质量和队列恢复，12 个 panels |

数据源 UID 固定为 `prometheus`，URL 指向 host network 下的 `127.0.0.1:19090`。Dashboard 文件由发布包管理，线上手工修改会被生产基线判定为漂移。

## 重点指标

| 领域 | 指标 |
| --- | --- |
| JVM | 堆内存、非堆内存、GC 次数和耗时、线程数 |
| HTTP | 请求量、状态码、P95/P99、慢接口 |
| 数据库 | Hikari 连接池、等待连接数、活跃连接 |
| Redis | 业务上关注连接、限流命中、缓存命中和关键 key |
| WebSocket | 在线人数、连接失败、心跳超时、消息失败 |
| OJ | 判题耗时、判题状态、go-judge 可用性、队列积压 |
| 文件 | 上传成功率、文件大小、迁移任务状态 |
| AI | 调用次数、成功率、失败率、兜底率、结构化解析失败率、Token、成本、平均耗时 |

SRE Outbox 也通过同一个 Prometheus 端点暴露运行指标。它们不包含事故 ID、用户 ID
或原始 payload，适合直接配置阈值告警：

| 指标 | 类型 | 说明 |
| --- | --- | --- |
| `xiaou_sre_outbox_pending` | Gauge | 当前到期且等待处理的队列积压量 |
| `xiaou_sre_outbox_processing` | Gauge | 当前进程正在处理的事件数 |
| `xiaou_sre_outbox_lease_recoveries_total` | Counter | 租约过期后重新放回队列的事件数 |
| `xiaou_sre_outbox_events_total{event_type,outcome}` | Counter | 事件处理结果，结果为 `success`、`retry`、`failed` 等 |
| `xiaou_sre_outbox_event_duration_seconds{event_type,outcome}` | Timer | 单个事件处理耗时，可用于 P95/P99 |
| `xiaou_sre_outbox_worker_runs_total{outcome}` | Counter | Worker 扫描轮次，区分 `success` 与 `error` |
| `xiaou_sre_outbox_worker_duration_seconds{outcome}` | Timer | Worker 单轮扫描和派发耗时 |

告警入口和事故状态也有低基数指标，便于区分“没有告警”和“告警接收链路坏了”：

| 指标 | 类型 | 说明 |
| --- | --- | --- |
| `xiaou_sre_alerts_ingested_total{status,severity}` | Counter | Alertmanager 告警接收量 |
| `xiaou_sre_alerts_duplicates_total` | Counter | 被 incident key 去重的告警量 |
| `xiaou_sre_alerts_ingestion_errors_total` | Counter | 请求校验或持久化异常量 |
| `xiaou_sre_incidents_open` | Gauge | 当前仍未关闭的事故数 |
| `xiaou_sre_evidence_collections_total{source,outcome}` | Counter | 证据采集结果 |
| `xiaou_sre_evidence_collection_duration_seconds{source,outcome}` | Timer | 证据采集耗时，可用于 P95/P99 |
| `xiaou_sre_evidence_collection_errors_total{source}` | Counter | 证据采集失败量 |

建议至少为 `xiaou_sre_outbox_pending` 持续增长、`outcome="failed"` 增长和事件处理
P95 超过租约时间配置设置告警；Worker 默认关闭，启用前仍需先完成数据库迁移。

基础指标由 Actuator/Micrometer 暴露；AI 指标由 AI Runtime 在管理端展示，目前不等同于 Prometheus 指标，需要通过 `/admin/ai/config/metrics` 或治理页面查看。

## 告警规则

`docker/monitoring/alert_rules.yml` 已启用 Recording Rules 和这些生产告警：

| 告警 | 条件 | 严重度 |
| --- | --- | --- |
| `CodeNestTargetDown` / `CodeNestPublicEndpointDown` | metrics target 或 Blackbox 公网页面持续失败 | critical |
| `CodeNestHighHttpErrorRatio` / `CodeNestHighHttpLatencyP95` | 有流量时 5xx 比例或 P95 延迟超过门槛 | critical / warning |
| `CodeNestHighJvmHeapUsage` / `CodeNestHostCpuHigh` | JVM heap 或主机 CPU 持续高位 | warning |
| `CodeNestHostDiskLow` / `CodeNestHostDiskCritical` | 根文件系统可用比例低于 15% / 5% | warning / critical |
| `CodeNestSreOutboxStalled` / `CodeNestSreEvaluationQueueStalled` | 队列存在积压且最老任务超过 5 分钟 | critical / warning |
| `CodeNestSreQueueTerminalFailure` / `CodeNestSreQueueDeadlineExceeded` | 15 分钟内出现受控终态失败或 deadline | critical / warning |
| `CodeNestSreLeaseRecoverySpike` | 15 分钟租约恢复次数异常 | warning |
| `CodeNestSreInvestigationFailureRatioHigh` / `CodeNestSreInvestigationDurationHigh` | RCA 失败率或 P95 耗时持续超限 | warning |

`prometheus.yml` 已加载 `/etc/prometheus/rules/*.yml`。`validate-config.sh` 使用固定 Prometheus/Alertmanager 镜像执行 `promtool`、`amtool` 和 Compose 校验，校验失败时不得 reload。

具体到每条告警响了以后先看什么、先止损什么，见 [告警 Runbook](/operations/alert-runbooks)。

## SRE 事故工作台

管理端 `/sre/incidents` 将 Alertmanager 告警聚合为事故，并提供事故状态、时间线、固定白名单
证据和只读 RCA。打开事故时会恢复最近一次 RCA，也可以切换历史运行并查看上下文加载、调查
计划、最多 5 轮固定只读工具、证据重载、模型分析和报告校验轨迹。模型先从后端给出的固定
tool key 中生成一次计划；后端再次做白名单、去重和 5 轮截断，重复证据会提前停止。每轮新增
证据都先持久化并获得 evidence ID，最后只调用一次 RCA 报告模型。每次成功构建最终模型上下文
后，还会固化同一份脱敏
输入及其 SHA-256、Prompt/Schema 版本、配置模型、实际模型和调用结果；页面只展示来源摘要，
不回显上下文正文。管理员可对每次运行评价准确度、标记主要缺口并填写期望结论；每次修改
追加审计修订，可下载不含原始输入、备注和管理员身份的评测样本。

评测工作台的运行请求只负责入队并返回 HTTP `202`，模型不会占用请求线程。管理端每 2 秒
轮询一次运行详情，展示 `QUEUED / RUNNING` 进度以及最终的
`SUCCEEDED / DEGRADED / FAILED`。Worker 使用数据库原子领取、用例级心跳、租约恢复、指数
退避、最大尝试次数和绝对 deadline；重试会复用已有结果并跳过已完成用例。入队时还会冻结
有序成员、Prompt/Schema、源码修订、构建 ID 和构建版本，执行环境不一致时以固定失败码终止。
同一管理员最多有一个活动运行。

后端边界分为两部分：`xiaou-sre` 负责告警、事故、证据与调查记录，`xiaou-system` 复用统一
AI Runtime 生成并校验结构化报告。AI 不进入 QQ 邮件告警热路径，也不能执行 Shell、Docker、
任意 PromQL/LogQL 或修复动作。当前没有自动修复或写操作，也不设置金额、Token、模型成本
预算闸门；运行边界使用固定工具白名单、最多 5 轮、各客户端超时、响应/上下文大小限制和
查询指纹幂等保证。

数据库准备顺序：

1. 新环境直接使用包含 16 张 SRE 表的 `sql/MySql/code_nest.sql`。
2. 已有环境先执行 `sql/v2.5.0/sre_incident.sql`，已有四张早期 SRE 表时可改用
   `sre_incident_evidence.sql`。
3. 部署带 RCA 历史、反馈和回放来源的后端前，再执行
   `sql/v2.5.0/sre_investigation_run.sql`；已执行过早期版本的环境需再次执行该幂等脚本，
   以创建反馈表和 `sre_investigation_artifact`。
4. 从早期 v2.5.0 表结构升级时，停止 RCA 流量并执行一次
   `sql/v2.5.0/sre_investigation_loop.sql`，为证据表增加调查 run 和查询指纹；新环境使用主
   schema 时不执行该增量脚本。
5. 保持 `XIAOU_SRE_EVALUATION_ENABLED=false`，依次执行
   `sql/v2.5.0/sre_rca_evaluation.sql`、`sre_rca_evaluation_suite.sql` 和
   `sre_rca_evaluation_queue.sql`。后两个脚本包含不可重复的 `ALTER TABLE`，只能按顺序各执行
   一次；queue 脚本会把无法建立可信租约的旧 `RUNNING` 记录标记为失败。
6. 所有 SRE 表完成后可开启 `XIAOU_SRE_METRICS_ENABLED=true`；开启前 gauge 刷新不查询数据库。
7. 配置 `XIAOU_SRE_EVALUATION_SOURCE_REVISION`、`XIAOU_SRE_EVALUATION_BUILD_ID` 和
   `XIAOU_SRE_EVALUATION_BUILD_VERSION`，确认迁移成功后再开启评测 Worker。未开启时创建运行
   返回业务码 `503`，不会遗留无人消费的任务。

完整监控栈和私网 Webhook 配置见 `docker/monitoring/README.md`。

SRE 自身指标覆盖开放事故、Outbox/RCA 评测积压与最老任务年龄、活动调查数、告警接收和证据
采集耗时、调查总耗时与轮数、固定只读工具调用耗时，以及队列的入队、领取、重试、租约恢复、
deadline 和终态失败。数据库 gauge 由定时快照更新，Prometheus scrape 线程不会直接访问数据库。

## v2.5.1 生产治理与验收

发布包只安装仓库定义的白名单资产：Nginx 主配置、Prometheus 主配置与规则、Grafana provisioning/Dashboard、两个 systemd unit 和三个治理工具。`/opt/code-nest/monitoring/.env`、`alertmanager.local.yml`、`targets/*.local.yml` 与 `secrets/` 不进入 bundle，也不会在部署或回滚时被覆盖。

容量治理默认只报告：

```bash
/opt/code-nest/bin/server-capacity-governance.sh
/opt/code-nest/bin/server-capacity-governance.sh --apply
/opt/code-nest/bin/server-capacity-governance.sh \
  --apply --include-build-outputs --include-runner-cache
```

只有显式增加 `--include-build-outputs` 才会选择前端 `node_modules/dist`、文档构建输出和 Maven `target`；`--include-runner-cache` 额外治理未被当前 `bin`/`externals` 链接引用的旧 Runner 版本、更新缓存和安装包。检测到 Runner 构建进程时会停止这些显式清理。默认保留 4 份 release backup、14 份数据库备份、2 个构建 bundle，并清理超过 24 小时的孤儿 `release-stage.*`。不要对共享 Podman 存储执行全局 prune。

生产基线命令：

```bash
/opt/code-nest/bin/verify-production-baseline.sh \
  --require-grafana \
  --expected-version v2.5.1 \
  --expected-sha <release-sha> \
  --min-free-gb 8
```

它检查应用/Nginx service、本地 health、用户端和管理端 HTTP 200、公网 Actuator/internal SRE 404、至少 4 个 Prometheus targets 全绿、Grafana health、RELEASE 版本与 SHA、监控 secret owner/mode、磁盘门槛，以及 `/opt/code-nest/ops` 与活动配置的逐文件一致性。

GitHub `External Uptime` workflow 每 5 分钟从 GitHub-hosted Runner 对 `:81` 和 `:82` 各重试 3 次。故障时创建或更新带固定 marker 的单个 Issue，恢复时留言并关闭；不需要新增 GitHub secret。

合成演练会真实触发 QQ firing/resolved 邮件，只能在明确通知相关人员后执行：

```bash
/opt/code-nest/bin/sre-alertmanager-e2e.py \
  --confirm-notification \
  --admin-token-file /etc/code-nest/sre-e2e-admin-token
```

管理员 token 文件必须仅 owner 可读。演练告警名称、severity、annotations 和查询路径均固定，验证两次重复投递不会新增事件或证据，最后等待事故 `RESOLVED` 且 Alertmanager active alert 清理；脚本不提供任意告警或查询参数。

## 常用 PromQL

HTTP 请求速率：

```text
sum(rate(http_server_requests_seconds_count{application="Code-Nest"}[5m]))
```

5xx 错误速率：

```text
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
```

P99 响应时间：

```text
histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m]))
```

JVM 堆内存使用率：

```text
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}
```

数据库连接等待：

```text
hikaricp_connections_pending
```

应用是否可采集：

```text
up{job="code-nest"}
```

## AI Runtime 观测

AI Runtime 指标在管理端看，主要入口：

| 页面 | 说明 |
| --- | --- |
| `/system/ai-config` | 运行配置、Prompt 调试、RAG 调试、回归、metrics |
| `/system/ai-governance` | Prompt/Schema/RAG 覆盖率、质量分、风险队列、运行洞察 |

AI 指标口径：

| 指标 | 说明 |
| --- | --- |
| `totalInvocations` | 总调用次数 |
| `successRate` | 成功率 |
| `errorRate` | 失败率 |
| `fallbackRate` | 兜底率 |
| `structuredParseFailureRate` | 结构化解析失败率 |
| `averageLatencyMs` | 平均耗时 |
| `observedScenes` | 有运行样本的场景数 |
| `recentCallCount` | 最近调用数量，最多保留 50 条 |

AI 指标排查顺序：

1. 先看模型配置是否可用。
2. 再看 Prompt 是否完整。
3. 再看 Schema 覆盖率。
4. 如果 RAG 开启，看 RAG sidecar 健康和召回结果。
5. 最后看最近调用里的失败、兜底和解析失败。

## 日常巡检清单

| 频率 | 检查项 |
| --- | --- |
| 每天 | 应用是否 UP、5xx 是否异常、P99 是否变慢 |
| 每天 | MySQL 连接池是否有 pending |
| 每天 | Redis 是否正常，关键缓存是否异常膨胀 |
| 每天 | AI 失败率、兜底率、解析失败率 |
| 每周 | OJ 判题耗时和 system_error 比例 |
| 每周 | 文件上传量、上传失败和迁移任务 |
| 每周 | 慢接口、慢 SQL 和高频错误日志 |
| 每次发版后 | 版本发布后一小时内重点观察 HTTP、JVM、DB、AI |

## 线上问题定位 SOP

接口变慢：

1. 看 Grafana HTTP P99。
2. 找到慢 URI。
3. 看 Hikari pending 和数据库连接。
4. 看应用日志是否有慢 SQL 或外部调用超时。
5. 如果是 AI 接口，看 AI Runtime 平均耗时、RAG 耗时和 fallback。

接口 5xx 增加：

1. 看 Prometheus 5xx 曲线从什么时候开始。
2. 对照发版时间和配置变更。
3. 查后端日志异常栈。
4. 如果只影响某模块，进入对应模块文档查核心表和常见坑。
5. 临时降级高风险外部依赖，例如 AI/RAG 或 OJ。

AI 质量变差：

1. 打开 `/system/ai-governance`。
2. 看风险队列是 Prompt、Schema、RAG、Error Rate、Fallback 还是 Latency。
3. 在 `/system/ai-config` 执行对应场景回归。
4. 检查最近调用的模型名和耗时。
5. 回滚 Prompt 或补兜底样例。

WebSocket 在线数异常：

1. 查 Redis `chat:online:users`、`chat:online:heartbeat`、`chat:room:{roomId}:users`。
2. 看聊天定时任务是否每 30 秒清理超时用户。
3. 查 Nginx/WebSocket Upgrade 配置。
4. 多实例部署时确认是否已经做跨实例广播。

OJ 判题异常：

1. 访问 go-judge `5050`。
2. 进入容器检查编译器版本。
3. 查后端 OJ 配置 `oj.judge.goJudgeUrl`。
4. 查提交状态是否集中为 `system_error`。

## 常见故障

| 问题 | 原因 | 处理 |
| --- | --- | --- |
| Prometheus target down | 后端未启动、路径错、容器访问不到宿主机 | 改 target，确认 `/api/actuator/prometheus` |
| Grafana 无数据 | 数据源 URL 错或 Prometheus 未采集 | 数据源填 `http://prometheus:9090` |
| 告警规则不生效 | `rule_files` 未启用 | 取消注释并 reload Prometheus |
| Linux 下采集失败 | `host.docker.internal` 不可用 | 改成宿主机 IP 或 bridge 地址 |
| AI 治理页没有运行样本 | 没有触发真实 AI 场景或 metrics 被清空 | 手动触发场景后刷新 |
| AI 指标重启丢失 | Redis 持久化未开启或连接失败 | 检查 `xiaou.ai.metrics.persistence.*` |
| P99 查询为空 | 指标名或标签不匹配 | 在 Prometheus Graph 里先搜索 `http_server_requests` |

## 文档维护提醒

新增一个高风险模块时，至少补三类观测：

1. 业务成功率或失败率。
2. 耗时或队列积压。
3. 能定位问题的日志字段或管理端排查入口。

如果模块调用外部服务，例如 AI、RAG、go-judge、对象存储，还要补“外部服务不可用时怎么降级”。


## 相关文档

| 文档 | 说明 |
| --- | --- |
| [Docker 与服务部署](/operations/docker) | Docker 部署 |
| [告警 Runbook](/operations/alert-runbooks) | 告警处理 |
| [事故响应](/operations/incident-response) | 事故处理 |
| [问题定位流程](/operations/diagnosis-flow) | 问题排查 |
