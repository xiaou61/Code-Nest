# 常见问题排查

本页记录本地开发和部署时优先检查的方向。

## 后端无法启动

优先检查：

1. Java 版本是否为 17+。
2. MySQL 和 Redis 是否启动。
3. `application-dev.yml`、`application-prod.yml`、`application-sec.yml` 是否完整。
4. 数据库脚本是否导入。
5. Maven 是否能正常解析多模块依赖。

## 前端接口 401 或 701/702

优先检查：

1. 管理端和用户端是否使用了对应 Token。
2. 请求路径是否进入正确登录域。
3. 后端 CORS 白名单是否包含当前前端地址。
4. Token 是否过期或被覆盖。

## WebSocket 连接失败

优先检查：

1. 是否先获取一次性握手票据。
2. 票据是否过期或重复使用。
3. WebSocket 地址是否被代理正确转发。
4. 后端 CORS 和 WebSocket origin 配置是否匹配。

## AI 调用失败

优先检查：

1. OpenAI Compatible API 地址和 Key。
2. 模型名称是否正确。
3. `llamaindex-service` 是否启动。
4. RAG 文档是否已导入。
5. 管理端 `/system/ai-config` 连通性测试结果。

## OJ 判题失败

优先检查：

1. go-judge 服务是否可访问。
2. 测试用例是否完整。
3. 语言配置是否支持。
4. 时间和内存限制是否过低。
5. 提交记录中的编译错误、运行错误或超时信息。

## 文档站构建失败

优先检查：

1. 当前目录是否为 `docs-site`。
2. Node.js 是否为 18+。
3. 是否执行过 `npm install` 或 `npm ci`。
4. Markdown 链接是否指向存在的页面。
5. 子路径部署时 `VITEPRESS_BASE` 是否正确。

