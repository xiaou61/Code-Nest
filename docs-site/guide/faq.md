# 常见问题（FAQ）

汇总开发、部署和使用过程中的高频问题。遇到问题时建议先查阅本文档。

## 环境搭建

### Q: 后端启动报 `RedisConnectionException`？

**原因**：Redis 未启动或配置错误。

**解决**：
1. 确认 Redis 已启动：`redis-cli ping` 应返回 `PONG`
2. 检查 `application-dev.yml` 中的 Redis 配置：
   - `host`、`port`、`database`、`password`
3. Sa-Token 使用独立 Redis 连接池（`sa-token.alone-redis`），需单独配置

### Q: 前端启动报 `ECONNREFUSED`？

**原因**：后端未启动或端口不匹配。

**解决**：
1. 确认后端已启动，默认端口 `9999`
2. 检查前端 `vite.config.js` 中的 `proxy` 配置
3. 确认 `context-path` 是否为 `/api`

### Q: 数据库连接失败？

**原因**：MySQL 未启动或配置错误。

**解决**：
1. 确认 MySQL 已启动，版本 8.0+
2. 检查 `application-dev.yml` 中的数据源配置
3. 确认数据库 `code_nest` 已创建
4. 执行 `sql/` 目录下的初始化脚本

### Q: go-judge 沙箱无法连接？

**原因**：go-judge Docker 容器未启动。

**解决**：
1. 执行 `docker-compose up -d go-judge`
2. 确认容器运行状态：`docker ps | grep go-judge`
3. 检查端口 `5050` 是否被占用

## 鉴权相关

### Q: 调用接口返回 `701` 或 `702`？

**原因**：Token 无效或已过期。

**解决**：
1. 检查请求头是否携带 `Authorization: Bearer <token>`
2. 确认 Token 未过期（默认 2 小时）
3. 管理端和用户端使用不同的 Token，不要混用
4. 重新登录获取新 Token

### Q: 管理端登录后访问接口仍提示未登录？

**原因**：使用了用户端 Token 调用管理端接口。

**解决**：
1. 管理端使用 `token`（localStorage）
2. 用户端使用 `user_token`（localStorage）
3. 两端 Token 互相隔离，不能混用

### Q: 如何刷新 Token？

**管理端**：
```http
POST /api/auth/refresh
Authorization: Bearer <token>
```

**用户端**：
```http
POST /api/user/auth/refresh
Authorization: Bearer <user_token>
```

### Q: `@RequireAdmin` 注解不生效？

**原因**：方法上缺少注解或切面未扫描。

**解决**：
1. 确认 Controller 方法上添加了 `@RequireAdmin`
2. 确认 `xiaou-common` 模块的 `AdminAuthAspect` 被扫描
3. 检查 `SaTokenConfig` 中的拦截路径是否包含 `/admin/**`

## 积分与抽奖

### Q: 积分余额不对？

**原因**：余额表和明细表不一致。

**解决**：
1. 以 `user_points_detail` 为审计来源
2. 对比 `balance_after` 字段与 `user_points_balance.total_points`
3. 检查是否有异常回滚不完整的情况

### Q: 签到连续天数在跨月时断了？

**原因**：30 天月份末到月初的日期差计算问题。

**解决**：
1. 检查 `lastCheckinDate` 和当前日期的差值
2. 30 天月份（4、6、9、11月）30 号到 1 号差 2 天
3. 2 月 28/29 号到 1 号差 2-3 天

### Q: 抽奖扣了积分但没中奖记录？

**原因**：扣积分后抽奖、库存或记录阶段异常。

**解决**：
1. 查应用日志和 `lottery_draw_record`
2. 检查库存回滚日志
3. 确认 Redis 和数据库库存是否一致

## OJ 判题

### Q: 提交代码后一直 `pending`？

**原因**：异步判题未启动或判题线程异常。

**解决**：
1. 检查 `oj_submission.status` 是否停在 `pending`
2. 查看 `JudgeService.judge` 异步任务日志
3. 确认 go-judge 沙箱是否正常

### Q: 编译错误但本地能运行？

**原因**：go-judge 环境与本地环境不同。

**解决**：
1. 检查语言版本是否匹配
2. 确认依赖库是否在沙箱中可用
3. 查看 `compile_error` 的详细错误信息

### Q: OJ 首次 AC 没加分？

**原因**：积分发放失败不会回滚判题结果。

**解决**：
1. 查判题日志
2. 检查 `user_points_detail` 的 `OJ_AC` 明细
3. 确认 `PointsServiceImpl.grantOjPoints` 是否被调用

## AI 相关

### Q: AI 配置测试失败？

**原因**：base URL、API Key 或模型名错误。

**解决**：
1. 先看 `/api/admin/ai/config/runtime` 返回的配置
2. 用 `/api/admin/ai/config/test` 测试连通性
3. 确认 API Key 是否有效

### Q: AI 治理页显示 Prompt 风险？

**原因**：Prompt Catalog 里模板为空。

**解决**：
1. 检查对应 `*PromptSpecs` 的模板内容
2. 确认 `systemPrompt` 和 `userPrompt` 不为空

### Q: RAG 检索不生效？

**原因**：RAG sidecar 未启动或配置错误。

**解决**：
1. 访问 `/api/admin/ai/config/rag-service/health`
2. 确认 `xiaou.ai.rag.enabled=true`
3. 检查 `xiaou.ai.rag.endpoint` 配置

## 文件存储

### Q: 文件上传失败？

**原因**：文件大小超限或类型不允许。

**解决**：
1. 检查文件大小是否超过 `MAX_FILE_SIZE`（100MB）
2. 确认文件类型在允许列表中
3. 检查存储配置是否正确

### Q: 图片显示 404？

**原因**：文件路径或访问权限问题。

**解决**：
1. 确认文件是否为公开文件
2. 检查 `/files/**` 静态资源映射
3. 确认本地存储目录 `uploads/` 是否存在

## 通知相关

### Q: 未读数一直不更新？

**原因**：Redis 缓存未刷新。

**解决**：
1. 未读数缓存 TTL 为 30 分钟
2. 手动清除 Redis key `notification:unread:{userId}`
3. 检查 `NotificationCacheUtil` 是否正常工作

### Q: 公告未读数不对？

**原因**：`notification_user_read_record` 表数据异常。

**解决**：
1. 检查 LEFT JOIN 查询是否正确
2. 确认公告删除时是否清理了阅读记录

## 敏感词

### Q: 敏感词检测不生效？

**原因**：词库未加载或白名单配置问题。

**解决**：
1. 检查 `sensitive_word` 表是否有数据
2. 确认白名单是否误放行
3. 检查策略配置是否正确

### Q: 正常词汇被误判为敏感词？

**原因**：词库配置过于严格。

**解决**：
1. 将词汇加入白名单
2. 调整检测策略
3. 使用 `checkText` 接口查看详细命中信息

## 部署相关

### Q: Docker 部署后无法访问？

**原因**：端口映射或网络配置问题。

**解决**：
1. 检查 `docker-compose.yml` 中的端口映射
2. 确认容器网络是否正常
3. 检查防火墙设置

### Q: 生产环境如何配置？

**参考**：
- [Docker 与服务部署](/operations/docker)
- [环境变量总表](/operations/env-vars)
- [独立部署](/guide/deploy)

## 前端相关

### Q: 页面刷新后登录状态丢失？

**原因**：Token 未持久化或过期。

**解决**：
1. 检查 localStorage 中的 Token 是否存在
2. 确认 Token 未过期
3. 管理端支持自动刷新（30 分钟内）

### Q: 跨 Tab 登出不同步？

**原因**：storage 事件监听问题。

**解决**：
1. 管理端支持跨 Tab 登出广播
2. 检查 `stores/user.js` 中的 storage 事件监听
3. 用户端暂不支持此功能

## 数据库相关

### Q: 如何进行数据库迁移？

**参考**：
- [数据库与脚本](/architecture/database)
- [数据表索引](/reference/database-tables)

### Q: 如何查看表结构？

**方式**：
1. 查看 `sql/` 目录下的建表脚本
2. 使用 [数据表索引](/reference/database-tables) 在线查看
3. 连接数据库直接查看

## 更多帮助

如果以上内容无法解决你的问题：

1. 查看 [问题定位流程](/operations/diagnosis-flow)
2. 查看 [常见问题排查](/operations/troubleshooting)
3. 在 GitHub Issues 提交问题
4. 联系项目维护者

## 相关文档

| 文档 | 说明 |
| --- | --- |
| [问题定位流程](/operations/diagnosis-flow) | 系统化排查指南 |
| [常见问题排查](/operations/troubleshooting) | 更多故障场景 |
| [快速开始](/guide/quick-start) | 环境搭建指南 |
| [本地开发](/guide/local-dev) | 开发环境配置 |
