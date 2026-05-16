# Docker 与服务部署

Code Nest 的部署资料分散在 `docker/`、根 README、各服务 README 和脚本目录中。文档站先建立统一入口，后续逐步补齐每个部署模式的完整步骤。

## 目录入口

| 路径 | 说明 |
| --- | --- |
| `docker/Dockerfile` | 主后端镜像示例 |
| `docker/env/example.env` | 环境变量示例 |
| `docker/ai/docker-compose.yml` | AI 相关服务编排 |
| `docker/ai/README.md` | AI sidecar 部署说明 |
| `docker/go-judge/README.md` | OJ 判题沙箱说明 |
| `docker/monitoring/README.md` | 监控组件说明 |

## 推荐拆分

生产环境建议至少拆分：

- MySQL。
- Redis。
- Spring Boot API。
- 管理端静态资源。
- 用户端静态资源。
- 文档站静态资源。
- LlamaIndex sidecar。
- go-judge 判题服务。
- Prometheus/Grafana/Alertmanager。

## 文档站部署

文档站只需要构建静态文件：

```bash
cd docs-site
npm ci
npm run build
```

然后部署 `docs-site/.vitepress/dist`。

