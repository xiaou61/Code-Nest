# 告警 Runbook

这页不是替代 [监控与观测](/operations/monitoring) 或 [事故响应](/operations/incident-response)，而是把“某条告警响了以后第一分钟看什么、第三分钟做什么、什么时候该回滚”写成可执行手册。

使用顺序建议是：

1. 先在 [监控与观测](/operations/monitoring) 确认是哪条告警、从什么时候开始、影响面多大。
2. 如果已经影响用户或服务不可用，立刻切到 [事故响应](/operations/incident-response) 先止损。
3. 再回到这页，按具体告警项执行对应 Runbook。

## 怎么用这页

每条告警都按同一套顺序处理：

1. 先看是不是误报，确认时间窗和趋势。
2. 再看影响面，是单实例、单模块还是全站。
3. 再看第一证据，优先日志、指标、变更记录和依赖状态。
4. 能止损就先止损，不能止损再深入排查。
5. 处理完把结论回写到 [验证记录与已知问题](/manuals/verified-scenarios) 或对应模块文档。

## 告警总表

| 告警 | 严重度 | 第一判断 | 第一证据 |
| --- | --- | --- | --- |
| `ApplicationDown` | critical | 应用是否真的不可达 | `up{job="code-nest"}`、`/api/actuator/health` |
| `HighErrorRate` | critical | 是全站 5xx 还是单模块 5xx | 5xx 曲线、最近发布、异常栈 |
| `SlowRequests` | warning | 是单 URI 变慢还是整体变慢 | P99、慢 URI、连接池、慢 SQL |
| `DatabaseConnectionPoolExhausted` | warning | 是数据库慢还是连接泄漏 | `hikaricp_connections_pending`、慢 SQL、线程堆栈 |
| `HighMemoryUsage` | warning | 是短时尖峰还是持续泄漏 | Heap 使用率、GC、实例重启历史 |
| `HighGCTime` | warning | 是内存抖动还是流量突增 | GC pause、Heap、对象创建高峰 |
| `HighCPUUsage` | warning | 是热点接口、死循环还是外部依赖卡死 | CPU、慢接口、线程状态 |

## `ApplicationDown`

### 现象

- Prometheus `up{job="code-nest"} == 0`
- Grafana 应用面板断崖
- `/api/actuator/health` 无法访问

### 先看什么

1. 是单实例 down，还是整个服务组都 down。
2. 最近 10 分钟是否刚发版、改配置、重启容器、切代理。
3. 前端是全站报错，还是只有 API 不可达。

### 第一证据

- 容器/进程状态
- 应用启动日志
- Nginx 或网关转发日志
- `/api/actuator/health`

### 常见根因

- 应用根本没起来
- 配置错误导致启动失败
- MySQL/Redis 不可达
- 代理转发错到不存在的 upstream
- 宿主机端口或容器网络异常

### 先止损怎么做

1. 如果是刚发版后出现，优先回滚上一版。
2. 如果是单实例问题，先摘流量或重启故障实例。
3. 如果是依赖不可达，先恢复 MySQL/Redis/代理，再决定是否恢复应用。

### 下一页看哪里

- 部署和容器问题： [Docker 与服务部署](/operations/docker)
- 事故流程： [事故响应](/operations/incident-response)
- 本地或预发排查： [常见问题排查](/operations/troubleshooting)

## `HighErrorRate`

### 现象

- 5xx 比例持续升高
- 某些页面大量报“服务异常”
- 日志里异常栈爆发式增长

### 先看什么

1. 影响的是全站、单模块，还是单个 URI。
2. 错误从哪次发布或哪次配置变更后开始。
3. 是业务异常、依赖超时，还是权限/参数错误被错误包装成 5xx。

### 第一证据

- Prometheus 5xx 曲线
- 后端异常栈
- 最近变更记录
- 对应模块的 API / 页面入口

### 常见根因

- 刚上线的代码路径异常
- AI/RAG、go-judge、对象存储等外部依赖超时
- 数据异常导致空指针或状态机分支漏判
- 数据库连接池被打满

### 先止损怎么做

1. 如果只影响新功能，优先关入口或回滚。
2. 如果只影响外部依赖链路，先降级高风险功能，例如 AI、OJ、上传。
3. 如果是单模块集中报错，先隔离该模块入口，不要拖全站。

### 下一页看哪里

- 总流程： [事故响应](/operations/incident-response)
- 具体现象缩小范围： [问题定位流程](/operations/diagnosis-flow)
- 模块常见坑：对应模块页

## `SlowRequests`

### 现象

- P99 持续超过 2 秒
- 用户反馈页面卡顿但不一定报错
- 发布后特定页面明显变慢

### 先看什么

1. 是所有请求都慢，还是某个 URI 慢。
2. 是应用处理慢，还是数据库/外部依赖慢。
3. 是否刚好在大流量、批任务、导入任务或 AI 调用高峰后出现。

### 第一证据

- P95 / P99
- 慢 URI
- Hikari pending
- 慢 SQL 和外部超时日志

### 常见根因

- 慢 SQL 或索引缺失
- 外部 AI / RAG / go-judge 调用超时
- 某个批量接口放大了流量
- JVM GC 抖动

### 先止损怎么做

1. 暂停批量任务、导入任务或高成本 AI 场景。
2. 必要时关闭最慢的高风险入口。
3. 如果是单个慢接口，先把入口流量压下来再排查。

### 下一页看哪里

- 监控指标： [监控与观测](/operations/monitoring)
- 失败和依赖类问题： [问题定位流程](/operations/diagnosis-flow)

## `DatabaseConnectionPoolExhausted`

### 现象

- `hikaricp_connections_pending > 0`
- 接口开始排队，随后可能演变成 5xx
- 日志出现获取连接超时

### 先看什么

1. 是数据库本身慢，还是应用没有及时释放连接。
2. 是单个重查询拖垮连接池，还是整体流量突增。
3. 最近是否上线了新列表、新统计、新批处理。

### 第一证据

- Hikari pending / active / idle
- 慢 SQL
- 线程堆栈
- 最近发布的 DAO / Mapper 变更

### 常见根因

- 慢 SQL、缺索引
- 大分页、统计聚合、循环查库
- 事务范围过大
- 数据库抖动

### 先止损怎么做

1. 暂停高成本任务或后台批处理。
2. 必要时临时下线慢查询入口。
3. 如果数据库异常，优先恢复数据库可用性。

### 下一页看哪里

- 部署和数据库依赖： [Docker 与服务部署](/operations/docker)
- 常见排障： [常见问题排查](/operations/troubleshooting)

## `HighMemoryUsage`

### 现象

- Heap 使用率超过 90%
- 实例频繁 Full GC
- 可能进一步演变成 OOM 或重启

### 先看什么

1. 是持续攀升还是短时尖峰。
2. 重启后是否很快再次爬高。
3. 是否有大对象加载、批量任务、导出、AI 长上下文等场景。

### 第一证据

- Heap 使用率
- GC 次数和耗时
- 实例重启历史
- 发布前后曲线对比

### 常见根因

- 内存泄漏
- 批量数据加载过大
- 缓存膨胀
- 长时间持有大对象

### 先止损怎么做

1. 如果已经逼近 OOM，优先摘流量或重启故障实例。
2. 暂停大批量任务、导出任务或高上下文 AI 场景。
3. 如果是发版后才出现，优先回滚。

### 下一页看哪里

- 事故流程： [事故响应](/operations/incident-response)
- JVM/GC 指标： [监控与观测](/operations/monitoring)

## `HighGCTime`

### 现象

- GC pause 时间占比过高
- 接口时延抖动
- CPU 可能同时升高

### 先看什么

1. 是否和 Heap 飙升同时出现。
2. 是否和大流量、大对象或缓存膨胀同时出现。
3. 是偶发 spike 还是持续恶化。

### 第一证据

- `jvm_gc_pause_seconds_sum`
- Heap 使用率
- 慢请求曲线

### 常见根因

- 对象创建过快
- 内存回收压力过大
- Heap 设置偏小

### 先止损怎么做

1. 先压掉高成本入口。
2. 如果发版后明显恶化，回滚最近版本。
3. 如果实例已明显抖动，先保核心链路。

### 下一页看哪里

- 内存告警：本页 `HighMemoryUsage`
- 总事故流程： [事故响应](/operations/incident-response)

## `HighCPUUsage`

### 现象

- CPU 使用率持续超过 80%
- 接口变慢、线程堆积
- 可能伴随 GC 或 5xx

### 先看什么

1. 是应用 CPU 高，还是宿主机整体打满。
2. 是否和某个热点接口、死循环、批量任务同时出现。
3. 是否和 GC、数据库等待或外部依赖超时交织出现。

### 第一证据

- `process_cpu_usage`
- 热点 URI
- 线程状态
- 发版和任务调度记录

### 常见根因

- 热点接口流量突增
- 死循环或低效计算
- GC 抖动
- 大批量任务

### 先止损怎么做

1. 暂停非核心批任务和高成本入口。
2. 必要时扩容或摘掉异常实例。
3. 如果是新代码引入，优先回滚。

### 下一页看哪里

- 事故处理： [事故响应](/operations/incident-response)
- 慢请求排查：本页 `SlowRequests`

## 补充动作

一条告警处理完以后，至少补下面三件事：

1. 记录根因、止损动作和验证方式。
2. 判断是否需要补新告警、调阈值或加降级开关。
3. 如果这次问题已经能形成复用经验，更新对应模块页、[验证记录与已知问题](/manuals/verified-scenarios) 或 [监控与观测](/operations/monitoring)。


## 相关文档

| 文档 | 说明 |
| --- | --- |
| [监控与观测](/operations/monitoring) | 监控配置 |
| [事故响应](/operations/incident-response) | 事故处理 |
| [问题定位流程](/operations/diagnosis-flow) | 问题排查 |
| [常见问题排查](/operations/troubleshooting) | 常见问题 |
