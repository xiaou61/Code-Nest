# Code Nest Release Notes

## v2.5.1

`v2.5.1` 是 `v2.5.0` 只读 SRE 工作流的生产治理版本。它将指标、告警、Dashboard、容量、发布、回滚、外部探针和合成演练收敛为一套可重复验收的单机生产基线。系统仍不执行自动修复，也不代表已经完全对齐 OpenSRE。

### Release Scope

- 应用和 SRE 指标在零流量时即可发现；Recording Rules 与 15 条生产告警覆盖可用性、JVM、主机、Outbox、评测队列、deadline、租约恢复和 RCA 质量。
- Grafana 自动 provision 应用生产与 SRE Runtime 两个 Dashboard，Prometheus 数据源 UID 固定为 `prometheus`。
- Outbox 证据增加不可变部署快照和服务端固定 Runbook 快照，不读取 Git、不执行 Shell、不查询数据库生成运行证据。
- release bundle 同时管理应用、双前端、Nginx、监控配置、Dashboard、容量 timer、治理脚本和 RELEASE 元数据。
- 部署失败会恢复 JAR、双前端、应用构建溯源、受管运维配置和 systemd 状态；监控 `.env`、Alertmanager 本地配置、targets 与 secrets 始终保留。
- GitHub 托管 Runner 每 5 分钟从服务器外探测用户端 `:81` 和管理端 `:82`，故障期间只维护一个 Issue，恢复后自动关闭。

### Compatibility And Migration

- 本版本没有新数据库 DDL。
- 已完成 `v2.5.0` 全部 SRE 迁移的生产环境可直接升级。
- 尚未完成迁移的环境必须先按 `sql/v2.5.0` 中 incident、investigation、evaluation、suite、queue 和权限脚本的既有顺序执行，并在迁移完成前保持相应 Worker 与数据库 gauge 关闭。
- `XIAOU_SRE_EVALUATION_BUILD_VERSION` 更新为 `2.5.1`；发布脚本会从 bundle 的 `RELEASE` 文件原子更新 source revision、build ID 和 build version。

### Production Prerequisites

1. `code-nest.service`、`nginx.service`、Prometheus、Alertmanager、Node Exporter 和 Blackbox Exporter 处于健康状态。
2. `/opt/code-nest/monitoring/.env` 已配置非默认 Grafana 管理员密码，且所有监控端口只绑定 `127.0.0.1`。
3. `qq_smtp_auth_code` 与 `sre_webhook_token` 非空，owner 为 `65534:65534`，mode 为 `0400`；secret 目录 mode 为 `0711`。
4. 数据库已有发布前压缩备份，并记录 SHA-256。
5. 容量治理执行后 `/opt/code-nest` 所在文件系统至少有 8 GiB 可用空间。不要用全局 Podman prune 代替有界清理。

### Deployment

```bash
# 先查看候选项，再显式执行有界清理
/opt/code-nest/bin/server-capacity-governance.sh
/opt/code-nest/bin/server-capacity-governance.sh \
  --apply --include-build-outputs --include-runner-cache

# GitHub self-hosted Runner 发布前会自动执行不含构建输出的有界清理
# 然后构建 release bundle 并调用 deploy-release.sh deploy
```

部署脚本按以下顺序执行：

1. 校验 tar 路径、必需资产、符号链接和 RELEASE 元数据。
2. 备份当前应用、双前端、应用环境中的构建溯源和全部受管运维文件。
3. 逐文件安装白名单资产，保留所有本地监控状态与 secret。
4. 运行 `nginx -t`、Prometheus/Alertmanager 配置校验和 Compose 配置校验。
5. 刷新 systemd timer，启动监控栈与 Grafana，重启 Prometheus，并重载 Nginx。
6. 重启应用，执行包含版本/SHA、4 个 Prometheus targets、Grafana、secret 权限、8 GiB 容量和配置漂移的生产基线检查。
7. 任一步失败时恢复备份并重新验证原版本。

### Acceptance

```bash
/opt/code-nest/bin/verify-production-baseline.sh \
  --require-grafana \
  --expected-version v2.5.1 \
  --expected-sha <full-release-sha> \
  --min-free-gb 8
```

合成演练会真实发送 QQ firing/resolved 通知，必须准备 mode `0600` 的管理员 token 文件并显式确认：

```bash
/opt/code-nest/bin/sre-alertmanager-e2e.py \
  --confirm-notification \
  --admin-token-file /etc/code-nest/sre-e2e-admin-token
```

演练验收 Alertmanager 指纹、事故、`ALERT_SNAPSHOT`、`DEPLOYMENT_SNAPSHOT`、`RUNBOOK_SNAPSHOT`、两次重复投递幂等、resolved 状态和 active-alert 清理。脚本不接受任意告警内容或查询语句。

### Rollback

```bash
sudo /opt/code-nest/bin/deploy-release.sh rollback \
  /opt/code-nest/backups/releases/<timestamp>-v2.5.1
```

回滚后重新执行生产基线，并确认 `code-nest.service`、Nginx、Prometheus targets 和原版本页面正常。数据库无本版迁移，因此应用回滚不需要反向 DDL。

### Release Gate

- `scripts/code-nest-eval.ps1 -Tier sre`
- 后端完整测试与 `xiaou-application` 打包
- 双前端 contract tests 与生产构建
- 文档 audit 与 VitePress 构建
- shell syntax、容量、发布/回滚、基线、外部 uptime 契约
- Alertmanager 合成演练本地 HTTP 模拟
- Prometheus rules、Grafana JSON、Compose/YAML 和 systemd unit 校验
- 生产容量治理、部署、基线、外部探针与显式 QQ 合成演练

正式 tag 只在发布分支合并、全部门禁和生产验收完成后创建。
