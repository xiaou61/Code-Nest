# AI 联调 Docker Compose

该目录提供一套可直接联调的容器编排，统一拉起：

- `mysql`
- `redis`
- `llamaindex-service`
- `code-nest`

## 使用方式

1. 复制环境变量模板：

```bash
cp .env.example .env
```

2. 在 `.env` 中填写真实的 AI 中转站配置：

- `XIAOU_AI_BASE_URL`
- `XIAOU_AI_API_KEY`
- `XIAOU_AI_CHAT_MODEL`
- `XIAOU_AI_RAG_API_KEY`

3. 在仓库根目录执行：

```bash
docker compose -f docker/ai/docker-compose.yml --env-file docker/ai/.env up -d --build
```

4. 查看服务状态：

```bash
docker compose -f docker/ai/docker-compose.yml --env-file docker/ai/.env ps
docker compose -f docker/ai/docker-compose.yml --env-file docker/ai/.env logs -f code-nest
```

## 说明

- `mysql` 首次启动时会自动导入 `sql/MySql/code_nest.sql` 与 `sql/MySql/code_nest_data.sql`。
- `code-nest` 使用 `docker` profile，会自动接入容器内的 MySQL、Redis 和 `llamaindex-service`。
- `llamaindex-service` 的知识文件持久化到 `rag_data` volume；如果只想导入样例知识，可在后台 AI 配置页里执行“导入样例知识”。

## 常用地址

- Java 后端：`http://127.0.0.1:9999/api`
- Swagger：`http://127.0.0.1:9999/api/swagger-ui.html`
- RAG Health：`http://127.0.0.1:18080/health`

## 停止与清理

```bash
docker compose -f docker/ai/docker-compose.yml --env-file docker/ai/.env down
docker compose -f docker/ai/docker-compose.yml --env-file docker/ai/.env down -v
```
