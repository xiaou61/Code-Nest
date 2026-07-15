# 部署与运维导航

运维文档按“部署配置、日常观测、故障响应、验证记录”组织。线上问题先止损，再定位；不要从长篇模块文档开始搜索。

## 部署配置

| 任务 | 文档 |
| --- | --- |
| 使用 Docker 启动依赖和应用 | [Docker 与服务部署](/operations/docker) |
| 配置数据库、Redis、AI、RAG 和安全密钥 | [环境变量总表](/operations/env-vars) |
| 独立部署前后端和文档站 | [独立部署](/guide/deploy) |
| 使用 CI/CD 发布生产版本 | [生产 CI/CD 自动发布](/guide/ci-cd-production) |

## 观测与故障响应

| 场景 | 第一入口 | 补充材料 |
| --- | --- | --- |
| 指标异常或服务抖动 | [监控与观测](/operations/monitoring) | [告警 Runbook](/operations/alert-runbooks) |
| 线上事故 | [事故响应](/operations/incident-response) | [问题定位流程](/operations/diagnosis-flow) |
| 本地或线上功能异常 | [常见问题排查](/operations/troubleshooting) | 对应模块页 |
| 发布前确认 | [发布前验证](/guide/release-verification) | [模块最小回归矩阵](/reference/module-regression-matrix) |

## 响应原则

1. 先确认影响范围和用户可见症状。
2. 记录时间线、版本、配置和关键日志。
3. 优先止损，避免在事故中直接做大范围重构。
4. 修复后补回归项、Runbook 和已知问题记录。

## 验证记录

- [线上接口业务正确性测试（2026-06-18）](/operations/online-api-business-correctness-2026-06-18)
- [验证记录与已知问题](/manuals/verified-scenarios)
