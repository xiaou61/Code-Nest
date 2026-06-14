# 监控与观测

Code Nest 已经具备 Prometheus + Grafana 的基础监控配置，并在 AI Runtime 中补充了运行指标和治理看板。监控体系可以分成两层：基础设施/应用指标负责发现“系统有没有问题”，AI Runtime 指标负责发现“模型调用和结构化输出有没有问题”。

如果你已经看到告警、用户大面积失败或服务不可用，先看 [事故响应](/operations/incident-response)。本页更适合确认“问题有没有发生、发生在哪、趋势是不是在恶化”。

如果你已经知道是哪条告警响了，但不想临场再想“第一分钟该做什么”，直接去看 [告警 Runbook](/operations/alert-runbooks)。

## 资料入口

| 路径 | 说明 |
| --- | --- |
| `AI-DOCS/Deployment/监控告警/Prometheus监控部署指南.md` | Prometheus 原始部署文档 |
| `docker/monitoring/docker-compose.yml` | Prometheus + Grafana 编排 |
| `docker/monitoring/prometheus.yml` | Prometheus scrape 配置 |
| `docker/monitoring/alert_rules.yml` | 告警规则示例 |
| `xiaou-application/src/main/resources/application.yml` | Actuator/Micrometer 配置 |
| `xiaou-ai/src/main/java/com/xiaou/ai/metrics` | AI Runtime 指标聚合 |
| `/system/ai-governance` | AI 质量治理中心 |
| `/system/ai-config` | AI 配置、回归、指标和 RAG 调试 |

## 快速启动监控组件

```bash
cd docker/monitoring
docker compose up -d
```

默认地址：

| 服务 | 地址 | 默认账号 |
| --- | --- | --- |
| Prometheus | `http://localhost:9090` | 无 |
| Grafana | `http://localhost:3000` | `admin / admin123` |

启动后打开 Prometheus Targets：

```text
http://localhost:9090/targets
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
| 默认 target | `host.docker.internal:9999` |
| 外部标签 | `monitor=code-nest-monitor`、`env=dev` |

如果在 Linux 上运行 Docker，`host.docker.internal` 可能不可用，需要改成宿主机 IP 或 Docker bridge 地址。

## Grafana 配置

第一次登录 Grafana 后：

1. 进入 Data Sources。
2. 添加 Prometheus。
3. URL 填 `http://prometheus:9090`。
4. Save & Test。

推荐导入 Dashboard：

| ID | 名称 | 用途 |
| --- | --- | --- |
| `11378` | Spring Boot 2.1 Statistics | Spring Boot 综合监控 |
| `4701` | JVM (Micrometer) | JVM 内存、GC、线程 |
| `6756` | Spring Boot Statistics | 轻量 Spring Boot 监控 |

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

基础指标由 Actuator/Micrometer 暴露；AI 指标由 AI Runtime 在管理端展示，目前不等同于 Prometheus 指标，需要通过 `/admin/ai/config/metrics` 或治理页面查看。

## 告警规则

`docker/monitoring/alert_rules.yml` 已准备这些规则：

| 告警 | 条件 | 严重度 |
| --- | --- | --- |
| `HighMemoryUsage` | JVM heap 使用率 > 90% 持续 5 分钟 | warning |
| `HighErrorRate` | 5xx 速率 > 0.05 持续 5 分钟 | critical |
| `DatabaseConnectionPoolExhausted` | `hikaricp_connections_pending > 0` 持续 2 分钟 | warning |
| `ApplicationDown` | `up{job="code-nest"} == 0` 持续 1 分钟 | critical |
| `HighGCTime` | GC pause sum 5 分钟速率 > 0.1 | warning |
| `HighCPUUsage` | `process_cpu_usage > 0.8` 持续 5 分钟 | warning |
| `SlowRequests` | HTTP P99 > 2 秒持续 5 分钟 | warning |

当前 `prometheus.yml` 里 `rule_files` 是注释状态。要启用规则：

```yaml
rule_files:
  - 'alert_rules.yml'
```

如果要发邮件、企业微信、飞书或 Webhook，还需要部署并配置 Alertmanager。

具体到每条告警响了以后先看什么、先止损什么，见 [告警 Runbook](/operations/alert-runbooks)。

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
