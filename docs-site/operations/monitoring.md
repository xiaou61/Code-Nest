# 监控与观测

Code Nest 已经具备 Prometheus + Grafana 方向的监控基础，并在 AI Runtime 中补充了运行指标和治理看板。

## 监控入口

| 路径 | 说明 |
| --- | --- |
| `docs/Prometheus监控部署指南.md` | Prometheus 监控部署原始文档 |
| `docker/monitoring/` | 监控组件部署资料 |
| `xiaou-application/src/main/resources/application.yml` | Actuator/Micrometer 配置 |
| `xiaou-ai` | AI Runtime 指标采集 |
| `/system/ai-governance` | 管理端 AI 质量与治理中心 |

## 关注指标

- API 请求耗时和错误率。
- 数据库慢查询。
- Redis 连接和限流命中。
- WebSocket 在线人数和消息失败。
- OJ 判题耗时、错误类型和队列积压。
- AI 调用次数、耗时、失败率、场景质量分。
- 文件上传量和存储迁移状态。

## 后续补齐

- Grafana 仪表盘截图。
- Prometheus scrape 配置。
- 告警规则。
- 线上问题定位 SOP。
- AI Runtime 质量指标口径。

